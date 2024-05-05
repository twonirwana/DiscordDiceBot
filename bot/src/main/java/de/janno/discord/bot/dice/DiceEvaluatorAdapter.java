package de.janno.discord.bot.dice;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.ImageResultCreator;
import de.janno.evaluator.dice.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        final int contextSize = 6;
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

    public Optional<String> validateDiceExpression(String expression, String helpCommand, @NonNull Locale userLocale) {
        RollerOrError rollerOrError = cachingDiceEvaluator.get(expression);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
            if (!Strings.isNullOrEmpty(expression)) {
                BotMetrics.incrementInvalidExpression(expression);
            }
            return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.invalid", userLocale, rollerOrError.getExpression(), rollerOrError.getErrorMessage(), helpCommand));
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
        RollerOrError rollerOrError = cachingDiceEvaluator.get(diceExpression);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
            //no invalid metric increment because this is called by each letter typed

            //todo maybe add the rollerOrError.getExpression to geht a better errorLocation
            return Optional.of(rollerOrError.getErrorMessage());
        }
    }

    public RollAnswer answerRollWithOptionalLabelInExpression(String expression, String labelDelimiter, boolean sumUp, AnswerFormatType answerFormatType, DiceStyleAndColor diceStyleAndColor, Locale userLocale) {
        String diceExpression = getExpressionFromExpressionWithOptionalLabel(expression, labelDelimiter);
        String label = getLabelFromExpressionWithOptionalLabel(expression, labelDelimiter).orElse(null);
        return answerRollWithGivenLabel(diceExpression, label, sumUp, answerFormatType, diceStyleAndColor, userLocale);
    }

    public RollAnswer answerRollWithGivenLabel(String expression,
                                               @Nullable String label,
                                               boolean sumUp,
                                               AnswerFormatType answerFormatType,
                                               DiceStyleAndColor styleAndColor,
                                               @NonNull Locale userLocale) {
        try {
            final RollerOrError rollerOrError = cachingDiceEvaluator.get(expression);

            final List<Roll> rolls;
            if (rollerOrError.getRoller() != null) {
                rolls = rollerOrError.getRoller().roll().getRolls();
            } else {
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(rollerOrError.getErrorMessage())
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

    public boolean validExpression(String expression) {
        return cachingDiceEvaluator.get(expression).isValid();
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
