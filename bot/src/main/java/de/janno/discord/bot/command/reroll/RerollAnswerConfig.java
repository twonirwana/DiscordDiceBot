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
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class RerollAnswerConfig extends Config {
    @NonNull
    private final String expression;
    @NonNull
    private final List<DieIdTypeAndValue> dieIdTypeAndValues;
    private final int rerollCount;
    @NonNull
    private final String owner;
    private final boolean alwaysSumUp;
    @Nullable
    private final String label;

    @JsonCreator
    public RerollAnswerConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                              @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                              @JsonProperty("resultImage") ResultImage resultImage,
                              @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                              @JsonProperty("configLocale") Locale configLocale,
                              @JsonProperty("answerInteractionType") AnswerInteractionType answerInteractionType,
                              @JsonProperty("expression") @NonNull String expression,
                              @JsonProperty("dieIdTypeAndValues") @NonNull List<DieIdTypeAndValue> dieIdTypeAndValues,
                              @JsonProperty("rerollCount") int rerollCount,
                              @JsonProperty("owner") String owner,
                              @JsonProperty("alwaysSumUp") boolean alwaysSumUp,
                              @JsonProperty("label") String label
    ) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale);
        this.expression = expression;
        this.dieIdTypeAndValues = dieIdTypeAndValues;
        this.rerollCount = rerollCount;
        this.owner = owner;
        this.alwaysSumUp = alwaysSumUp;
        this.label = label;
    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s, %s, %s]".formatted(expression, dieIdTypeAndValues, rerollCount, getAnswerFormatType(), getDiceStyleAndColor());
    }

    @Override
    public String toCommandOptionsString() {
        throw new NotImplementedException();
    }

    @Override
    public boolean alwaysSumResultUp() {
        return alwaysSumUp;
    }
}
