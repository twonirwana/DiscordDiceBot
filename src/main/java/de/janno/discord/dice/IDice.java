package de.janno.discord.dice;

import dev.diceroll.parser.ResultTree;

public interface IDice {
    ResultTree detailedRoll(String input);

    int roll(String input);
}
