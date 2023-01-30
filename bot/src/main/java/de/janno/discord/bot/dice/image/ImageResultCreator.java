package de.janno.discord.bot.dice.image;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.hash.Hashing;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.evaluator.dice.RandomElement;
import de.janno.evaluator.dice.Roll;
import io.micrometer.core.instrument.Gauge;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class ImageResultCreator {

    private static final String CACHE_FOLDER = "imageCache";
    private static final String CACHE_INDEX = CACHE_FOLDER + "/" + "imageCacheName.csv";

    public ImageResultCreator() {
        createCacheIndexFileIfMissing();
    }

    private void createCacheIndexFileIfMissing() {
        try {
            Files.createDirectories(Paths.get(CACHE_FOLDER));
            File cacheIndex = new File(CACHE_INDEX);
            if (!cacheIndex.exists()) {
                boolean createdCacheFolder = cacheIndex.createNewFile();
                if (createdCacheFolder) {
                    log.info("created new image cache folder");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Gauge.builder("diceImage.cache", () -> {
            try (Stream<Path> files = Files.list(Paths.get(CACHE_FOLDER))) {
                return files.count();
            } catch (IOException e) {
                return -1;
            }
        }).register(globalRegistry);

    }

    @VisibleForTesting
    String createRollCacheName(Roll roll, ResultImage resultImage) {
        return "%s@%s".formatted(resultImage.name(), roll.getRandomElementsInRoll().getRandomElements().stream()
                .map(r -> r.getRandomElements().stream()
                        .map(RandomElement::toString)
                        .collect(Collectors.joining(","))
                )
                .filter(l -> !l.isEmpty())
                .map("[%s]"::formatted)
                .collect(Collectors.joining(",")));
    }

    public @Nullable File getImageForRoll(@NonNull List<Roll> rolls, ResultImage resultImage) {
        if (rolls.size() != 1 ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().size() == 0 ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().size() > 10 ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream().anyMatch(r -> r.getRandomElements().size() > 15) ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream().anyMatch(r -> r.getRandomElements().size() == 0) ||
                rolls.get(0).getRandomElementsInRoll().getRandomElements().stream()
                        .flatMap(r -> r.getRandomElements().stream())
                        .anyMatch(r -> ImageProviderFactory.getImageFor(resultImage, r.getMaxInc(), r.getRollElement().asInteger().orElse(null)).isEmpty())
        ) {
            return null;
        }

        String name = createRollCacheName(rolls.get(0), resultImage);
        String hashName = Hashing.sha256()
                .hashString(name, StandardCharsets.UTF_8)
                .toString();

        String filePath = "%s/%s.png".formatted(CACHE_FOLDER, hashName);
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            createNewFileForRoll(rolls.get(0), imageFile, name, resultImage);
            BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_MISS);
        } else {
            log.trace("Use cached file %s for %s".formatted(filePath, name));
            BotMetrics.incrementImageResultMetricCounter(BotMetrics.CacheTag.CACHE_HIT);
        }
        return imageFile;
    }

    private void createNewFileForRoll(Roll roll, File file, String name, ResultImage resultImage) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<List<BufferedImage>> images = roll.getRandomElementsInRoll().getRandomElements().stream()
                .map(r -> r.getRandomElements().stream()
                        .flatMap(re -> ImageProviderFactory.getImageFor(resultImage, re.getMaxInc(), re.getRollElement().asInteger().orElse(null)).stream())
                        .toList()
                )
                .filter(l -> !l.isEmpty())
                .toList();


        int maxInnerSize = images.stream()
                .mapToInt(List::size)
                .max().orElseThrow();

        int singleDiceSize = ImageProviderFactory.getDieHighAndWith(resultImage);
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
        log.debug("Created image {} in {}ms", name, stopwatch.elapsed(TimeUnit.MILLISECONDS));
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
