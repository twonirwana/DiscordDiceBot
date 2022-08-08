package de.janno.discord.bot.command.sumCustomSet;

import de.janno.discord.bot.command.StateData;
import lombok.NonNull;
import lombok.Value;

@Value
public class SumCustomSetStateData implements StateData {

    @NonNull
    String diceExpression;
    String lockedForUserName;

    @Override
    public String getShortStringValues() {
        return String.format("%s, %s", diceExpression, lockedForUserName);
    }
}
