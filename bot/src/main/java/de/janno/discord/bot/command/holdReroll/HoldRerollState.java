package de.janno.discord.bot.command.holdReroll;

import de.janno.discord.bot.command.IState;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class HoldRerollState implements IState {
    @NonNull
    String state; //last action number of the dice, reroll, finish and clear
    @NonNull
    List<Integer> currentResults;
    int rerollCounter;

    @Override
    public String toShortString() {
        return String.format("[%s, %s, %d]", state, currentResults, rerollCounter);
    }
}
