package de.janno.discord.bot.command.poolTarget;

import de.janno.discord.bot.command.StateData;
import lombok.Value;

@Value
public class PoolTargetStateData implements StateData {

    Integer dicePool;
    Integer targetNumber;
    Boolean doReroll;

    @Override
    public String getShortStringValues() {
        return String.format("%s, %s, %s", dicePool, targetNumber, doReroll);
    }
}
