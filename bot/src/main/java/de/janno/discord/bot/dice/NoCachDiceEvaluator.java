package de.janno.discord.bot.dice;

import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roller;
import de.janno.evaluator.dice.random.NumberSupplier;

public class NoCachDiceEvaluator implements ErrorCatchingDiceEvaluator {

    private final DiceEvaluator diceEvaluator;

    public NoCachDiceEvaluator(NumberSupplier numberSupplier) {
        //todo config
        diceEvaluator = new DiceEvaluator(numberSupplier, 1000,10000, false);
    }

    @Override
    public RollerOrError get(String expression) {
        try {
            Roller roller = diceEvaluator.buildRollSupplier(expression);
            // todo make stable, GivenDiceNumberSupplier removes the number after each roll
           // roller.roll();
            return new RollerOrError(expression, roller, true, null);
        } catch (ExpressionException e) {
            String errorLocation = DiceEvaluatorAdapter.getErrorLocationString(expression, e.getExpressionPosition());
            //todo full expression or errorLocation
            return new RollerOrError(errorLocation, null, false, e.getMessage());
        }
    }
}
