package de.janno.discord.bot.dice;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.DieIdDb;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.command.reroll.DieIdTypeAndValue;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.ImageResultCreator;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.evaluator.dice.*;
import de.janno.evaluator.dice.random.GivenDiceNumberSupplier;
import io.avaje.config.Config;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DiceEvaluatorAdapter {

    public final static String LABEL_DELIMITER = "@";
    private final static ImageResultCreator IMAGE_RESULT_CREATOR = new ImageResultCreator();
    private final CachingDiceEvaluator diceEvaluator;

    public DiceEvaluatorAdapter(CachingDiceEvaluator cachingDiceEvaluator) {
        this.diceEvaluator = cachingDiceEvaluator;
    }

    public static @NonNull String getExpressionFromExpressionWithOptionalLabel(String expressionWithOptionalLabel) {
        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(LABEL_DELIMITER);
            return expressionWithOptionalLabel.substring(0, firstDelimiter);
        }
        return expressionWithOptionalLabel;
    }

    public static Optional<String> validateLabel(@NonNull String expressionWithOptionalLabel, @NonNull Locale userLocale) {
        String label;
        String diceExpression;

        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            String[] split = expressionWithOptionalLabel.split(LABEL_DELIMITER);
            if (StringUtils.countMatches(expressionWithOptionalLabel, LABEL_DELIMITER) > 1) {
                return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.toManyAt", userLocale, expressionWithOptionalLabel));
            }
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


    private static Optional<String> getLabelFromExpressionWithOptionalLabel(String expressionWithOptionalLabel) {
        if (expressionWithOptionalLabel.contains(LABEL_DELIMITER)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(LABEL_DELIMITER);
            String label = expressionWithOptionalLabel.substring(firstDelimiter + LABEL_DELIMITER.length());
            if (!label.isEmpty()) {
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    private static String getResult(Roll roll, boolean sumUp) {
        if (sumUp && allElementsAreDecimal(roll) && allElementsHaveNoTag(roll)) {
            return roll.getElements().stream().flatMap(r -> r.asDecimal().stream()).reduce(BigDecimal::add).map(BigDecimal::toString).orElse(roll.getResultString());
        }
        return roll.getResultString();
    }

    private static boolean allElementsAreDecimal(Roll roll) {
        return roll.getElements().stream().allMatch(r -> r.asDecimal().isPresent());
    }

    private static boolean allElementsHaveNoTag(Roll roll) {
        return roll.getElements().stream().allMatch(r -> RollElement.NO_TAG.equals(r.getTag()));
    }

    public static String getHelp() {
        return "```\n" + DiceEvaluator.getHelpText() + "\n```";
    }

    public static String getErrorLocationString(String expression, ExpressionPosition expressionPosition) {
        final int contextSize = Config.getInt("diceEvaluator.errorContextSize", 6);
        expression = expression.trim();
        int location = expressionPosition.getStartInc();
        String errorValue = expressionPosition.getValue();
        int leftContextStart = Math.max(location - contextSize, 0);
        int rightContextEnd = Math.min(location + errorValue.length() + contextSize, expression.length());
        String left = expression.substring(leftContextStart, location);
        String right = expression.substring(location + errorValue.length(), rightContextEnd);
        return String.format("%s__%s__%s",
                left.isBlank() ? "" : ("`" + left + "`"),
                errorValue,
                right.isBlank() ? "" : ("`" + right + "`")
        );
    }

    private static List<DieIdTypeAndValue> getDieIdAndValue(List<Roll> rolls) {
        return rolls.stream()
                .flatMap(r -> r.getRandomElementsInRoll().stream())
                .flatMap(Collection::stream)
                .map(r -> new DieIdTypeAndValue(DieIdDb.fromDieId(
                        r.getDieId()),
                        r.getRollElement().getValue(),
                        r.getNumberSupplierValue(),
                        r.getMaxInc(),
                        r.getRandomSelectedFrom()
                ))
                .toList();
    }


    public Optional<String> validateListOfExpressions(List<String> expressions, String helpCommand, @NonNull Locale userLocale) {
        if (expressions.isEmpty()) {
            return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.missingExpression", userLocale, helpCommand));
        }
        for (String startOptionString : expressions) {
            if (startOptionString.contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER)) {
                return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.invalidCharacter", userLocale, startOptionString));
            }
            Optional<String> diceParserValidation = validateDiceExpressionWitOptionalLabel(startOptionString, helpCommand, userLocale);
            if (diceParserValidation.isPresent()) {
                return diceParserValidation;
            }
        }

        return Optional.empty();
    }

    public Optional<String> validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String helpCommand, @NonNull Locale userLocale) {
        Optional<String> validateLabel = validateLabel(expressionWithOptionalLabel, userLocale);
        if (validateLabel.isPresent()) {
            return validateLabel;
        }
        String diceExpression = DiceEvaluatorAdapter.getExpressionFromExpressionWithOptionalLabel(expressionWithOptionalLabel);
        return validateDiceExpression(diceExpression, helpCommand, userLocale);

    }

    public Optional<String> validateDiceExpression(String expression, String helpCommand, @NonNull Locale userLocale) {
        RollerOrError rollerOrError = diceEvaluator.get(expression);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
            if (!Strings.isNullOrEmpty(expression)) {
                BotMetrics.incrementInvalidExpression(expression);
            }
            return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.invalid", userLocale, rollerOrError.getErrorExpressionContext(), rollerOrError.getErrorMessage(), helpCommand));
        }
    }

    public Optional<String> shortValidateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, @NonNull Locale userLocale) {
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
        RollerOrError rollerOrError = diceEvaluator.get(diceExpression);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
            //no invalid metric increment because this is called by each letter typed
            //not enough space to add the error location, which is easy to see for the user because it should be the last input
            return Optional.of(rollerOrError.getErrorMessage());
        }
    }

    public RollAnswer answerRollWithOptionalLabelInExpression(String expression, boolean sumUp, AnswerFormatType answerFormatType, DiceStyleAndColor diceStyleAndColor, Locale userLocale) {
        String diceExpression = getExpressionFromExpressionWithOptionalLabel(expression);
        String label = getLabelFromExpressionWithOptionalLabel(expression).orElse(null);
        return answerRollWithGivenLabel(diceExpression, label, sumUp, answerFormatType, diceStyleAndColor, userLocale);
    }

    public RollAnswer answerRollWithGivenLabel(String expression,
                                               @Nullable String label,
                                               boolean sumUp,
                                               AnswerFormatType answerFormatType,
                                               DiceStyleAndColor styleAndColor,
                                               @NonNull Locale userLocale) {
        return answerRollWithGivenLabel(expression, label, sumUp, answerFormatType, styleAndColor, userLocale, List.of());

    }

    public RollAnswer answerRollWithGivenLabel(String expression,
                                               @Nullable String label,
                                               boolean sumUp,
                                               AnswerFormatType answerFormatType,
                                               DiceStyleAndColor styleAndColor,
                                               @NonNull Locale userLocale,
                                               List<DiceIdAndValue> dieAndValues) {
        try {
            final RollerOrError rollerOrError = diceEvaluator.get(expression);

            final List<Roll> rolls;
            if (rollerOrError.getRoller() != null) {
                if (dieAndValues.isEmpty()) {
                    rolls = rollerOrError.getRoller().roll().getRolls();
                } else {
                    GivenDiceNumberSupplier givenDiceNumberSupplier = diceEvaluator.getGivenDiceNumberSuppler(dieAndValues);
                    rolls = rollerOrError.getRoller().roll(givenDiceNumberSupplier).getRolls();
                }
            } else {
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(rollerOrError.getExpression())
                        .errorMessage(rollerOrError.getErrorMessage())
                        .build();
            }

            BotMetrics.incrementUseImageResultMetricCounter(styleAndColor);
            Supplier<? extends InputStream> diceImage = IMAGE_RESULT_CREATOR.getImageForRoll(rolls, styleAndColor);
            if (rolls.size() == 1) {
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expression)
                        .expressionLabel(label)
                        .image(diceImage)
                        .warning(getWarningFromRoll(rolls, userLocale))
                        .result(getResult(rolls.getFirst(), sumUp))
                        .rollDetails(getRandomElementsString(rolls.getFirst().getRandomElementsInRoll()))
                        .dieIdTypeAndValues(getDieIdAndValue(rolls))
                        .build();
            } else {
                List<RollAnswer.RollResults> multiRollResults = rolls.stream()
                        .map(r -> new RollAnswer.RollResults(r.getExpression(), getResult(r, sumUp), getRandomElementsString(r.getRandomElementsInRoll())))
                        .collect(ImmutableList.toImmutableList());
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expression)
                        .expressionLabel(label)
                        .warning(getWarningFromRoll(rolls, userLocale))
                        .multiRollResults(multiRollResults)
                        .dieIdTypeAndValues(getDieIdAndValue(rolls))
                        .build();
            }
        } catch (ExpressionException e) {
            return RollAnswer.builder()
                    .answerFormatType(answerFormatType)
                    .expression(expression)
                    .errorMessage(e.getMessage())
                    .errorLocation(getErrorLocationString(expression, e.getExpressionPosition()))
                    .build();
        }
    }

    public boolean isValidExpression(String expression) {
        return diceEvaluator.get(expression).isValid();
    }

    private String getWarningFromRoll(List<Roll> rolls, Locale userLocale) {
        if (rolls.stream().allMatch(r -> r.getRandomElementsInRoll().isEmpty())) {
            return I18n.getMessage("diceEvaluator.reply.warning.noRandomElement", userLocale);
        }
        return null;
    }

    private String getRandomElementsString(ImmutableList<ImmutableList<RandomElement>> randomElementsInRoll) {
        if (randomElementsInRoll.size() == 1) {
            return randomElementsInRoll.getFirst().stream().map(r -> r.getRollElement().toStringWithColorAndTag()).toList().toString();
        }
        return randomElementsInRoll.stream().map(l -> l.stream().map(r -> r.getRollElement().toStringWithColorAndTag()).toList().toString()).collect(Collectors.joining(" "));
    }

}
