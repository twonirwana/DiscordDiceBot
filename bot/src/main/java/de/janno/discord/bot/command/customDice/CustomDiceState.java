package de.janno.discord.bot.command.customDice;

import de.janno.discord.bot.command.IState;
import lombok.NonNull;
import lombok.Value;

@Value
public class CustomDiceState implements IState {
    @NonNull
    String diceExpression;

    @Override
    public String toShortString() {
        return String.format("[%s]", diceExpression);
    }
}
