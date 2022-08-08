package de.janno.discord.bot.command;

import lombok.*;

/**
 * The current state of the interaction. The state can be used over multiple interactions.
 */
@EqualsAndHashCode
@Getter
@AllArgsConstructor
@ToString
public class State {

    /**
     * The value of the last button click
     */
    @NonNull
    private final String buttonValue;

    public String toShortString() {
        return String.format("[%s]", getButtonValue());
    }
}
