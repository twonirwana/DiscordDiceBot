package de.janno.discord.bot.command;

import lombok.NonNull;
import lombok.Value;

@Value
public class LabelAndDiceExpression {
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

