package de.janno.discord.bot.dice.image;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import de.janno.discord.bot.BotMetrics;
import de.janno.evaluator.dice.RandomElement;
import de.janno.evaluator.dice.RollElement;
import de.janno.evaluator.dice.RollResult;
import io.micrometer.core.instrument.Gauge;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
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
import java.util.Collection;
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
    private final BigInteger MAX_ROLL_COMBINATION_TO_CACHE;
    private final LoadingCache<Integer, BufferedImage> separatorImage = CacheBuilder.newBuilder()
            .maximumSize(10)
            .build(new CacheLoader<>() {
                @Override
                public @NonNull BufferedImage load(@NonNull Integer high) {
                    BufferedImage combined = new BufferedImage(15, high, BufferedImage.TYPE_INT_ARGB_PRE);

                    Graphics g = combined.getGraphics();
                    g.setColor(Color.lightGray);
                    g.fillRect(1, (high / 2) - 2, 13, 4);
                    g.setColor(Color.black);
                    g.drawRect(1, (high / 2) - 2, 13, 4);
                    g.dispose();
                    return combined;
                }
            });

    public ImageResultCreator() {
        this(1000);
    }

    @VisibleForTesting
    public ImageResultCreator(int maxRollCombinationToCache) {
        createCacheIndexFileIfMissing();
        MAX_ROLL_COMBINATION_TO_CACHE = BigInteger.valueOf(maxRollCombinationToCache);
    }

    private void createFolderIfMissing(String folderName) {
        try {
            Files.createDirectories(Paths.get("%s/%s/".formatted(CACHE_FOLDER, folderName)));
            File cacheIndex = new File("%s/%s/%s".formatted(CACHE_FOLDER, folderName, CACHE_INDEX_FILE));
            if (!cacheIndex.exists()) {
                cacheIndex.createNewFile();
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
    String createRollCacheName(RollResult rollResult, DiceStyleAndColor diceStyleAndColor) {
        return "%s@%s".formatted(diceStyleAndColor.toString(), rollResult.getGroupedRandomElements().stream()
                .map(r -> r.stream()
                        .map(re -> {
                            if (RollElement.NO_COLOR.equals(re.getRollElement().getColor())) {
                                return getRandomElementCacheName(re);
                            }
                            return "%s:%s".formatted(re.getRollElement().getColor(), getRandomElementCacheName(re));
                        })
                        .collect(Collectors.joining(","))
                )
                .filter(l -> !l.isEmpty())
                .map("[%s]"::formatted)
                .collect(Collectors.joining(",")));
    }

    //the String of the RandowmElement without the dieId because we are only interested at the dice value, sides and color
    private String getRandomElementCacheName(RandomElement randomElement) {
        if (randomElement.getRandomSelectedFrom() != null) {
            return "%s∈%s".formatted(randomElement.getRollElement(), randomElement.getRandomSelectedFrom());
        } else {
            return "%s∈[%d...%d]".formatted(randomElement.getRollElement(), randomElement.getMinInc(), randomElement.getMaxInc());
        }

    }

    public @Nullable Supplier<? extends InputStream> getImageForRoll(@NonNull RollResult rollResult, @Nullable DiceStyleAndColor diceStyleAndColor) {
        if (diceStyleAndColor == null) {
            return null;
        }
        if (rollResult.getAllRandomElements().isEmpty() || rollResult.getAllRandomElements().size() > 30) {
            return null;
        }

        String name = createRollCacheName(rollResult, diceStyleAndColor);
        String hashName = Hashing.sha256()
                .hashString(name, StandardCharsets.UTF_8)
                .toString();

        String filePath = "%s/%s/%s.png".formatted(CACHE_FOLDER, diceStyleAndColor.toString(), hashName);
        File imageFile = new File(filePath);

        if (!imageFile.exists()) {
            Supplier<? extends InputStream> result = createNewFileForRoll(rollResult, imageFile, name, diceStyleAndColor);
            if (result != null) {
                BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_MISS);
            }
            return result;
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

    private @Nullable Supplier<? extends InputStream> createNewFileForRoll(RollResult rollResult, File file, String name, DiceStyleAndColor diceStyleAndColor) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        final List<List<BufferedImage>> images = rollResult.getGroupedRandomElements().stream()
                .map(r -> r.stream()
                        .filter(re -> !DiceImageStyle.NONE_DICE_COLOR.equals(re.getRollElement().getColor()))
                        .flatMap(re -> diceStyleAndColor.getImageFor(re).stream())
                        .toList()
                )
                .filter(l -> !l.isEmpty())
                .toList();

        //we check it only now, so we need to draw images only once if there is no cache
        if (images.isEmpty() || images.stream().anyMatch(List::isEmpty)) {
            return null;
        }

        final int maxImageHight = images.stream().flatMap(Collection::stream)
                .mapToInt(BufferedImage::getHeight).max().orElse(0);

        BufferedImage separator = separatorImage.getUnchecked(maxImageHight);
        ImmutableList.Builder<BufferedImage> builder = ImmutableList.builder();
        for (int i = 0; i < images.size() - 1; i++) {
            builder.addAll(images.get(i));
            builder.add(separator);
        }
        builder.addAll(images.getLast());

        final List<BufferedImage> imagesWithSeparators = builder.build();

        final int maxLineWidth = 640;

        final int allInOneLineWidth = imagesWithSeparators.stream().mapToInt(BufferedImage::getWidth).sum();
        final int numberOfImageLines = (int) Math.ceil(((double) allInOneLineWidth) / ((double) maxLineWidth));

        final int w = Math.min(allInOneLineWidth, maxLineWidth);
        final int h = maxImageHight * numberOfImageLines;
        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);

        Graphics g = combined.getGraphics();

        int currentLine = 0;
        int currentLineWidth = 0;
        for (BufferedImage image : imagesWithSeparators) {
            if (image.getWidth() + currentLineWidth > maxLineWidth) {
                currentLine++;
                currentLineWidth = image.getWidth();
            } else {
                currentLineWidth += image.getWidth();
            }
            g.drawImage(image, currentLineWidth - image.getWidth(), maxImageHight * currentLine, image.getWidth(), image.getHeight(), null);
        }

        g.dispose();
        final String indexPath = "%s/%s/%s".formatted(CACHE_FOLDER, diceStyleAndColor.toString(), CACHE_INDEX_FILE);
        BigInteger combinations = rollResult.getGroupedRandomElements()
                .stream().flatMap(Collection::stream)
                .map(r -> {
                            if (r.getMaxInc() != null && r.getMinInc() != null) {
                                return (r.getMaxInc() + 1) - r.getMinInc();
                            } else if (r.getRandomSelectedFrom() != null) {
                                return r.getRandomSelectedFrom().size();
                            } else {
                                throw new IllegalStateException("The roll %s should not used to crate images".formatted(rollResult));
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
            log.trace("Created image {} in {}ms", name, stopwatch.elapsed(TimeUnit.MILLISECONDS));
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
