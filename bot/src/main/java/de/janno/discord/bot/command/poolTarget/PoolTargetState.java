package de.janno.discord.bot.command.poolTarget;

import de.janno.discord.bot.command.IState;
import lombok.Value;

import java.util.stream.Stream;

@Value
public class PoolTargetState implements IState {
    Integer dicePool;
    Integer targetNumber;
    Boolean doReroll;
    boolean clear;

    @Override
    public String toShortString() {
        return Stream.of(dicePool, targetNumber, doReroll)
                .map(s -> s == null ? "" : String.valueOf(s)).toList().toString();
    }
}
