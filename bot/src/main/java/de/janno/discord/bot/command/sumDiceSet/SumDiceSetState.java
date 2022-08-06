package de.janno.discord.bot.command.sumDiceSet;

import de.janno.discord.bot.command.IState;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Value
public class SumDiceSetState implements IState {
    @NonNull
    String buttonValue;
    @NonNull
    Map<String, Integer> diceSetMap;

    @Override
    public String toShortString() {
        return String.format("[%s, %s]", buttonValue, diceSetMap);
    }
}
