package de.janno.discord.bot.command.reroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class RerollAnswerConfig extends Config {
    @NonNull
    private final String expression;
    @NonNull
    private final List<DieIdAndValue> dieIdAndValues;
    private final int rerollCount;

    @JsonCreator
    public RerollAnswerConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                              @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                              @JsonProperty("resultImage") ResultImage resultImage,
                              @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                              @JsonProperty("configLocale") Locale configLocale,
                              @JsonProperty("answerInteractionType") AnswerInteractionType answerInteractionType,
                              @JsonProperty("expression") @NonNull String expression,
                              @JsonProperty("dieIdAndValues") @NonNull List<DieIdAndValue> dieIdAndValues,
                              @JsonProperty("rerollCount") int rerollCount
    ) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale);
        this.expression = expression;
        this.dieIdAndValues = dieIdAndValues;
        this.rerollCount = rerollCount;
    }

    @Override
    public String toShortString() {
        //todo
        return "";
    }

    @Override
    public String toCommandOptionsString() {
        //todo
        return "";
    }

}
