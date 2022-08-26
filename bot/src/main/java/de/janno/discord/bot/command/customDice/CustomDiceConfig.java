package de.janno.discord.bot.command.customDice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class CustomDiceConfig extends Config {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions;

    @JsonCreator
    public CustomDiceConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("buttonIdLabelAndDiceExpressions") @NonNull List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions) {
        super(answerTargetChannelId);
        this.buttonIdLabelAndDiceExpressions = buttonIdLabelAndDiceExpressions;
    }

    @Override
    public String toShortString() {
        return Stream.concat(buttonIdLabelAndDiceExpressions.stream()
                                .map(ButtonIdLabelAndDiceExpression::toShortString),
                        Stream.of(getTargetChannelShortString()))
                .toList()
                .toString();
    }

}
