package de.janno.discord.bot.command.channelConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollConfig;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class DirectRollConfig extends RollConfig {
    private final boolean alwaysSumResult;

    @JsonCreator
    public DirectRollConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("alwaysSumResult") Boolean alwaysSumResult,
                            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                            @JsonProperty("answerInteractionType") AnswerInteractionType answerInteractionType,
                            @JsonProperty("resultImage") ResultImage resultImage,
                            @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                            @JsonProperty("configLocale") Locale configLocale
    ) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale, null);
        this.alwaysSumResult = alwaysSumResult == null || alwaysSumResult;
    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s, %s]".formatted(getTargetChannelShortString(), alwaysSumResult, getAnswerFormatType(), getDiceStyleAndColor());
    }

    @Override
    public String toCommandOptionsString() {
        return "%s: %s %s".formatted(ChannelConfigCommand.ALWAYS_SUM_RESULTS_OPTION_NAME, alwaysSumResult, super.toCommandOptionsString());
    }

    @JsonIgnore
    @Override
    public boolean alwaysSumResultUp() {
        return alwaysSumResult;
    }
}
