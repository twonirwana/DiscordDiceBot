package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.command.IState;
import lombok.Value;

@Value
public class CountSuccessesState implements IState {
    int numberOfDice;

    @Override
    public String toShortString() {
        return String.format("[%d]", numberOfDice);
    }
}
