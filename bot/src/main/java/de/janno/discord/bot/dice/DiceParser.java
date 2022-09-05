package de.janno.discord.bot.dice;

import dev.diceroll.parser.ResultTree;

public class DiceParser implements Dice {

    @Override
    public ResultTree detailedRoll(String input) {
        return dev.diceroll.parser.Dice.detailedRoll(input);
    }
}
