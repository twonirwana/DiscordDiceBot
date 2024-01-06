package de.janno.discord.bot.dice;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.ImageResultCreator;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.RollElement;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

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
        if (sumUp && allElementsAreDecimal(roll) && allElementsHaveNoColor(roll)) {
            return roll.getElements().stream().flatMap(r -> r.asDecimal().stream()).reduce(BigDecimal::add).map(BigDecimal::toString).orElse(roll.getResultString());
        }
        return roll.getResultString();
    }

    private static boolean allElementsAreDecimal(Roll roll) {
        return roll.getElements().stream().allMatch(r -> r.asDecimal().isPresent());
    }

    private static boolean allElementsHaveNoColor(Roll roll) {
        return roll.getElements().stream().allMatch(r -> RollElement.NO_COLOR.equals(r.getColor()));
    }

    public static String getHelp() {
        return "```\n" + DiceEvaluator.getHelpText() + "\n```";
    }

    public Optional<String> validateDiceExpression(String expression, String helpCommand, @NonNull Locale userLocale) {
        RollerOrError rollerOrError = cachingDiceEvaluator.get(expression);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
            if(!Strings.isNullOrEmpty(expression)){
                BotMetrics.incrementInvalidExpression(expression);
            }
            return Optional.of(I18n.getMessage("diceEvaluator.reply.validation.invalid", userLocale, expression, rollerOrError.getErrorMessage(), helpCommand));
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
        RollerOrError rollerOrError = cachingDiceEvaluator.get(expressionWithOptionalLabel);
        if (rollerOrError.isValid()) {
            return Optional.empty();
        } else {
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
                rolls = rollerOrError.getRoller().roll();
            } else {
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expression)
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
                        .rollDetails(rolls.getFirst().getRandomElementsString())
                        .build();
            } else {
                List<RollAnswer.RollResults> multiRollResults = rolls.stream()
                        .map(r -> new RollAnswer.RollResults(r.getExpression(), getResult(r, sumUp), r.getRandomElementsString()))
                        .collect(ImmutableList.toImmutableList());
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expression)
                        .expressionLabel(label)
                        .warning(getWarningFromRoll(rolls, userLocale))
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

    private String getWarningFromRoll(List<Roll> rolls, Locale userLocale) {
        if (rolls.stream().allMatch(r -> r.getRandomElementsInRoll().getRandomElements().isEmpty())) {
            return I18n.getMessage("diceEvaluator.reply.warning.noRandomElement", userLocale);
        }
        return null;
    }

}
