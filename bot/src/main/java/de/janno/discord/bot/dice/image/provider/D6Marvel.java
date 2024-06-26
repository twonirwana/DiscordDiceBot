package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class D6Marvel implements ImageProvider {

    public static final String BLUE = "blue";
    public static final String RED = "red";
    private static final List<String> SUPPORTED_COLORS = List.of(BLUE, RED);
    private final Map<String, FileSidesDiceImageMap> color2DiceSideImageMap;


    public D6Marvel() {
        this.color2DiceSideImageMap = SUPPORTED_COLORS.stream()
                .collect(ImmutableMap.toImmutableMap(Function.identity(), c -> new FileSidesDiceImageMap("marvel_" + c, List.of(6))));
    }

    @Override
    public @NonNull String getDefaultColor() {
        return BLUE;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return SUPPORTED_COLORS;
    }

    @Override
    public @NonNull List<BufferedImage> getImageFor(int totalDieSides, int shownDieSide, String color) {
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
