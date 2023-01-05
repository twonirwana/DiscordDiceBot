package de.janno.discord.bot.dice.image;

import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.random.GivenNumberSupplier;
import org.junit.jupiter.api.Test;

import java.util.List;

class ImageResultCreatorTest {

    @Test
    void convert() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1,2,3,4,5,6,1,2,3,4,5,6), 1000).evaluate("6d6+1d6+3d6");

        ImageResultCreator underTest = new ImageResultCreator();
        underTest.getImageForRoll(rolls);
    }

    @Test
    void convertD100_00() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d100");

        ImageResultCreator underTest = new ImageResultCreator();
        underTest.getImageForRoll(rolls);
    }

    @Test
    void convertD100_01() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        ImageResultCreator underTest = new ImageResultCreator();
        underTest.getImageForRoll(rolls);
    }

    @Test
    void convertD100_99() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        ImageResultCreator underTest = new ImageResultCreator();
        underTest.getImageForRoll(rolls);
    }
}