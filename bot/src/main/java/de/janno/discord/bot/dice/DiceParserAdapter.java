package de.janno.discord.bot.dice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import dev.diceroll.parser.ResultTree;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class DiceParserAdapter {

    private static final Pattern BOOLEAN_EXPRESSION_PATTERN = Pattern.compile("(^.+?)((?:<=|>=|<>|<|>|=)\\d+\\?.+)+:(.+)$");
    private static final Pattern MULTI_ROLL_EXPRESSION_PATTERN = Pattern.compile("^(\\d+?)x\\[(.*)?]$");
    private static final Pattern VALUE_COMPERE_PATTER = Pattern.compile("(<=|>=|<>|<|>|=)(\\d+)\\?(.+)");
    private static final String MULTI_DIFF_EXPRESSION_DELIMITER = "&";
    private final Dice dice;

    public DiceParserAdapter() {
        this(new DiceParser());
    }

    @VisibleForTesting
    public DiceParserAdapter(Dice dice) {
        this.dice = dice;
    }

    @VisibleForTesting
    static boolean isMultipleRoll(String input) {
        if (input.contains("x[") && input.contains(MULTI_DIFF_EXPRESSION_DELIMITER)) {
            return false;
        }
        return isMultipleIdenticalRolls(input)
                || isMultipleDifferentRolls(input);
    }

    @VisibleForTesting
    static boolean isMultipleIdenticalRolls(String input) {
        return MULTI_ROLL_EXPRESSION_PATTERN.matcher(input).matches();
    }

    @VisibleForTesting
    static boolean isMultipleDifferentRolls(String input) {
        return input.contains(MULTI_DIFF_EXPRESSION_DELIMITER);
    }

    @VisibleForTesting
    static boolean isBooleanExpression(String input) {
        return BOOLEAN_EXPRESSION_PATTERN.matcher(input).matches();
    }

    @VisibleForTesting
    static int getNumberOfMultipleRolls(String input) {
        Matcher matcher = MULTI_ROLL_EXPRESSION_PATTERN.matcher(input);
        if (matcher.find()) {
            int numberOfRolls = Integer.parseInt(matcher.group(1));
            return Math.min(numberOfRolls, 25); //limited to 25 because that is the max number of embed discord fields
        }
        throw new IllegalArgumentException(String.format("Number of multiplier in '%s' not found", input));

    }

    @VisibleForTesting
    static String getInnerDiceExpressionFromMultiRoll(String input) {
        Matcher matcher = MULTI_ROLL_EXPRESSION_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(2);
        }
        throw new IllegalArgumentException(String.format("Inner expression in '%s' not found", input));
    }

    private static List<Integer> getBaseResults(ResultTree resultTree) {
        if (!resultTree.getResults().isEmpty()) {
            return resultTree.getResults().stream()
                    .flatMap(rt -> getBaseResults(rt).stream())
                    .collect(Collectors.toList());
        }
        return ImmutableList.of(resultTree.getValue());
    }

    public static @NonNull String getExpressionFromExpressionWithOptionalLabel(String expressionWithOptionalLabel, String labelDelimiter) {
        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(labelDelimiter);
            return expressionWithOptionalLabel.substring(0, firstDelimiter);
        }
        return expressionWithOptionalLabel;
    }

    public static Optional<String> getLabelFromExpressionWithOptionalLabel(String expressionWithOptionalLabel, String labelDelimiter) {
        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(labelDelimiter);
            return Optional.of(expressionWithOptionalLabel.substring(firstDelimiter + labelDelimiter.length()));
        }
        return Optional.empty();
    }

    public Optional<String> validateDiceExpression(String expression) {
        if (!validExpression(expression)) {
            return Optional.of(String.format("The following dice expression is invalid: '%s'", expression));
        }
        return Optional.empty();
    }

    public Optional<String> validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String labelDelimiter) {
        String label;
        String diceExpression;

        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            String[] split = expressionWithOptionalLabel.split(labelDelimiter);
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
        return validateDiceExpression(diceExpression);
    }

    public Optional<String> validateListOfExpressions(List<String> optionValues, String labelDelimiter) {
        if (optionValues.isEmpty()) {
            return Optional.of("You must configure at least one dice expression");
        }
        for (String startOptionString : optionValues) {
            if (startOptionString.contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER)) {
                return Optional.of(String.format("The button definition '%s' is not allowed to contain '%s'", startOptionString, BottomCustomIdUtils.CUSTOM_ID_DELIMITER));
            }
            Optional<String> diceParserValidation = validateDiceExpressionWitOptionalLabel(startOptionString, labelDelimiter);
            if (diceParserValidation.isPresent()) {
                return diceParserValidation;
            }
        }

        return Optional.empty();
    }

    private String removeLeadingPlus(String diceExpression) {
        if (diceExpression.startsWith("+")) {
            return diceExpression.substring(1);
        }
        return diceExpression;
    }

    private List<String> splitMultipleDifferentExpressions(String input) {
        return Arrays.stream(input.split(MULTI_DIFF_EXPRESSION_DELIMITER))
                .map(String::trim)
                .filter(s -> !Strings.isNullOrEmpty(s))
                .collect(Collectors.toList());
    }

    public RollAnswer answerRollWithOptionalLabelInExpression(String expressionAndOptionalLabel, String labelDelimiter, AnswerFormatType answerFormatType) {
        String label = DiceParserAdapter.getLabelFromExpressionWithOptionalLabel(expressionAndOptionalLabel, labelDelimiter).orElse(null);
        String diceExpression = DiceParserAdapter.getExpressionFromExpressionWithOptionalLabel(expressionAndOptionalLabel, labelDelimiter);
        return answerRollWithGivenLabel(diceExpression, label, answerFormatType);
    }

    public RollAnswer answerRollWithGivenLabel(String input, @Nullable String label, AnswerFormatType answerFormatType) {
        try {
            if (isMultipleRoll(input)) {
                List<ExpressionLabelResultAndDetails> expressionLabelResultAndDetails;
                if (isMultipleIdenticalRolls(input)) {
                    int numberOfRolls = getNumberOfMultipleRolls(input);
                    String innerExpression = getInnerDiceExpressionFromMultiRoll(input);
                    expressionLabelResultAndDetails = IntStream.range(0, numberOfRolls)
                            .mapToObj(i -> singleRoll(innerExpression, null))
                            .collect(Collectors.toList());
                } else if (isMultipleDifferentRolls(input)) {
                    expressionLabelResultAndDetails = splitMultipleDifferentExpressions(input).stream()
                            .map(s -> singleRoll(s, null))
                            .collect(Collectors.toList());
                } else {
                    throw new IllegalStateException(String.format("Can't find correct multi roll version for: %s", input));
                }

                List<RollAnswer.RollResults> multiRollResults = expressionLabelResultAndDetails.stream()
                        .map(r -> new RollAnswer.RollResults(r.getExpression(), r.getResult(), r.getDetails()))
                        .collect(ImmutableList.toImmutableList());
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(input)
                        .expressionLabel(Strings.isNullOrEmpty(label) ? "Multiple Results" : label)
                        .multiRollResults(multiRollResults)
                        .build();
            } else {
                ExpressionLabelResultAndDetails expressionLabelResultAndDetails = singleRoll(input, label);
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(expressionLabelResultAndDetails.getExpression())
                        .expressionLabel(expressionLabelResultAndDetails.getLabel())
                        .result(expressionLabelResultAndDetails.getResult())
                        .rollDetails(expressionLabelResultAndDetails.getDetails())
                        .build();
            }
        } catch (Throwable t) {
            return RollAnswer.builder()
                    .answerFormatType(answerFormatType)
                    .expression(input)
                    .errorMessage(t.getMessage())
                    .build();
        }
    }

    private ExpressionLabelResultAndDetails singleRoll(String input, String label) {
        if (isBooleanExpression(input)) {
            BooleanExpression booleanExpression = getBooleanExpression(input);
            RollWithDetails rollWithDetails = rollWithDiceParser(booleanExpression.getExpression());
            if (rollWithDetails.getResult() == null) { //there was an error
                return new ExpressionLabelResultAndDetails(input, label, rollWithDetails.getRoll(), rollWithDetails.getDetails());
            }
            String result = booleanExpression.getResult(rollWithDetails.getResult());
            String details = String.format("%s = %s", rollWithDetails.getDetails(), booleanExpression.getDetail(rollWithDetails.getResult()));
            return new ExpressionLabelResultAndDetails(booleanExpression.getExpression(), label, result, details);
        } else {
            RollWithDetails rollWithDetails = rollWithDiceParser(input);
            return new ExpressionLabelResultAndDetails(input, label, String.valueOf(rollWithDetails.getResult()), rollWithDetails.getDetails());
        }
    }

    @VisibleForTesting
    BooleanExpression getBooleanExpression(String input) {
        Matcher matcher = BOOLEAN_EXPRESSION_PATTERN.matcher(input);
        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("'%s' doesn't match the required patter '%s'", input, BOOLEAN_EXPRESSION_PATTERN));
        }
        String expression = matcher.group(1);
        String compares = matcher.group(2);
        String defaultAnswer = matcher.group(3);
        List<ValueCompereResult> valueCompereResults = Arrays.stream(compares.split("(?<=[^<>=])(?=<=|>=|<>|<|>|=)"))
                .map(this::parseValueCompereResult)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new BooleanExpression(expression, valueCompereResults, defaultAnswer);
    }

    private ValueCompereResult parseValueCompereResult(String in) {
        Matcher matcher = VALUE_COMPERE_PATTER.matcher(in);
        if (!matcher.find()) {
            return null;
        }
        String booleanOperatorExpression = matcher.group(1);
        BooleanOperator operator = Arrays.stream(BooleanOperator.values())
                .filter(o -> o.expression.equals(booleanOperatorExpression))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("'%s' is not a valid boolean operator ", booleanOperatorExpression)));
        int compereValue = Integer.parseInt(matcher.group(2));
        String result = matcher.group(3);
        return new ValueCompereResult(operator, compereValue, result);
    }

    private RollWithDetails rollWithDiceParser(String input) {
        input = removeLeadingPlus(input);
        ResultTree resultTree = dice.detailedRoll(input);
        String title = String.format("%s = %d", input, resultTree.getValue());
        String details = String.format("[%s]", getBaseResults(resultTree).stream().map(String::valueOf).collect(Collectors.joining(", ")));
        return new RollWithDetails(title, details, resultTree.getValue());
    }

    public boolean validExpression(String input) {
        try {
            input = removeLeadingPlus(input);
            if (isMultipleRoll(input)) {
                if (isMultipleIdenticalRolls(input)) {
                    singleRoll(getInnerDiceExpressionFromMultiRoll(input), null);
                } else if (isMultipleDifferentRolls(input)) {
                    splitMultipleDifferentExpressions(input).forEach(e -> singleRoll(e, null));
                }
            } else if (isBooleanExpression(input)) {
                dice.detailedRoll(getBooleanExpression(input).getExpression());
            } else {
                dice.detailedRoll(input);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Value
    private static class ExpressionLabelResultAndDetails {
        @NonNull
        String expression;
        @Nullable
        String label;
        @NonNull
        String result;
        @NonNull
        String details;
    }

    @Value
    private static class RollWithDetails {
        @NonNull
        String roll;
        @NonNull
        String details;
        Integer result;
    }

    @Value
    @VisibleForTesting
    static class BooleanExpression {
        String expression;
        List<ValueCompereResult> valueCompereResults;
        String defaultResult;

        public String getResult(int value) {
            return valueCompereResults.stream()
                    .filter(vcr -> vcr.matches(value))
                    .findFirst()
                    .map(ValueCompereResult::getResultValue)
                    .orElse(defaultResult);
        }

        public String getDetail(int value) {
            String resultDetail = valueCompereResults.stream()
                    .filter(vcr -> vcr.matches(value))
                    .findFirst()
                    .map(ValueCompereResult::toString)
                    .orElse(String.format(" ⟹ %s", defaultResult));
            return String.format("%d%s", value, resultDetail);
        }
    }

    @Value
    @VisibleForTesting
    static class ValueCompereResult {

        BooleanOperator operator;
        int compareValue;
        String resultValue;

        private boolean matches(int value) {
            return operator.function.apply(value, compareValue);
        }

        public String toString() {
            return String.format("%s%d ⟹ %s", operator.pretty, compareValue, resultValue);
        }
    }

}
