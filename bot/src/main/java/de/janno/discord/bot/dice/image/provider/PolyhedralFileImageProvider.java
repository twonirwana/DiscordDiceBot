package de.janno.discord.bot.dice.image.provider;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class PolyhedralFileImageProvider implements ImageProvider {

    private final Map<String, FileSidesDiceImageMap> color2DiceSideImageMap;
    private final List<String> supportedColors;
    private final String defaultColor;

    public PolyhedralFileImageProvider(String styleFolder, List<String> supportedColors, String defaultColor) {
        this(styleFolder, supportedColors, defaultColor, supportedColors.stream().collect(ImmutableMap.toImmutableMap(Function.identity(), c -> List.of(4, 6, 8, 10, 12, 20, 100))));
    }

    public PolyhedralFileImageProvider(String styleFolder, List<String> supportedColors, String defaultColor, Map<String, List<Integer>> colorSupportedDiceSides) {
        this.color2DiceSideImageMap = supportedColors.stream()
                .collect(ImmutableMap.toImmutableMap(Function.identity(), c -> new FileSidesDiceImageMap(styleFolder + "_" + c, colorSupportedDiceSides.get(c))));
        this.defaultColor = defaultColor;
        Preconditions.checkArgument(supportedColors.contains(defaultColor), "The default color {} was not in the supported colors {}", defaultColor, supportedColors);
        this.supportedColors = supportedColors;
    }

    @Override
    public @NonNull String getDefaultColor() {
        return defaultColor;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return supportedColors;
    }

    @Override
    public @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide, String color) {
        if (totalDieSides == null || shownDieSide == null) {
            return List.of();
        }

        final String validatedColor;
        if (color == null || !getSupportedColors().contains(color)) {
            validatedColor = getDefaultColor();
        } else {
            validatedColor = color;
        }

        FileSidesDiceImageMap fileSidesDiceImageMap = color2DiceSideImageMap.get(validatedColor);
        if (totalDieSides == 100) {
            int tens = shownDieSide / 10;

            int ones = shownDieSide - (tens * 10);
            if (ones == 0) {
                ones = 10;
            }
            if (tens == 0) {
                tens = 10;
            }

            return Stream.of(
                            fileSidesDiceImageMap.getDiceImageMap().getOrDefault(100, Map.of()).getOrDefault(tens, null),
                            fileSidesDiceImageMap.getDiceImageMap().getOrDefault(10, Map.of()).getOrDefault(ones, null))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return Optional.ofNullable(fileSidesDiceImageMap.getDiceImageMap().get(totalDieSides))
                .map(m -> m.get(shownDieSide))
                .map(List::of).orElse(List.of());
    }
}
