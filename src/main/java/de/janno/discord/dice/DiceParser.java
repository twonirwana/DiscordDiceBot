package de.janno.discord.dice;

import dev.diceroll.parser.Dice;
import dev.diceroll.parser.ResultTree;

public class DiceParser implements IDice {

    @Override
    public ResultTree detailedRoll(String input) {
        return Dice.detailedRoll(input);
    }

    @Override
    public int roll(String input) {
        return Dice.roll(input);
    }
}
