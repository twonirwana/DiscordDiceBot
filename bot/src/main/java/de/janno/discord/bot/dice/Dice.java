package de.janno.discord.bot.dice;

import dev.diceroll.parser.ResultTree;

public interface Dice {
    ResultTree detailedRoll(String input);
}
