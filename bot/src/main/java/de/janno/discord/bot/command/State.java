package de.janno.discord.bot.command;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;

/**
 * The current state of the interaction. The state can be used over multiple interactions.
 */
@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class State implements Serializable {

    /**
     * The value of the last button click
     */
    @NonNull
    private final String buttonValue;

    public String toShortString() {
        return String.format("[%s]", getButtonValue());
    }
}
