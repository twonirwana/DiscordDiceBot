package de.janno.discord.bot.command.sumCustomSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.dice.DiceParserSystem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class SumCustomSetConfig extends Config {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> labelAndExpression;
    @NonNull
    private final DiceParserSystem diceParserSystem;
    private final boolean alwaysSumResult;

    @JsonCreator
    public SumCustomSetConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                              @JsonProperty("labelAndExpression") @NonNull List<ButtonIdLabelAndDiceExpression> labelAndExpression,
                              @JsonProperty("diceParserSystem") DiceParserSystem diceParserSystem,
                              @JsonProperty("alwaysSumResult") Boolean alwaysSumResult,
                              @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                              @JsonProperty("resultImage") ResultImage resultImage) {
        super(answerTargetChannelId, answerFormatType, resultImage);
        this.labelAndExpression = labelAndExpression;
        this.diceParserSystem = diceParserSystem == null ? DiceParserSystem.DICEROLL_PARSER : diceParserSystem;
        this.alwaysSumResult = alwaysSumResult == null || alwaysSumResult;

    }

    @Override
    public String toShortString() {
        String buttons = labelAndExpression.stream()
                .map(ButtonIdLabelAndDiceExpression::toShortString)
                .collect(Collectors.joining(", "));
        return "[%s, %s, %s, %s, %s, %s]".formatted(buttons, getTargetChannelShortString(), diceParserSystem, alwaysSumResult, getAnswerFormatType(), getResultImage());
    }
}
