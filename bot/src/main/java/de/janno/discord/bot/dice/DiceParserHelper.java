package de.janno.discord.bot.dice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedDefinition;
import dev.diceroll.parser.ResultTree;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class DiceParserHelper {

    public static final String HELP =
            """
                    ```
                          Name     |   Syntax  |  Example
                    ---------------------------------------
                    Single Die     |'d'        |'d6'
                    ---------------------------------------
                    Multiple Dice  |'d'        |'3d20'
                    ---------------------------------------
                    Keep Dice      |'dk'       |'3d6k2'
                    ---------------------------------------
                    Keep Low Dice  |'dl'       |'3d6l2'
                    ---------------------------------------
                    Multiply Die   |'dX'       |'d10X'
                     --------------------------------------
                    Multiply Dice  |'dX'       |'2d10X'
                    ---------------------------------------
                    Fudge Dice     |'dF'       |'dF'
                    ---------------------------------------
                    Multiple Fudge |'dF'       |'3dF'
                     Dice          |           |
                     --------------------------------------
                    Weighted Fudge |'dF.'      |'dF.1'
                     Die           |           |
                     --------------------------------------
                    Weighted       |'dF.'      |'2dF.1'
                     Fudge Dice    |           |
                    ---------------------------------------
                    Exploding Dice |'d!'       |'4d6!'
                    ---------------------------------------
                    Exploding Dice |'d!>'      |'3d6!>5'
                     (Target)      |           |
                    ---------------------------------------
                    Exploding Add  |'d^'       |'3d6^'
                     Dice          |           |
                    ---------------------------------------
                    Compounding    |'d!!'      |'3d6!!'
                     Dice          |           |
                    ---------------------------------------
                    Compounding    |'d!!>'     |'3d6!!>5'
                     Dice (Target) |           |
                    ---------------------------------------
                    Target Pool    |'d[>,<,=]' |'3d6=6'
                     Dice          |           |
                    ---------------------------------------
                    Target Pool    |'()[>,<,=]'|'(4d8-2)>6'
                    Dice Expression|           |
                    ---------------------------------------
                    Multiple Rolls |'x[]'      |`3x[3d6]`
                     (identical)   |           |
                    ---------------------------------------
                    Multiple Rolls |'x1&x2'    |`1d6&2d10`
                     (different)   |           |
                    ---------------------------------------
                    Result Label   |'x>y?a:b'  |`1d2=2?A:B`
                    ---------------------------------------
                    Request Label  |'x@l'      |`1d20@Att'
                    ---------------------------------------
                    Integer        |''         |'42'
                    ---------------------------------------
                    Add            |' + '      |'2d6 + 2'
                    ---------------------------------------
                    Subtract       |' - '      |'2 - 1'
                    ---------------------------------------
                    Multiply       |' * '      |'1d4*2d6'
                    ---------------------------------------
                    Divide         |' / '      |'4 / 2'
                    ---------------------------------------
                    Negative       |'-'        |'-1d6'
                    ---------------------------------------
                    Order          |'asc, desc'|'10d10asc'
                    ---------------------------------------
                    Min/Max        |'min, max' |'2d6min3d4'
                    ```
                     it is also possible to use **/r** to directly use a dice expression without buttons
                    see https://github.com/twonirwana/DiscordDiceBot/blob/main/README.md for more details""";

    private static final Pattern BOOLEAN_EXPRESSION_PATTERN = Pattern.compile("(^.+?)((?:<=|>=|<>|<|>|=)\\d+\\?.+)+:(.+)$");
    private static final Pattern MULTI_ROLL_EXPRESSION_PATTERN = Pattern.compile("^(\\d+?)x\\[(.*)?]$");
    private static final Pattern VALUE_COMPERE_PATTER = Pattern.compile("(<=|>=|<>|<|>|=)(\\d+)\\?(.+)");
    private static final String MULTI_DIFF_EXPRESSION_DELIMITER = "&";
    private final IDice dice;

    public DiceParserHelper() {
        this(new DiceParser());
    }

    @VisibleForTesting
    public DiceParserHelper(IDice dice) {
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

    public Optional<String> validateDiceExpression(String expression, String helpCommand, int maxCharacters) {
        if (expression.length() > maxCharacters) {
            return Optional.of(String.format("The following dice expression is to long: '%s'. The expression must be %d or less characters long", expression, maxCharacters));
        }
        if (!validExpression(expression)) {
            return Optional.of(String.format("The following dice expression is invalid: '%s'. Use %s to get more information on how to use the command.", expression, helpCommand));
        }
        return Optional.empty();
    }

    public Optional<String> validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String labelDelimiter, String helpCommand, int maxCharacters) {
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
        return validateDiceExpression(diceExpression, helpCommand, maxCharacters);
    }

    public Optional<String> validateListOfExpressions(List<String> optionValues, String labelDelimiter, String configDelimiter, String helpCommand, int maxCharacters) {
        if (optionValues.isEmpty()) {
            return Optional.of(String.format("You must configure at least one dice expression. Use '%s' to get more information on how to use the command.", helpCommand));
        }
        for (String startOptionString : optionValues) {
            if (startOptionString.contains(configDelimiter)) {
                return Optional.of(String.format("The button definition '%s' is not allowed to contain ','", startOptionString));
            }
            Optional<String> diceParserValidation = validateDiceExpressionWitOptionalLabel(startOptionString, labelDelimiter, helpCommand, maxCharacters);
            if (diceParserValidation.isPresent()) {
                return diceParserValidation;
            }
        }

        Map<String, Long> expressionOccurrence = optionValues.stream()
                .map(s -> s.split(labelDelimiter)[0].toLowerCase().trim())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        for (Map.Entry<String, Long> e : expressionOccurrence.entrySet()) {
            if (e.getValue() > 1) {
                return Optional.of(String.format("The dice expression '%s' is not unique. Each dice expression must only once.", e.getKey()));
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

    public EmbedDefinition roll(String input, @Nullable String label) {
        try {
            if (isMultipleRoll(input)) {
                List<LabelResult> labelResults;
                if (isMultipleIdenticalRolls(input)) {
                    int numberOfRolls = getNumberOfMultipleRolls(input);
                    String innerExpression = getInnerDiceExpressionFromMultiRoll(input);
                    labelResults = IntStream.range(0, numberOfRolls)
                            .mapToObj(i -> singleRoll(innerExpression, null))
                            .collect(Collectors.toList());
                } else if (isMultipleDifferentRolls(input)) {
                    labelResults = splitMultipleDifferentExpressions(input).stream()
                            .map(s -> singleRoll(s, null))
                            .collect(Collectors.toList());
                } else {
                    throw new IllegalStateException(String.format("Can't find correct multi roll version for: %s", input));
                }

                List<EmbedDefinition.Field> fields = labelResults.stream()
                        .limit(25) //max number of embedFields
                        .map(r -> new EmbedDefinition.Field(r.getLabel(), r.getResult(), false))
                        .collect(ImmutableList.toImmutableList());
                String title = Strings.isNullOrEmpty(label) ? "Multiple Results" : label;
                return EmbedDefinition.builder()
                        .title(title)
                        .fields(fields).build();
            } else {
                LabelResult labelResult = singleRoll(input, label);
                return EmbedDefinition.builder()
                        .title(labelResult.getLabel())
                        .description(labelResult.getResult()).build();
            }
        } catch (Throwable t) {
            log.error(String.format("Error in %s:", input), t);
            return EmbedDefinition.builder()
                    .title("Error")
                    .description(String.format("Could not execute the dice expression: %s", input))
                    .build();
        }
    }

    private LabelResult singleRoll(String input, String label) {
        try {
            if (isBooleanExpression(input)) {
                BooleanExpression booleanExpression = getBooleanExpression(input);
                RollWithDetails rollWithDetails = rollWithDiceParser(booleanExpression.getExpression());
                if (rollWithDetails.getResult() == null) { //there was an error
                    return new LabelResult(rollWithDetails.getRoll(), rollWithDetails.getDetails());
                }

                String result = booleanExpression.getResult(rollWithDetails.getResult());
                String labelOrExpression = Strings.isNullOrEmpty(label) ? booleanExpression.getExpression() : label;
                String title = String.format("%s: %s", labelOrExpression, result);
                String details = String.format("%s = %s", rollWithDetails.getDetails(), booleanExpression.getDetail(rollWithDetails.getResult()));
                return new LabelResult(title, details);
            } else {
                RollWithDetails rollWithDetails = rollWithDiceParser(input);
                String title = Strings.isNullOrEmpty(label) ? rollWithDetails.getRoll() : String.format("%s: %s", label, rollWithDetails.getRoll());
                return new LabelResult(title, rollWithDetails.getDetails());
            }
        } catch (Throwable t) {
            log.error(String.format("Error in %s:", input), t);
            return new LabelResult("Error", String.format("Could not execute the dice expression: %s", input));
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
        try {
            input = removeLeadingPlus(input);
            ResultTree resultTree = dice.detailedRoll(input);
            String title = String.format("%s = %d", input, resultTree.getValue());
            String details = String.format("[%s]", getBaseResults(resultTree).stream().map(String::valueOf).collect(Collectors.joining(", ")));
            return new RollWithDetails(title, details, resultTree.getValue());
        } catch (ArithmeticException t) {
            log.error(String.format("Executing '%s' resulting in: %s", input, t.getMessage()));
            return new RollWithDetails("Arithmetic Error", String.format("Executing '%s' resulting in: %s", input, t.getMessage()), null);
        } catch (Throwable t) {
            log.error(String.format("DiceParser error in %s:", input), t);
            return new RollWithDetails("Error", String.format("Could not execute the dice expression: %s", input), null);
        }
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
                dice.roll(getBooleanExpression(input).getExpression());
            } else {
                dice.roll(input);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Value
    private static class LabelResult {
        @NonNull
        String label;
        @NonNull
        String result;
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
