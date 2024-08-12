package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractD6 implements ImageProvider{

    private final Map<String, FileSidesDiceImageMap> color2DiceSideImageMap;

    public AbstractD6(String folderName) {
        this.color2DiceSideImageMap = getSupportedColors().stream()
                .collect(ImmutableMap.toImmutableMap(Function.identity(), c -> new FileSidesDiceImageMap(folderName + c, List.of(6))));
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
