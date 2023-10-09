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
import java.util.ArrayList;
import java.util.List;

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
                  @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor) {
        this.answerTargetChannelId = answerTargetChannelId;
        this.answerFormatType = answerFormatType == null ? AnswerFormatType.full : answerFormatType;

        if (resultImage != null) {
            this.diceStyleAndColor = LegacyImageConfigHelper.getStyleAndColor(resultImage);
        } else if (diceStyleAndColor != null) {
            this.diceStyleAndColor = diceStyleAndColor;
        } else {
            this.diceStyleAndColor = new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor());
        }
    }

    public String toShortString() {
        return String.format("[%s, %s, %s]", getTargetChannelShortString(), answerFormatType, diceStyleAndColor);
    }

    public String toCommandOptionsString() {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s: %s", BaseCommandOptions.ANSWER_FORMAT_OPTION, answerFormatType));
        out.add(String.format("%s: %s", BaseCommandOptions.DICE_IMAGE_STYLE_OPTION, diceStyleAndColor.getDiceImageStyle()));
        out.add(String.format("%s: %s", BaseCommandOptions.DICE_IMAGE_COLOR_OPTION, diceStyleAndColor.getConfiguredDefaultColor()));
        if (answerTargetChannelId != null) {
            out.add(String.format("%s: <#%s>", BaseCommandOptions.ANSWER_TARGET_CHANNEL_OPTION, answerTargetChannelId));
        }
        return String.join(" ", out);
    }

    protected String getTargetChannelShortString() {
        return answerTargetChannelId == null ? "local" : "target";
    }

}
