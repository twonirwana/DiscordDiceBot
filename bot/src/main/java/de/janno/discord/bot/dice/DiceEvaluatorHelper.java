package de.janno.discord.bot.dice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.evaluator.ExpressionException;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.NumberSupplier;
import de.janno.evaluator.dice.Result;
import de.janno.evaluator.dice.ResultElement;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public class DiceEvaluatorHelper {

    private final DiceEvaluator diceEvaluator;

    public DiceEvaluatorHelper(NumberSupplier numberSupplier, int maxNumberOfDice) {
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

    private static String getTitleResult(Result result) {
        if (result.getElements().stream().allMatch(r -> r.asInteger().isPresent()) &&
                result.getElements().stream().allMatch(r -> ResultElement.NO_COLOR.equals(r.getColor()))) {
            return String.valueOf(result.getElements().stream().flatMap(r -> r.asInteger().stream()).mapToInt(i -> i).sum());
        } else {
            return result.getResultString();
        }
    }

    private static String getDetailResult(Result result) {
        return result.getRandomElementsString();
    }

    public EmbedDefinition answerRoll(String expression, String labelDelimiter) {
        EmbedDefinition answer;
        String diceExpression = getExpressionFromExpressionWithOptionalLabel(expression, labelDelimiter);
        Optional<String> label = getLabelFromExpressionWithOptionalLabel(expression, labelDelimiter);
        try {
            List<Result> results = diceEvaluator.evaluate(diceExpression);
            if (results.size() == 1) {
                String title = label.map(l -> String.format("%s: %s", l, diceExpression)).orElse(diceExpression);
                answer = EmbedDefinition.builder()
                        .title("%s = %s".formatted(title, getTitleResult(results.get(0))))
                        .description(getDetailResult(results.get(0)))
                        .build();
            } else {
                List<EmbedDefinition.Field> fields = results.stream()
                        .limit(25) //max number of embedFields
                        .map(r -> new EmbedDefinition.Field(r.getExpression() + " = " + getTitleResult(r), getDetailResult(r), false))
                        .collect(ImmutableList.toImmutableList());
                answer = EmbedDefinition.builder()
                        .title(label.orElse(diceExpression))
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
}
