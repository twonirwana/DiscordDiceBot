package de.janno.discord.command;

/**
 * The current state of the interaction. The state can be used over multiple interactions.
 */
public interface IState {
    String toShortString();
}
