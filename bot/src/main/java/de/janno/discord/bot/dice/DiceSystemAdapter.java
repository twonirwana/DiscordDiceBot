package de.janno.discord.bot.dice;

import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.evaluator.dice.NumberSupplier;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class DiceSystemAdapter {

    public final static String LABEL_DELIMITER = "@";
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;
    private final DiceParserAdapter parserHelper;

    public DiceSystemAdapter(NumberSupplier numberSupplier, int maxNumberOfDice, Dice dice) {
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(numberSupplier, maxNumberOfDice);
        this.parserHelper = new DiceParserAdapter(dice);
    }

    public static @NonNull String getExpressionFromExpressionWithOptionalLabel(String expressionWithOptionalLabel) {
        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(LABEL_DELIMITER);
            return expressionWithOptionalLabel.substring(0, firstDelimiter);
        }
        return expressionWithOptionalLabel;
    }

    public String getHelpText(DiceParserSystem system) {
        return switch (system) {
            case DICE_EVALUATOR -> DiceEvaluatorAdapter.getHelp();
            case DICEROLL_PARSER -> DiceParserAdapter.HELP;
        };
    }

    public EmbedDefinition answerRoll(String expression, boolean sumUp, DiceParserSystem system) {
        //todo metrics
        return switch (system) {
            case DICE_EVALUATOR -> diceEvaluatorAdapter.answerRoll(expression, LABEL_DELIMITER, sumUp);
            case DICEROLL_PARSER -> parserHelper.rollWithOptionalLablel(expression, LABEL_DELIMITER);
        };
    }

    public EmbedDefinition answerRollWithOptionalLabel(String expression, @Nullable String label, boolean sumUp, DiceParserSystem system) {
        //todo metrics
        return switch (system) {
            case DICE_EVALUATOR -> diceEvaluatorAdapter.answerRollWithOptionalLabel(expression, label, sumUp);
            case DICEROLL_PARSER -> parserHelper.roll(expression, label);
        };
    }

    public Optional<String> validateExpression(String expression, String helpCommand, DiceParserSystem system) {
        return validateListOfExpressions(List.of(expression), helpCommand, system);
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

    public Optional<String> validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String helpCommand, DiceParserSystem system) {
        String label;
        String diceExpression;

        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            String[] split = expressionWithOptionalLabel.split(LABEL_DELIMITER);
            if (split.length != 2) {
                return Optional.of(String.format("The button definition '%s' should have the diceExpression@Label", expressionWithOptionalLabel));
            }
            label = split[1].trim();
            diceExpression = split[0].trim();
        } else {
            label = expressionWithOptionalLabel;
            diceExpression = expressionWithOptionalLabel;
        }
        if (label.length() > 80) {
            return Optional.of(String.format("Label for '%s' is to long, max number of characters is 80", expressionWithOptionalLabel));
        }
        if (label.isBlank()) {
            return Optional.of(String.format("Label for '%s' requires a visible name", expressionWithOptionalLabel));
        }
        if (diceExpression.isBlank()) {
            return Optional.of(String.format("Dice expression for '%s' is empty", expressionWithOptionalLabel));
        }
        return switch (system) {
            case DICE_EVALUATOR -> diceEvaluatorAdapter.validateDiceExpression(diceExpression, helpCommand);
            case DICEROLL_PARSER -> parserHelper.validateDiceExpression(diceExpression, helpCommand);
        };
    }

    public boolean isValidExpression(String input, DiceParserSystem system) {
        return switch (system) {
            case DICE_EVALUATOR -> diceEvaluatorAdapter.validExpression(input);
            case DICEROLL_PARSER -> parserHelper.validExpression(input);
        };
    }

}
