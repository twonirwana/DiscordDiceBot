package de.janno.discord.bot.dice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.dice.image.ImageResultCreator;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.RollElement;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static de.janno.discord.bot.dice.DiceSystemAdapter.LABEL_DELIMITER;

@Slf4j
public class DiceEvaluatorAdapter {

    private final static ImageResultCreator IMAGE_RESULT_CREATOR = new ImageResultCreator();
    private final CachingDiceEvaluator cachingDiceEvaluator;

    public DiceEvaluatorAdapter(CachingDiceEvaluator cachingDiceEvaluator) {
        this.cachingDiceEvaluator = cachingDiceEvaluator;
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

    private static String getResult(Roll roll, boolean sumUp) {
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

    public static String getHelp() {
        return "```\n" + DiceEvaluator.getHelpText() + "\n```";
    }

    public Optional<String> validateDiceExpression(String expression, String helpCommand) {
        RollerOrError rollerOrError = cachingDiceEvaluator.get(expression);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
            return Optional.of(String.format("The following expression is invalid: '%s'. The error is: %s. Use %s to get more information on how to use the command.", expression, rollerOrError.getErrorMessage(), helpCommand));
        }
    }


    public Optional<String> shortValidateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel) {
        String label;
        String diceExpression;

        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            String[] split = expressionWithOptionalLabel.split(LABEL_DELIMITER);
            if (split.length != 2) {
                return Optional.of("The expression must have the format diceExpression@Label");
            }
            label = split[1].trim();
            diceExpression = split[0].trim();
        } else {
            label = expressionWithOptionalLabel;
            diceExpression = expressionWithOptionalLabel;
        }
        if (label.isBlank()) {
            return Optional.of("Lable must not be empty");
        }
        if (diceExpression.isBlank()) {
            return Optional.of("Expression must not be empty");
        }
        RollerOrError rollerOrError = cachingDiceEvaluator.get(expressionWithOptionalLabel);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
            return Optional.of(rollerOrError.getErrorMessage());
        }
    }

    public RollAnswer answerRollWithOptionalLabelInExpression(String expression, String labelDelimiter, boolean sumUp, AnswerFormatType answerFormatType, ResultImage resultImage) {
        String diceExpression = getExpressionFromExpressionWithOptionalLabel(expression, labelDelimiter);
        String label = getLabelFromExpressionWithOptionalLabel(expression, labelDelimiter).orElse(null);
        return answerRollWithGivenLabel(diceExpression, label, sumUp, answerFormatType, resultImage);
    }

    public RollAnswer answerRollWithGivenLabel(String expression, @Nullable String label, boolean sumUp, AnswerFormatType answerFormatType, ResultImage resultImage) {

        try {
            final RollerOrError rollerOrError = cachingDiceEvaluator.get(expression);

            final List<Roll> rolls;
            if (rollerOrError.getRoller() != null) {
                rolls = rollerOrError.getRoller().roll();
            } else {
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expression)
                        .errorMessage(rollerOrError.getErrorMessage())
                        .build();
            }

            BotMetrics.incrementUseImageResultMetricCounter(resultImage);
            File diceImage = null;
            if (!resultImage.equals(ResultImage.none)) {
                diceImage = IMAGE_RESULT_CREATOR.getImageForRoll(rolls, resultImage);
            }
            if (rolls.size() == 1) {
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expression)
                        .expressionLabel(label)
                        .file(diceImage)
                        .result(getResult(rolls.get(0), sumUp))
                        .rollDetails(rolls.get(0).getRandomElementsString())
                        .build();
            } else {
                List<RollAnswer.RollResults> multiRollResults = rolls.stream()
                        .map(r -> new RollAnswer.RollResults(r.getExpression(), getResult(r, sumUp), r.getRandomElementsString()))
                        .collect(ImmutableList.toImmutableList());
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expression)
                        .expressionLabel(label)
                        .multiRollResults(multiRollResults)
                        .build();
            }
        } catch (ExpressionException | ArithmeticException e) {
            return RollAnswer.builder()
                    .answerFormatType(answerFormatType)
                    .expression(expression)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }


    public boolean validExpression(String expression) {
        return cachingDiceEvaluator.get(expression).isValid();
    }

}
