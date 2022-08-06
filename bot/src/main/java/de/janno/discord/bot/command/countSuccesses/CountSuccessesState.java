package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
public class CountSuccessesState extends State {

    public CountSuccessesState(String buttonValue) {
        super(buttonValue);
    }

    public int getNumberOfDice() {
        return Integer.parseInt(getButtonValue());
    }

    @Override
    public String toShortString() {
        return String.format("[%s]", getButtonValue());
    }
}
