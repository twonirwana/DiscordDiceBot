package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FileSidesDiceImageMap {

    private final Map<Integer, Map<Integer, BufferedImage>> diceImageMap;

    public FileSidesDiceImageMap(String folder, List<Integer> sides) {
        this.diceImageMap = sides.stream()
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
                                    return ImageIO.read(Resources.getResource("images/%s/d%d/d%ds%d.png".formatted(folder, d, d, s)).openStream());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }));
                }));
    }

    public Map<Integer, Map<Integer, BufferedImage>> getDiceImageMap() {
        return diceImageMap;
    }

}
