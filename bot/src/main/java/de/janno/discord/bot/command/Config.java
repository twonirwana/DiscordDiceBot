package de.janno.discord.bot.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.LegacyImageConfigHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;
import java.util.Optional;

/**
 * A configuration for a dice system. It will be created with the slash command and not modified afterwards.
 */
@EqualsAndHashCode
@Getter
@ToString
public class Config implements Serializable {

    private final Long answerTargetChannelId;

    @NonNull
    private final AnswerFormatType answerFormatType;

    @NonNull
    private final DiceStyleAndColor diceStyleAndColor;

    @JsonCreator
    public Config(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                  @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                  @JsonProperty("resultImage") ResultImage resultImage,
                  @JsonProperty("diceImageStyle") DiceImageStyle diceImageStyle,
                  @JsonProperty("diceDefaultColor") String diceDefaultColor) {
        this.answerTargetChannelId = answerTargetChannelId;
        this.answerFormatType = answerFormatType == null ? AnswerFormatType.full : answerFormatType;

        if (resultImage != null) {
            diceStyleAndColor = LegacyImageConfigHelper.getStyleAndColor(resultImage);
        } else {
            diceStyleAndColor = new DiceStyleAndColor(diceImageStyle, diceDefaultColor);
        }
    }

    public String toShortString() {
        return String.format("[%s, %s, %s]", getTargetChannelShortString(), answerFormatType, diceStyleAndColor);
    }

    protected String getTargetChannelShortString() {
        return answerTargetChannelId == null ? "local" : "target";
    }

}
