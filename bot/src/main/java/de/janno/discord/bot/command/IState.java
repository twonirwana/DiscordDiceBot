package de.janno.discord.bot.command;

/**
 * The current state of the interaction. The state can be used over multiple interactions.
 */
public interface IState {

    //TODO abstract and button value as protected variable
    String toShortString();
}
