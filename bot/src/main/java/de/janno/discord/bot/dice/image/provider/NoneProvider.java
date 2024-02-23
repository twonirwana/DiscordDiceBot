package de.janno.discord.bot.dice.image.provider;

import de.janno.discord.bot.dice.image.DiceImageStyle;
import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;

public final class NoneProvider implements ImageProvider {
    @Override
    public @NonNull List<BufferedImage> getImageFor(int totalDieSides, int shownDieSide, String color) {
        return List.of();
    }

    @Override
    public @NonNull String getDefaultColor() {
        return DiceImageStyle.NONE_DICE_COLOR;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of();
    }
}
