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
import java.util.Locale;
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

    @NonNull
    private final Locale configLocale;

    @JsonCreator
    public Config(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                  @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                  @JsonProperty("resultImage") ResultImage resultImage,
                  @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                  @JsonProperty("configLocale") Locale configLocale) {
        this.answerTargetChannelId = answerTargetChannelId;
        this.answerFormatType = Optional.ofNullable(answerFormatType).orElse(AnswerFormatType.full);

        if (resultImage != null) {
            this.diceStyleAndColor = LegacyImageConfigHelper.getStyleAndColor(resultImage);
        } else if (diceStyleAndColor != null) {
            this.diceStyleAndColor = diceStyleAndColor;
        } else {
            this.diceStyleAndColor = new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor());
        }
        this.configLocale = Optional.ofNullable(configLocale).orElse(Locale.ENGLISH);
    }

    public String toShortString() {
        return String.format("[%s, %s, %s]", getTargetChannelShortString(), answerFormatType, diceStyleAndColor);
    }

    public String toCommandOptionsString() {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s: %s", BaseCommandOptions.ANSWER_FORMAT_OPTION_NAME, answerFormatType));
        out.add(String.format("%s: %s", BaseCommandOptions.DICE_IMAGE_STYLE_OPTION_NAME, diceStyleAndColor.getDiceImageStyle()));
        out.add(String.format("%s: %s", BaseCommandOptions.DICE_IMAGE_COLOR_OPTION_NAME, diceStyleAndColor.getConfiguredDefaultColor()));
        if (answerTargetChannelId != null) {
            out.add(String.format("%s: <#%s>", BaseCommandOptions.TARGET_CHANNEL_OPTION_NAME, answerTargetChannelId));
        }
        return String.join(" ", out);
    }

    protected String getTargetChannelShortString() {
        return answerTargetChannelId == null ? "local" : "target";
    }

}
