package de.janno.discord.bot.dice;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
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

    public static Optional<String> validateLabel(@NonNull String expressionWithOptionalLabel, @NonNull Locale userLocale) {
        //todo remove duplicate
        String label;
        String diceExpression;

        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            String[] split = expressionWithOptionalLabel.split(LABEL_DELIMITER);
            if (split.length != 2) {
                return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.toManyAt", userLocale, expressionWithOptionalLabel));
            }
            label = split[1].trim();
            diceExpression = split[0].trim();
        } else {
            label = expressionWithOptionalLabel;
            diceExpression = expressionWithOptionalLabel;
        }
        if (label.isBlank()) {
            return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.blankLabel", userLocale, expressionWithOptionalLabel));
        }
        if (diceExpression.isBlank()) {
            return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.blankExpression", userLocale, expressionWithOptionalLabel));
        }
        return Optional.empty();
    }

    public RollAnswer answerRollWithGivenLabel(String expression, @Nullable String label, boolean sumUp, DiceParserSystem system, AnswerFormatType answerFormatType, DiceStyleAndColor diceStyleAndColor, Locale userLocale) {
        BotMetrics.incrementDiceParserSystemCounter(system);
        return switch (system) {
            case DICE_EVALUATOR ->
                    diceEvaluatorAdapter.answerRollWithGivenLabel(expression, label, sumUp, answerFormatType, diceStyleAndColor, userLocale);
            case DICEROLL_PARSER -> parserHelper.answerRollWithGivenLabel(expression, label, answerFormatType);
        };
    }

    public Optional<String> validateListOfExpressions(List<String> expressions, String helpCommand, DiceParserSystem system, @NonNull Locale userLocale) {
        if (expressions.isEmpty()) {
            return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.missingExpression", userLocale, helpCommand));
        }
        for (String startOptionString : expressions) {
            if (startOptionString.contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER)) {
                return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.invalidCharacter", userLocale, startOptionString));
            }
            Optional<String> diceParserValidation = validateDiceExpressionWitOptionalLabel(startOptionString, helpCommand, system, userLocale);
            if (diceParserValidation.isPresent()) {
                return diceParserValidation;
            }
        }

        return Optional.empty();
    }

    public Optional<String> validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String helpCommand, DiceParserSystem system, @NonNull Locale userLocale) {
        Optional<String> validateLabel = validateLabel(expressionWithOptionalLabel, userLocale);
        if (validateLabel.isPresent()) {
            return validateLabel;
        }
        String diceExpression = DiceSystemAdapter.getExpressionFromExpressionWithOptionalLabel(expressionWithOptionalLabel);
        return switch (system) {
            case DICE_EVALUATOR -> diceEvaluatorAdapter.validateDiceExpression(diceExpression, helpCommand, userLocale);
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
