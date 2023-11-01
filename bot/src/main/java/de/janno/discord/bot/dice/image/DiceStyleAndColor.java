package de.janno.discord.bot.dice.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;


@Value
public class DiceStyleAndColor {
    @NonNull
    DiceImageStyle diceImageStyle;
    @NonNull
    String configuredDefaultColor;

    public DiceStyleAndColor(@NonNull @JsonProperty("diceImageStyle") DiceImageStyle diceImageStyle,
                             @Nullable @JsonProperty("configuredDefaultColor") String configuredDefaultColor) {
        this.diceImageStyle = diceImageStyle;
        this.configuredDefaultColor = Optional.ofNullable(configuredDefaultColor)
                .map(c -> {
                    if (diceImageStyle.getSupportedColors().contains(c)) {
                        return c;
                    }
                    return diceImageStyle.getDefaultColor();
                })
                .orElse(diceImageStyle.getDefaultColor());
    }

    public @NonNull List<BufferedImage> getImageFor(@Nullable Integer totalDieSides,
                                                    @Nullable Integer shownDieSide,
                                                    @Nullable String rollColor) {

        return diceImageStyle.getImageProvider().getImageFor(totalDieSides, shownDieSide, Optional.ofNullable(rollColor)
                .filter(c -> !Strings.isNullOrEmpty(c))
                .orElse(configuredDefaultColor));
    }

    @Override
    public String toString() {
        return DiceImageStyle.combineStyleAndColorName(diceImageStyle, configuredDefaultColor);
    }
}
