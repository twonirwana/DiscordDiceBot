package de.janno.discord.bot.dice;

//todo rename
public interface ErrorCatchingDiceEvaluator {
    RollerOrError get(String expression);
}
