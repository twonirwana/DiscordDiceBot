package de.janno.discord.bot.dice.image;

import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;

public final class NoneProvider implements ImageProvider {
    @Override
    public @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide) {
        return List.of();
    }

    @Override
    public int getDieHighAndWith() {
        return 0;
    }
}
