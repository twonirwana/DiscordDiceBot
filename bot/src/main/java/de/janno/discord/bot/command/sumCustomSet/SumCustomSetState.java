package de.janno.discord.bot.command.sumCustomSet;

import de.janno.discord.bot.command.IState;
import lombok.NonNull;
import lombok.Value;

@Value
public class SumCustomSetState implements IState {
    @NonNull
    String buttonValue;
    @NonNull
    String diceExpression;
    String lockedForUserName;

    @Override
    public String toShortString() {
        return String.format("[%s, %s, %s]", buttonValue, diceExpression, lockedForUserName);
    }
}
