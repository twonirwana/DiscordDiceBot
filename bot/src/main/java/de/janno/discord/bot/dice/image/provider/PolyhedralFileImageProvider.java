package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import lombok.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class PolyhedralFileImageProvider implements ImageProvider {

    private final Map<Integer, Map<Integer, BufferedImage>> diceImageMap = Stream.of(4, 6, 8, 10, 12, 20, 100)
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
                                return ImageIO.read(Resources.getResource(this.getStyleFolder() + getDieFolder(d) + getFileName(d, s)).openStream());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
            }));

    protected abstract String getStyleFolder();

    protected abstract String getDieFolder(int totalDieSides);

    protected abstract String getFileName(int totalDieSides, int shownDieSide);

    @Override
    public int getDieHighAndWith() {
        return 100;
    }

    @Override
    public @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide, String color) {
        if (totalDieSides == null || shownDieSide == null) {
            return List.of();
        }
        if (totalDieSides == 100) {
            int tens = shownDieSide / 10;

            int ones = shownDieSide - (tens * 10);
            if (ones == 0) {
                ones = 10;
            }
            if (tens == 0) {
                tens = 10;
            }
            return List.of(
                    diceImageMap.get(100).get(tens),
                    diceImageMap.get(10).get(ones)
            );
        }
        return Optional.ofNullable(diceImageMap.get(totalDieSides))
                .map(m -> m.get(shownDieSide))
                .map(List::of).orElse(List.of());
    }
}
