package de.janno.discord.bot.dice;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class DiceSystemAdapter {

    public final static String LABEL_DELIMITER = "@";
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;
    private final DiceParserAdapter parserHelper;

    public DiceSystemAdapter(CachingDiceEvaluator cachingDiceEvaluator, Dice dice) {
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
        this.parserHelper = new DiceParserAdapter(dice);
    }

    public static @NonNull String getExpressionFromExpressionWithOptionalLabel(String expressionWithOptionalLabel) {
        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(LABEL_DELIMITER);
            return expressionWithOptionalLabel.substring(0, firstDelimiter);
        }
        return expressionWithOptionalLabel;
    }

    public RollAnswer answerRollWithGivenLabel(String expression, @Nullable String label, boolean sumUp, DiceParserSystem system, AnswerFormatType answerFormatType, DiceStyleAndColor diceStyleAndColor) {
        BotMetrics.incrementDiceParserSystemCounter(system);
        return switch (system) {
            case DICE_EVALUATOR ->
                    diceEvaluatorAdapter.answerRollWithGivenLabel(expression, label, sumUp, answerFormatType, diceStyleAndColor);
            case DICEROLL_PARSER -> parserHelper.answerRollWithGivenLabel(expression, label, answerFormatType);
        };
    }

    public Optional<String> validateListOfExpressions(List<String> expressions, String helpCommand, DiceParserSystem system) {
        if (expressions.isEmpty()) {
            return Optional.of(String.format("You must configure at least one dice expression. Use '%s' to get more information on how to use the command.", helpCommand));
        }
        for (String startOptionString : expressions) {
            if (startOptionString.contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER)) {
                return Optional.of(String.format("The button definition '%s' is not allowed to contain ','", startOptionString));
            }
            Optional<String> diceParserValidation = validateDiceExpressionWitOptionalLabel(startOptionString, helpCommand, system);
            if (diceParserValidation.isPresent()) {
                return diceParserValidation;
            }
        }

        return Optional.empty();
    }

    public static Optional<String> validateLabel(@NonNull String expressionWithOptionalLabel) {
        //todo remove duplicate
        String label;
        String diceExpression;

        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            String[] split = expressionWithOptionalLabel.split(LABEL_DELIMITER);
            if (split.length != 2) {
                return Optional.of(String.format("The expression '%s' should have the diceExpression@Label", expressionWithOptionalLabel));
            }
            label = split[1].trim();
            diceExpression = split[0].trim();
        } else {
            label = expressionWithOptionalLabel;
            diceExpression = expressionWithOptionalLabel;
        }
        if (label.isBlank()) {
            return Optional.of(String.format("Label for '%s' requires a visible name", expressionWithOptionalLabel));
        }
        if (diceExpression.isBlank()) {
            return Optional.of(String.format("Dice expression for '%s' is empty", expressionWithOptionalLabel));
        }
        return Optional.empty();
    }

    public Optional<String> validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String helpCommand, DiceParserSystem system) {
        Optional<String> validateLabel = validateLabel(expressionWithOptionalLabel);
        if (validateLabel.isPresent()) {
            return validateLabel;
        }
        String diceExpression = DiceSystemAdapter.getExpressionFromExpressionWithOptionalLabel(expressionWithOptionalLabel);
        return switch (system) {
            case DICE_EVALUATOR -> diceEvaluatorAdapter.validateDiceExpression(diceExpression, helpCommand);
            case DICEROLL_PARSER -> parserHelper.validateDiceExpression(diceExpression);
        };
    }

    public boolean isValidExpression(String input, DiceParserSystem system) {
        return switch (system) {
            case DICE_EVALUATOR -> diceEvaluatorAdapter.validExpression(input);
            case DICEROLL_PARSER -> parserHelper.validExpression(input);
        };
    }

}
