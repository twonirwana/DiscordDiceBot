package de.janno.discord.bot.dice;

import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roller;
import de.janno.evaluator.dice.random.NumberSupplier;
import io.avaje.config.Config;

public class NoCacheDiceEvaluator implements ErrorCatchingDiceEvaluator {

    private final DiceEvaluator diceEvaluator;

    public NoCacheDiceEvaluator(NumberSupplier numberSupplier) {
        diceEvaluator = new DiceEvaluator(numberSupplier, Config.getInt("diceEvaluator.maxNumberOfDice", 1000),
                Config.getInt("diceEvaluator.maxNumberOfElements", 10_000),
                Config.getBool("diceEvaluator.keepChildrenRolls", false));
    }

    @Override
    public RollerOrError get(String expression) {
        try {
            Roller roller = diceEvaluator.buildRollSupplier(expression);
            // todo make stable, GivenDiceNumberSupplier removes the number after each roll
            // roller.roll();
            return new RollerOrError(expression, roller, true, null, null);
        } catch (ExpressionException e) {
            String errorLocation = DiceEvaluatorAdapter.getErrorLocationString(expression, e.getExpressionPosition());
            return new RollerOrError(expression, null, false, e.getMessage(), errorLocation);
        }
    }
}
