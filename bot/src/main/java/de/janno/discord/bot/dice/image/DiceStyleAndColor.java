package de.janno.discord.bot.dice.image;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;


public class DiceStyleAndColor {
    @NonNull
    private final DiceImageStyle diceImageStyle;
    @NonNull
    private final String configuredDefaultColor;

    public DiceStyleAndColor(@NonNull DiceImageStyle diceImageStyle, @Nullable String configuredDefaultColor) {
        this.diceImageStyle = diceImageStyle;
        this.configuredDefaultColor = Optional.ofNullable(configuredDefaultColor).orElse(diceImageStyle.getImageProvider().getDefaultColor());
    }

    public @NonNull List<BufferedImage> getImageFor(@Nullable Integer totalDieSides,
                                                    @Nullable Integer shownDieSide,
                                                    @Nullable String rollColor) {

        return diceImageStyle.getImageProvider().getImageFor(totalDieSides, shownDieSide, Optional.ofNullable(rollColor).orElse(configuredDefaultColor));
    }

    public int getDieHighAndWith() {
        return diceImageStyle.getImageProvider().getDieHighAndWith();
    }

    @Override
    public String toString() {
        return "%s-%s".formatted(diceImageStyle.name(), configuredDefaultColor);
    }
}
