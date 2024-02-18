package de.janno.discord.bot.dice.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import de.janno.evaluator.dice.RandomElement;
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


    public @NonNull List<BufferedImage> getImageFor(@NonNull final RandomElement randomElement) {
        Integer maxInc = randomElement.getMaxInc();
        Integer diceValueAsInteger = randomElement.getRollElement().asInteger().orElse(null);
        if (maxInc != null && diceValueAsInteger != null) {
            return diceImageStyle.getImageProvider().getImageFor(maxInc, diceValueAsInteger, Optional.of(randomElement.getRollElement().getColor())
                    .filter(c -> !Strings.isNullOrEmpty(c))
                    .orElse(configuredDefaultColor));
        } else if (randomElement.getRandomSelectedFrom() != null) {
            return diceImageStyle.getImageProvider().getImageForString(randomElement.getRandomSelectedFrom().size(), randomElement.getRollElement().getValue(), Optional.of(randomElement.getRollElement().getColor())
                    .filter(c -> !Strings.isNullOrEmpty(c))
                    .orElse(configuredDefaultColor));
        }
        return List.of();
    }

    @Override
    public String toString() {
        return DiceImageStyle.combineStyleAndColorName(diceImageStyle, configuredDefaultColor);
    }
}
