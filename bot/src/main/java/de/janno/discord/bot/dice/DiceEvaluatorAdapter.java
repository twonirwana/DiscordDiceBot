package de.janno.discord.bot.dice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.evaluator.ExpressionException;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.NumberSupplier;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.RollElement;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class DiceEvaluatorAdapter {

    private final DiceEvaluator diceEvaluator;

    public DiceEvaluatorAdapter(NumberSupplier numberSupplier, int maxNumberOfDice) {
        this.diceEvaluator = new DiceEvaluator(numberSupplier, maxNumberOfDice);
    }

    private static @NonNull String getExpressionFromExpressionWithOptionalLabel(String expressionWithOptionalLabel, String labelDelimiter) {
        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(labelDelimiter);
            return expressionWithOptionalLabel.substring(0, firstDelimiter);
        }
        return expressionWithOptionalLabel;
    }

    private static Optional<String> getLabelFromExpressionWithOptionalLabel(String expressionWithOptionalLabel, String labelDelimiter) {
        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(labelDelimiter);
            String label = expressionWithOptionalLabel.substring(firstDelimiter + labelDelimiter.length());
            if (label.length() > 0) {
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    private static String getTitleResult(Roll roll, boolean sumUp) {
        if (sumUp && allElementsAreIntegers(roll) && allElementsHaveNoColor(roll)) {
            return String.valueOf(roll.getElements().stream().flatMap(r -> r.asInteger().stream()).mapToInt(i -> i).sum());
        }
        return roll.getResultString();
    }

    private static boolean allElementsAreIntegers(Roll roll) {
        return roll.getElements().stream().allMatch(r -> r.asInteger().isPresent());
    }

    private static boolean allElementsHaveNoColor(Roll roll) {
        return roll.getElements().stream().allMatch(r -> RollElement.NO_COLOR.equals(r.getColor()));
    }

    private static String getDetailResult(Roll result) {
        return result.getRandomElementsString();
    }

    public static String getHelp() {
        return "```\n" + DiceEvaluator.getHelpText() + "\n```\nSee here: https://github.com/twonirwana/DiceEvaluator";
    }

    public Optional<String> validateDiceExpression(String expression, String helpCommand) {
        try {
            diceEvaluator.evaluate(expression);
            return Optional.empty();
        } catch (ExpressionException | ArithmeticException e) {
            return Optional.of(String.format("The following expression is invalid: '%s'. The error is: %s. Use %s to get more information on how to use the command.", expression, e.getMessage(), helpCommand));
        }
    }

    public EmbedDefinition answerRoll(String expression, String labelDelimiter, boolean sumUp) {
        String diceExpression = getExpressionFromExpressionWithOptionalLabel(expression, labelDelimiter);
        String label = getLabelFromExpressionWithOptionalLabel(expression, labelDelimiter).orElse(null);
        return answerRollWithOptionalLabel(diceExpression, label, sumUp);
    }

    public EmbedDefinition answerRollWithOptionalLabel(String diceExpression, @Nullable String label, boolean sumUp) {
        EmbedDefinition answer;
        Optional<String> optionalLabel = Optional.ofNullable(label);
        try {
            List<Roll> rolls = diceEvaluator.evaluate(diceExpression);
            if (rolls.size() == 1) {
                String title = optionalLabel.map(l -> String.format("%s: %s", l, diceExpression)).orElse(diceExpression);
                answer = EmbedDefinition.builder()
                        .title("%s = %s".formatted(title, getTitleResult(rolls.get(0), sumUp)))
                        .description(getDetailResult(rolls.get(0)))
                        .build();
            } else {
                List<EmbedDefinition.Field> fields = rolls.stream()
                        .limit(25) //max number of embedFields
                        .map(r -> new EmbedDefinition.Field(r.getExpression() + " = " + getTitleResult(r, sumUp), getDetailResult(r), false))
                        .collect(ImmutableList.toImmutableList());
                answer = EmbedDefinition.builder()
                        .title(optionalLabel.orElse(diceExpression))
                        .fields(fields)
                        .build();
            }
        } catch (ExpressionException e) {
            answer = EmbedDefinition.builder()
                    .title("Error in: " + diceExpression)
                    .description(e.getMessage())
                    .build();
        }
        return answer;
    }


    public boolean validExpression(String expression) {
        try {
            diceEvaluator.evaluate(expression);
            return true;
        } catch (ExpressionException | ArithmeticException e) {
            return false;
        }
    }
}
