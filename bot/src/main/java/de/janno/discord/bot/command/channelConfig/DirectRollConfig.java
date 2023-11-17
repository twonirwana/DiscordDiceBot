package de.janno.discord.bot.command.channelConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class DirectRollConfig extends Config {
    private final boolean alwaysSumResult;

    @JsonCreator
    public DirectRollConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("alwaysSumResult") Boolean alwaysSumResult,
                            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                            @JsonProperty("resultImage") ResultImage resultImage,
                            @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                            @JsonProperty("configLocale") Locale configLocale) {
        super(answerTargetChannelId, answerFormatType, resultImage, diceStyleAndColor, configLocale);
        this.alwaysSumResult = alwaysSumResult == null || alwaysSumResult;

    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s, %s]".formatted(getTargetChannelShortString(), alwaysSumResult, getAnswerFormatType(), getDiceStyleAndColor());
    }

    @Override
    public String toCommandOptionsString() {
        return "%s: %s %s".formatted(ChannelConfigCommand.ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID, alwaysSumResult, super.toCommandOptionsString());
    }
}
