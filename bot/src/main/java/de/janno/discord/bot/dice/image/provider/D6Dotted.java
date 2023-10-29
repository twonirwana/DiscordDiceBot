package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class D6Dotted implements ImageProvider {

    public static final String WHITE = "white";
    public static final String BLACK_AND_GOLD = "black_and_gold";
    private static final List<String> SUPPORTED_COLORS = List.of(WHITE, BLACK_AND_GOLD);
    private final Map<String, FileSidesDiceImageMap> color2DiceSideImageMap;


    public D6Dotted() {
        this.color2DiceSideImageMap = SUPPORTED_COLORS.stream()
                .collect(ImmutableMap.toImmutableMap(Function.identity(), c -> new FileSidesDiceImageMap("d6_" + c, List.of(6))));
    }

    @Override
    public int getDieHighAndWide() {
        return 50;
    }

    @Override
    public @NonNull String getDefaultColor() {
        return WHITE;
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
