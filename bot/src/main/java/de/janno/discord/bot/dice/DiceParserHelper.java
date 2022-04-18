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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class DiceParserHelper {

    public static final String HELP =
            "```\n" +
                    "      Name     |   Syntax  |  Example  \n" +
                    "---------------------------------------\n" +
                    "Single Die     |'d'        |'d6'       \n" +
                    "---------------------------------------\n" +
                    "Multiple Dice  |'d'        |'3d20'     \n" +
                    "---------------------------------------\n" +
                    "Keep Dice      |'dk'       |'3d6k2'    \n" +
                    "---------------------------------------\n" +
                    "Keep Low Dice  |'dl'       |'3d6l2'    \n" +
                    "---------------------------------------\n" +
                    "Multiply Die   |'dX'       |'d10X'     \n" +
                    " --------------------------------------\n" +
                    "Multiply Dice  |'dX'       |'2d10X'    \n" +
                    "---------------------------------------\n" +
                    "Fudge Dice     |'dF'       |'dF'       \n" +
                    "---------------------------------------\n" +
                    "Multiple Fudge |'dF'       |'3dF'      \n" +
                    " Dice          |           |           \n" +
                    " --------------------------------------\n" +
                    "Weighted Fudge |'dF.'      |'dF.1'     \n" +
                    " Die           |           |           \n" +
                    " --------------------------------------\n" +
                    "Weighted       |'dF.'      |'2dF.1'    \n" +
                    " Fudge Dice    |           |           \n" +
                    "---------------------------------------\n" +
                    "Exploding Dice |'d!'       |'4d6!'     \n" +
                    "---------------------------------------\n" +
                    "Exploding Dice |'d!>'      |'3d6!>5'   \n" +
                    " (Target)      |           |           \n" +
                    "---------------------------------------\n" +
                    "Compounding    |'d!!'      |'3d6!!'    \n" +
                    " Dice          |           |           \n" +
                    "---------------------------------------\n" +
                    "Compounding    |'d!!>'     |'3d6!!>5'  \n" +
                    " Dice (Target) |           |           \n" +
                    "---------------------------------------\n" +
                    "Target Pool    |'d[>,<,=]' |'3d6=6'    \n" +
                    " Dice          |           |           \n" +
                    "---------------------------------------\n" +
                    "Target Pool    |'()[>,<,=]'|'(4d8-2)>6'\n" +
                    "Dice Expression|           |           \n" +
                    "---------------------------------------\n" +
                    "Multiple Rolls |'x[]'      |`3x[3d6]`  \n" +
                    "---------------------------------------\n" +
                    "Label Result   |'x>y?a:b'  |`1d2=2?A:B`\n" +
                    "---------------------------------------\n" +
                    "Label          |'x@l'      |`1d20@Att' \n" +
                    "---------------------------------------\n" +
                    "Integer        |''         |'42'       \n" +
                    "---------------------------------------\n" +
                    "Add            |' + '      |'2d6 + 2'  \n" +
                    "---------------------------------------\n" +
                    "Subtract       |' - '      |'2 - 1'    \n" +
                    "---------------------------------------\n" +
                    "Multiply       |' * '      |'1d4*2d6'  \n" +
                    "---------------------------------------\n" +
                    "Divide         |' / '      |'4 / 2'    \n" +
                    "---------------------------------------\n" +
                    "Negative       |'-'        |'-1d6'     \n" +
                    "---------------------------------------\n" +
                    "Order          |'asc, desc'|'10d10asc' \n" +
                    "---------------------------------------\n" +
                    "Min/Max        |'min, max' |'2d6min3d4'\n" +
                    "```" +
                    "\n it is also possible to use **/r** to directly use a dice expression without buttons" +
                    "\nsee https://github.com/twonirwana/DiscordDiceBot/blob/main/README.md for more details";

    private static final Pattern BOOLEAN_EXPRESSION_PATTERN = Pattern.compile("(^.+?)(=|<|>|<=|>=|<>?)(\\d+?)\\?(.+?):(.+$?)");
    private static final Pattern MULTI_ROLL_EXPRESSION_PATTERN = Pattern.compile("^(\\d+?)x\\[(.*)?]$");

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
        return MULTI_ROLL_EXPRESSION_PATTERN.matcher(input).matches();
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

    public Optional<String> validateDiceExpression(String expression, String helpCommand) {
        if (expression.length() > 80) {
            return Optional.of(String.format("The following dice expression are to long: '%s'. A expression must be 80 or less characters long", expression));
        }
        if (!validExpression(expression)) {
            return Optional.of(String.format("The following dice expression are invalid: '%s'. Use %s to get more information on how to use the command.", expression, helpCommand));
        }
        return Optional.empty();
    }

    public Optional<String> validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String labelDelimiter, String helpCommand) {
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
        return validateDiceExpression(diceExpression, helpCommand);
    }


    public Optional<String> validateListOfExpressions(List<String> optionValues, String labelDelimiter, String configDelimiter, String helpCommand) {
        if (optionValues.isEmpty()) {
            return Optional.of(String.format("You must configure at least one dice expression. Use '%s' to get more information on how to use the command.", helpCommand));
        }
        for (String startOptionString : optionValues) {
            if (startOptionString.contains(configDelimiter)) {
                return Optional.of(String.format("The button definition '%s' is not allowed to contain ','", startOptionString));
            }
            Optional<String> diceParserValidation = validateDiceExpressionWitOptionalLabel(startOptionString, labelDelimiter, helpCommand);
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

    public EmbedDefinition roll(String input, @Nullable String label) {
        try {
            if (isMultipleRoll(input)) {
                int numberOfRolls = getNumberOfMultipleRolls(input);
                String innerExpression = getInnerDiceExpressionFromMultiRoll(input);
                List<EmbedDefinition.Field> fields = IntStream.range(0, numberOfRolls)
                        .mapToObj(i -> rollWithDiceParser(innerExpression))
                        .map(r -> new EmbedDefinition.Field(r.getRoll(), r.getDetails(), false))
                        .collect(ImmutableList.toImmutableList());
                String title = Strings.isNullOrEmpty(label) ? "Multiple Results" : label;
                return EmbedDefinition.builder()
                        .title(title)
                        .fields(fields).build();
            } else if (isBooleanExpression(input)) {
                BooleanExpression booleanExpression = getBooleanExpression(input);
                RollWithDetails rollWithDetails = rollWithDiceParser(booleanExpression.getExpression());
                if (rollWithDetails.getResult() == null) {
                    return EmbedDefinition.builder()
                            .title(rollWithDetails.getRoll())
                            .description(rollWithDetails.getDetails())
                            .build();
                }

                String result = booleanExpression.getResult(rollWithDetails.getResult());
                String labelOrExpression = Strings.isNullOrEmpty(label) ? booleanExpression.getExpression() : label;
                String title = String.format("%s: %s", labelOrExpression, result);
                String details = String.format("%s%s=>%s%s%s", !Strings.isNullOrEmpty(label) ? booleanExpression.getExpression() + "=>" : "",
                        rollWithDetails.getDetails(), rollWithDetails.getResult(),
                        booleanExpression.getOperator(), booleanExpression.getCompareValue());
                return EmbedDefinition.builder()
                        .title(title)
                        .description(details)
                        .build();
            } else {
                RollWithDetails rollWithDetails = rollWithDiceParser(input);
                String title = Strings.isNullOrEmpty(label) ? rollWithDetails.getRoll() : String.format("%s: %s", label, rollWithDetails.getRoll());
                return EmbedDefinition.builder()
                        .title(title)
                        .description(rollWithDetails.getDetails())
                        .build();
            }
        } catch (Throwable t) {
            log.error(String.format("Error in %s:", input), t);
            return EmbedDefinition.builder()
                    .title("Error")
                    .description(String.format("Could not execute the dice expression: %s", input))
                    .build();
        }
    }

    @VisibleForTesting
    BooleanExpression getBooleanExpression(String input) {
        Matcher matcher = BOOLEAN_EXPRESSION_PATTERN.matcher(input);
        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("'%s' doesn't match the required patter '%s'", input, BOOLEAN_EXPRESSION_PATTERN));
        }
        return new BooleanExpression(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3)), matcher.group(4), matcher.group(5));
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
                dice.roll(getInnerDiceExpressionFromMultiRoll(input));
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
        String operator;
        int compareValue;
        String trueResult;
        String falseResult;

        private boolean matches(int value) {
            if ("=".equals(operator)) {
                return value == compareValue;
            } else if ("<".equals(operator)) {
                return value < compareValue;
            } else if (">".equals(operator)) {
                return value > compareValue;
            } else if ("<=".equals(operator)) {
                return value <= compareValue;
            } else if (">=".equals(operator)) {
                return value >= compareValue;
            } else if ("<>".equals(operator)) {
                return value != compareValue;
            }
            throw new IllegalArgumentException(String.format("Compare operator '%s' is not valid", operator));
        }

        public String getResult(int value) {
            return matches(value) ? trueResult : falseResult;
        }
    }

}
