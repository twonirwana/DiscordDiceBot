package de.janno.discord.bot.command.poolTarget;

import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
public class PoolTargetState extends State {
    final Integer dicePool;
    final Integer targetNumber;
    final Boolean doReroll;
    final boolean clear;

    public PoolTargetState(@NonNull String buttonValue, Integer dicePool, Integer targetNumber, Boolean doReroll, boolean clear) {
        super(buttonValue);
        this.dicePool = dicePool;
        this.targetNumber = targetNumber;
        this.doReroll = doReroll;
        this.clear = clear;
    }

    @Override
    public String toShortString() {
        return Stream.of(dicePool, targetNumber, doReroll)
                .map(s -> s == null ? "" : String.valueOf(s)).toList().toString();
    }
}
