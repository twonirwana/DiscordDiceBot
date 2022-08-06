package de.janno.discord.bot.command.customDice;

import de.janno.discord.bot.command.IConfig;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Value
public class CustomDiceConfig implements IConfig {
    @NonNull
    List<LabelAndDiceExpression> labelAndExpression;
    Long answerTargetChannelId;

    @Override
    public String toShortString() {
        return Stream.concat(labelAndExpression.stream()
                                .map(LabelAndDiceExpression::toShortString),
                        Stream.of(targetChannelToString(answerTargetChannelId)))
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
