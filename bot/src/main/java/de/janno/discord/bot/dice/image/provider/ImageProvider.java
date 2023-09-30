package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;

public interface ImageProvider {

    @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide, @Nullable String color);

    int getDieHighAndWide();

    @NonNull String getDefaultColor();

    @NonNull List<String> getSupportedColors();
}
