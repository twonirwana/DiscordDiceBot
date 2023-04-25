package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.awt.image.BufferedImage;
import java.util.List;

public interface ImageProvider {

    @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide, String color);

    int getDieHighAndWith();
}
