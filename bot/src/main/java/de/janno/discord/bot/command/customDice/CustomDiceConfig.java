package de.janno.discord.bot.command.customDice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@ToString
public class CustomDiceConfig extends Config {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions;
    @NonNull
    private final DiceParserSystem diceParserSystem;

    @JsonCreator
    public CustomDiceConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("buttonIdLabelAndDiceExpressions") @NonNull List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions,
                            @JsonProperty("diceParserSystem") DiceParserSystem diceParserSystem,
                            @JsonProperty("answerDisplayType") String answerDisplayType) {
        super(answerTargetChannelId, answerDisplayType);
        this.buttonIdLabelAndDiceExpressions = buttonIdLabelAndDiceExpressions;
        this.diceParserSystem = diceParserSystem == null ? DiceParserSystem.DICEROLL_PARSER : diceParserSystem;
    }

    @Override
    public String toShortString() {
        String buttons = buttonIdLabelAndDiceExpressions.stream()
                .map(ButtonIdLabelAndDiceExpression::toShortString)
                .collect(Collectors.joining(", "));
        return "[%s, %s, %s, %s]".formatted(buttons, getTargetChannelShortString(), diceParserSystem, getAnswerDisplayType());
    }

}
