package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.command.IState;
import lombok.Value;

import java.util.Optional;

@Value
public class FateState implements IState {
    Integer modifier;

    @Override
    public String toShortString() {
        return String.format("[%s]", Optional.ofNullable(modifier).map(String::valueOf).orElse(""));
    }
}
