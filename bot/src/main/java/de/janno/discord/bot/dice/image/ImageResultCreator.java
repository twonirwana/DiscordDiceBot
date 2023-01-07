package de.janno.discord.bot.dice.image;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import de.janno.discord.bot.BotMetrics;
import de.janno.evaluator.dice.RandomElement;
import de.janno.evaluator.dice.Roll;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class ImageResultCreator {

    private static final String CACHE_FOLDER = "imageCache";
    private static final String CACHE_INDEX = CACHE_FOLDER + "/" + "imageCacheName.csv";
    private final static Map<Integer, Map<Integer, BufferedImage>> DICE_IMAGE_MAP = Stream.of(4, 6, 8, 10, 12, 20, 100)
            .collect(ImmutableMap.toImmutableMap(d -> d, d -> {
                final Stream<Integer> sideStream;
                if (d != 100) {
                    sideStream = IntStream.range(1, d + 1).boxed();
                } else {
                    sideStream = IntStream.range(1, 11).boxed();
                }
                return sideStream
                        .collect(ImmutableMap.toImmutableMap(s -> s, s -> {
                            try {
                                return ImageIO.read(Resources.getResource("images/d%d/d%ds%d.png".formatted(d, d, s)).openStream());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
            }));

    public ImageResultCreator() {
        createCacheIndexFileIfMissing();
    }

    private void createCacheIndexFileIfMissing() {
        try {
            Files.createDirectories(Paths.get("imageCache"));
            File cacheIndex = new File(CACHE_INDEX);
            if (!cacheIndex.exists()) {
                cacheIndex.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NonNull List<BufferedImage> getImageForRandomElement(@NonNull RandomElement randomElement) {
        Preconditions.checkNotNull(randomElement.getMaxInc());
        if (randomElement.getMaxInc() == 100) {
            int number = randomElement.getRollElement().asInteger().orElseThrow();
            int tens = number / 10;

            int ones = number - (tens * 10);
            if (ones == 0) {
                ones = 10;
            }
            if (tens == 0) {
                tens = 10;
            }
            return List.of(
                    DICE_IMAGE_MAP.get(100).get(tens),
                    DICE_IMAGE_MAP.get(10).get(ones)
            );
        }
        return List.of(DICE_IMAGE_MAP.get(randomElement.getMaxInc()).get(randomElement.getRollElement().asInteger().orElseThrow()));
    }

    private boolean invalidDieResult(@NonNull RandomElement randomElement) {
        return randomElement.getMinInc() == null ||
                randomElement.getMinInc() != 1 ||
                randomElement.getMaxInc() == null ||
                !DICE_IMAGE_MAP.containsKey(randomElement.getMaxInc()) ||
                randomElement.getRollElement().asInteger().isEmpty() ||
                randomElement.getRollElement().asInteger().get() > randomElement.getMaxInc() ||
                randomElement.getRollElement().asInteger().get() < 1;
    }

    @VisibleForTesting
    String createRollCacheName(Roll roll) {
        return roll.getRandomElementsInRoll().getRandomElements().stream()
                .map(r -> r.getRandomElements().stream()
                        .map(re -> "d%ds%d".formatted(re.getMaxInc(), re.getRollElement().asInteger().orElseThrow()))
                        .collect(Collectors.joining())
                )
                .filter(l -> !l.isEmpty())
                .collect(Collectors.joining("-"));
    }

    public @Nullable File getImageForRoll(@NonNull List<Roll> rolls) {
        if (rolls.size() != 1 ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().size() > 10 ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream()
                        .anyMatch(r -> r.getRandomElements().size() > 15) ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream()
                        .flatMap(r -> r.getRandomElements().stream())
                        .anyMatch(this::invalidDieResult)
        ) {
            return null;
        }

        String name = createRollCacheName(rolls.get(0));
        String hashName = Hashing.sha256()
                .hashString(name, StandardCharsets.UTF_8)
                .toString();

        String filePath = CACHE_FOLDER + "/" + hashName + ".png";
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            createNewFileForRoll(rolls.get(0), imageFile, name);
            BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_MISS);
        } else {
            log.info("Use cached file %s for %s".formatted(filePath, name));
            BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_HIT);
        }
        return imageFile;
    }

    private void createNewFileForRoll(Roll roll, File file, String name) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<List<BufferedImage>> images = roll.getRandomElementsInRoll().getRandomElements().stream()
                .map(r -> r.getRandomElements().stream()
                        .flatMap(re -> getImageForRandomElement(re).stream())
                        .toList()
                )
                .filter(l -> !l.isEmpty())
                .toList();


        int maxInnerSize = images.stream()
                .mapToInt(List::size)
                .max().orElseThrow();

        int singleDiceSize = 100;
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

        writeFile(combined, file, name);

        BotMetrics.imageCreationTimer(stopwatch.elapsed());
        log.debug("Created image in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }


    private synchronized void writeFile(BufferedImage combined, File outputFile, String name) {
        try {
            createCacheIndexFileIfMissing();
            ImageIO.write(combined, "PNG", outputFile);
            Files.writeString(
                    Paths.get(CACHE_INDEX),
                    "%s;%s\n".formatted(name, outputFile.getName()),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
