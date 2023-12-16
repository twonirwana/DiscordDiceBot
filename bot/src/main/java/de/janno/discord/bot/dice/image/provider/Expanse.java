package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Expanse implements ImageProvider {

    public static final String BELT_DARK = "belt_dark";
    public static final String BELT_LIGHT = "belt_light";
    public static final String EARTH_DARK = "earth_dark";
    public static final String EARTH_LIGHT = "earth_light";
    public static final String MARS_DARK = "mars_dark";
    public static final String MARS_LIGHT = "mars_light";
    public static final String PROTOTGEN_DARK = "protogen_dark";
    public static final String PROTOTGEN_LIGHT = "protogen_light";
    private static final List<String> SUPPORTED_COLORS = List.of(BELT_DARK, BELT_LIGHT, EARTH_DARK, EARTH_LIGHT, MARS_DARK, MARS_LIGHT, PROTOTGEN_DARK, PROTOTGEN_LIGHT);
    private final Map<String, FileSidesDiceImageMap> color2DiceSideImageMap;

    public Expanse() {
        this.color2DiceSideImageMap = SUPPORTED_COLORS.stream()
                .collect(ImmutableMap.toImmutableMap(Function.identity(), c -> new FileSidesDiceImageMap("expanse_%s".formatted(c), List.of(6))));
    }

    @Override
    public @NonNull String getDefaultColor() {
        return BELT_DARK;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return SUPPORTED_COLORS;
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
        return Optional.ofNullable(fileSidesDiceImageMap.getDiceImageMap().get(totalDieSides))
                .map(m -> m.get(shownDieSide))
                .map(List::of).orElse(List.of());
    }
}
