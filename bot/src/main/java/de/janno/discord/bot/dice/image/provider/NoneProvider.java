package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;

public final class NoneProvider implements ImageProvider {
    @Override
    public @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide, String color) {
        return List.of();
    }

    @Override
    public @NonNull String getDefaultColor() {
        return "none";
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of();
    }
}
