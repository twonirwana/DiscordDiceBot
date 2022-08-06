package de.janno.discord.bot.command.customDice;

import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CustomDiceConfig extends Config {
    @NonNull
    final List<LabelAndDiceExpression> labelAndExpression;

    public CustomDiceConfig(Long answerTargetChannelId, @NonNull List<LabelAndDiceExpression> labelAndExpression) {
        super(answerTargetChannelId);
        this.labelAndExpression = labelAndExpression;
    }

    @Override
    public String toShortString() {
        return Stream.concat(labelAndExpression.stream()
                                .map(LabelAndDiceExpression::toShortString),
                        Stream.of(getTargetChannelShortString()))
                .toList()
                .toString();
    }

    @Value
    public static class LabelAndDiceExpression {
        @NonNull
        String label;
        @NonNull
        String diceExpression;


        public String toShortString() {
            if (diceExpression.equals(label)) {
                return diceExpression;
            }
            return String.format("%s@%s", diceExpression, label);
        }
    }
}
