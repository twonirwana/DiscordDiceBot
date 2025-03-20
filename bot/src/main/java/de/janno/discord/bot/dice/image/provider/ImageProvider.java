package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;


import java.awt.image.BufferedImage;
import java.util.List;

public interface ImageProvider {

    @NonNull List<BufferedImage> getImageFor(int totalDieSides, int shownDieSide, String color);

    default @NonNull List<BufferedImage> getImageForString(int totalDieSides, @NonNull String diceValue, String color) {
        return List.of();
    }

    @NonNull String getDefaultColor();

    @NonNull List<String> getSupportedColors();
}
