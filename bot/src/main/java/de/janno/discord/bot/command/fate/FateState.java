package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class FateState extends State {
    public FateState(String buttonValue) {
        super(buttonValue);
    }

    public Integer getModifier() {
        return Integer.parseInt(getButtonValue());
    }
}