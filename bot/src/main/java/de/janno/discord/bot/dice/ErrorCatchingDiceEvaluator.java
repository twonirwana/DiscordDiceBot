package de.janno.discord.bot.dice;

public interface ErrorCatchingDiceEvaluator {
    RollerOrError get(String expression);
}
