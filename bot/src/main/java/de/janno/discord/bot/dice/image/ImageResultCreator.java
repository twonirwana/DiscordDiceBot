package de.janno.discord.bot.dice.image;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.hash.Hashing;
import de.janno.discord.bot.BotMetrics;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.RollElement;
import io.micrometer.core.instrument.Gauge;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class ImageResultCreator {

    private static final String CACHE_FOLDER = "imageCache";
    private static final String CACHE_INDEX_FILE = "imageCacheName.csv";
    private final BigInteger MAX_ROLL_COMBINATION_TO_CACHE = BigInteger.valueOf(1000);

    public ImageResultCreator() {
        createCacheIndexFileIfMissing();
    }

    private void createFolderIfMissing(String folderName) {
        try {
            Files.createDirectories(Paths.get("%s/%s/".formatted(CACHE_FOLDER, folderName)));
            File cacheIndex = new File("%s/%s/%s".formatted(CACHE_FOLDER, folderName, CACHE_INDEX_FILE));
            if (!cacheIndex.exists()) {
                boolean createdCacheFolder = cacheIndex.createNewFile();
                if (createdCacheFolder) {
                    log.info("created new image cache folder");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createCacheIndexFileIfMissing() {
        Arrays.stream(DiceImageStyle.values())
                .flatMap(s -> s.getSupportedColors().stream()
                        .map(c -> DiceImageStyle.combineStyleAndColorName(s, c)))
                .forEach(ri -> {
                    createFolderIfMissing(ri);
                    Gauge.builder("diceImage.cache", () -> {
                        try (Stream<Path> files = Files.list(Paths.get("%s/%s/".formatted(CACHE_FOLDER, ri)))) {
                            return files.count();
                        } catch (IOException e) {
                            return -1;
                        }
                    }).tag("type", ri).register(globalRegistry);
                });
    }

    @VisibleForTesting
    String createRollCacheName(Roll roll, DiceStyleAndColor diceStyleAndColor) {
        return "%s@%s".formatted(diceStyleAndColor.toString(), roll.getRandomElementsInRoll().getRandomElements().stream()
                .map(r -> r.getRandomElements().stream()
                        .map(re -> {
                            if (RollElement.NO_COLOR.equals(re.getRollElement().getColor())) {
                                return re.toString();
                            }
                            return "%s:%s".formatted(re.getRollElement().getColor(), re.toString());
                        })
                        .collect(Collectors.joining(","))
                )
                .filter(l -> !l.isEmpty())
                .map("[%s]"::formatted)
                .collect(Collectors.joining(",")));
    }

    public @Nullable Supplier<? extends InputStream> getImageForRoll(@NonNull List<Roll> rolls, @Nullable DiceStyleAndColor diceStyleAndColor) {
        if (diceStyleAndColor == null) {
            return null;
        }
        if (rolls.size() != 1 ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().isEmpty() ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().size() > 10 ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream().anyMatch(r -> r.getRandomElements().size() > 15) ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream().anyMatch(r -> r.getRandomElements().isEmpty()) ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream()
                        .flatMap(r -> r.getRandomElements().stream())
                        .anyMatch(r -> diceStyleAndColor.getImageFor(r.getMaxInc(), r.getRollElement().asInteger().orElse(null), r.getRollElement().getColor()).isEmpty())
        ) {
            return null;
        }

        String name = createRollCacheName(rolls.get(0), diceStyleAndColor);
        String hashName = Hashing.sha256()
                .hashString(name, StandardCharsets.UTF_8)
                .toString();

        String filePath = "%s/%s/%s.png".formatted(CACHE_FOLDER, diceStyleAndColor.toString(), hashName);
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_MISS);
            return createNewFileForRoll(rolls.get(0), imageFile, name, diceStyleAndColor);
        } else {
            log.trace("Use cached file %s for %s".formatted(filePath, name));
            BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_HIT);
            try {
                byte[] imageBytes = com.google.common.io.Files.toByteArray(imageFile);
                return () -> new ByteArrayInputStream(imageBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Supplier<? extends InputStream> createNewFileForRoll(Roll roll, File file, String name, DiceStyleAndColor diceStyleAndColor) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<List<BufferedImage>> images = roll.getRandomElementsInRoll().getRandomElements().stream()
                .map(r -> r.getRandomElements().stream()
                        .flatMap(re -> diceStyleAndColor.getImageFor(re.getMaxInc(), re.getRollElement().asInteger().orElse(null), re.getRollElement().getColor()).stream())
                        .toList()
                )
                .filter(l -> !l.isEmpty())
                .toList();


        int maxInnerSize = images.stream()
                .mapToInt(List::size)
                .max().orElseThrow();

        int singleDiceSize = diceStyleAndColor.getDieHighAndWith();
        int w = singleDiceSize * (maxInnerSize);
        int h = singleDiceSize * (images.size());
        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);

        Graphics g = combined.getGraphics();

        for (int i = 0; i < images.size(); i++) {
            List<BufferedImage> line = images.get(i);
            for (int j = 0; j < line.size(); j++) {
                g.drawImage(line.get(j), singleDiceSize * j, singleDiceSize * i, singleDiceSize, singleDiceSize, null);
            }
        }
        g.dispose();
        String indexPath = "%s/%s/%s".formatted(CACHE_FOLDER, diceStyleAndColor.toString(), CACHE_INDEX_FILE);
        BigInteger combinations = roll.getRandomElementsInRoll().getRandomElements()
                .stream().flatMap(r -> r.getRandomElements().stream())
                .map(r -> {
                            if (r.getMaxInc() != null && r.getMinInc() != null) {
                                return (r.getMaxInc() + 1) - r.getMinInc();
                            } else if (r.getRandomSelectedFrom() != null) {
                                return r.getRandomSelectedFrom().size();
                            } else {
                                return 1;
                            }
                        }

                )
                .map(BigInteger::valueOf)
                .reduce(BigInteger.ONE, BigInteger::multiply);
        //don't cache images that unlikely to ever get generated again
        if (MAX_ROLL_COMBINATION_TO_CACHE.compareTo(combinations) < 0) {
            BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_SKIP);
            log.trace("no cache because roll has {} combinations", combinations);
        } else {
            writeFile(combined, file, Path.of(indexPath), name);
        }
        return () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(combined, "png", baos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            log.debug("Created image {} in {}ms", name, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            BotMetrics.imageCreationTimer(stopwatch.elapsed());
            return new ByteArrayInputStream(baos.toByteArray());
        };
    }

    private synchronized void writeFile(BufferedImage combined, File outputFile, Path indexFile, String name) {
        try {
            createCacheIndexFileIfMissing();
            ImageIO.write(combined, "PNG", outputFile);
            Files.writeString(
                    indexFile,
                    "%s;%s\n".formatted(name, outputFile.getName()),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
