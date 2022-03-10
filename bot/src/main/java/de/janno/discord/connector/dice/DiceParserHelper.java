package de.janno.discord.connector.dice;

import de.janno.discord.connector.api.Answer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import dev.diceroll.parser.ResultTree;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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


    private final IDice dice;

    public DiceParserHelper() {
        this(new DiceParser());
    }

    @VisibleForTesting
    public DiceParserHelper(IDice dice) {
        this.dice = dice;
    }

    static boolean isMultipleRoll(String input) {
        return input.matches("^\\d+x\\[.*]$");
    }

    static int getNumberOfMultipleRolls(String input) {
        int firstBracket = input.indexOf("x[");
        int numberOfRolls = Integer.parseInt(
                input.substring(0, firstBracket));
        return Math.min(numberOfRolls, 25); //limited to 25 because that is the max number of embed discord fields
    }

    static String getInnerDiceExpression(String input) {
        int firstBracket = input.indexOf("x[");
        return input.substring(firstBracket + 2, input.length() - 1);
    }

    private static List<Integer> getBaseResults(ResultTree resultTree) {
        if (!resultTree.getResults().isEmpty()) {
            return resultTree.getResults().stream()
                    .flatMap(rt -> getBaseResults(rt).stream())
                    .collect(Collectors.toList());
        }
        return ImmutableList.of(resultTree.getValue());
    }

    public String validateDiceExpression(String expression, String helpCommand) {
        if (expression.length() > 80) {
            return String.format("The following dice expression are to long: '%s'. A expression must be 80 or less characters long", expression);
        }
        if (!validExpression(expression)) {
            return String.format("The following dice expression are invalid: '%s'. Use %s to get more information on how to use the command.", expression, helpCommand);
        }
        return null;
    }

    public String validateDiceExpressionWitOptionalLabel(@NonNull String expressionWithOptionalLabel, String labelDelimiter, String helpCommand) {
        String label;
        String diceExpression;

        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            String[] split = expressionWithOptionalLabel.split(labelDelimiter);
            if (split.length != 2) {
                return String.format("The button definition '%s' should have the diceExpression@Label", expressionWithOptionalLabel);
            }
            label = split[1].trim();
            diceExpression = split[0].trim();
        } else {
            label = expressionWithOptionalLabel;
            diceExpression = expressionWithOptionalLabel;
        }
        if (label.length() > 80) {
            return String.format("Label for '%s' is to long, max number of characters is 80", expressionWithOptionalLabel);
        }
        if (label.isBlank()) {
            return String.format("Label for '%s' requires a visible name", expressionWithOptionalLabel);
        }
        if (diceExpression.isBlank()) {
            return String.format("Dice expression for '%s' is empty", expressionWithOptionalLabel);
        }
        return validateDiceExpression(diceExpression, helpCommand);
    }


    public String validateListOfExpressions(List<String> optionValues, String labelDelimiter, String configDelimiter, String helpCommand) {
        if (optionValues.isEmpty()) {
            return String.format("You must configure at least one dice expression. Use '%s' to get more information on how to use the command.", helpCommand);
        }
        for (String startOptionString : optionValues) {
            if (startOptionString.contains(configDelimiter)) {
                return String.format("The button definition '%s' is not allowed to contain ','", startOptionString);
            }
            String diceParserValidation = validateDiceExpressionWitOptionalLabel(startOptionString, labelDelimiter, helpCommand);
            if (diceParserValidation != null) {
                return diceParserValidation;
            }
        }

        Map<String, Long> expressionOccurrence = optionValues.stream()
                .map(s -> s.split(labelDelimiter)[0].toLowerCase().trim())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        for (Map.Entry<String, Long> e : expressionOccurrence.entrySet()) {
            if (e.getValue() > 1) {
                return String.format("The dice expression '%s' is not unique. Each dice expression must only once.", e.getKey());
            }
        }

        return null;
    }

    private String removeLeadingPlus(String diceExpression) {
        if (diceExpression.startsWith("+")) {
            return diceExpression.substring(1);
        }
        return diceExpression;
    }

    public Answer roll(String input, @Nullable String label) {
        try {
            if (isMultipleRoll(input)) {
                int numberOfRolls = getNumberOfMultipleRolls(input);
                String innerExpression = getInnerDiceExpression(input);
                List<Answer.Field> fields = IntStream.range(0, numberOfRolls)
                        .mapToObj(i -> rollWithDiceParser(innerExpression))
                        .map(r -> new Answer.Field(r.roll, r.getDetails(), false))
                        .collect(ImmutableList.toImmutableList());
                String title = Strings.isNullOrEmpty(label) ? "Multiple Results" : label;
                return new Answer(title, null, fields);
            } else {
                RollWithDetails rollWithDetails = rollWithDiceParser(input);
                String title = Strings.isNullOrEmpty(label) ? rollWithDetails.getRoll() : String.format("%s: %s", label, rollWithDetails.getRoll());
                return new Answer(title, rollWithDetails.getDetails(), ImmutableList.of());
            }
        } catch (Throwable t) {
            log.error(String.format("Error in %s:", input), t);
            return new Answer("Error", String.format("Could not execute the dice expression: %s", input), ImmutableList.of());
        }
    }

    private RollWithDetails rollWithDiceParser(String input) {
        try {
            input = removeLeadingPlus(input);
            ResultTree resultTree = dice.detailedRoll(input);
            String title = String.format("%s = %d", input, resultTree.getValue());
            String details = String.format("[%s]", getBaseResults(resultTree).stream().map(String::valueOf).collect(Collectors.joining(", ")));
            return new RollWithDetails(title, details);
        } catch (Throwable t) {
            log.error(String.format("DiceParser error in %s:", input), t);
            return new RollWithDetails("Error", String.format("Could not execute the dice expression: %s", input));
        }
    }

    public boolean validExpression(String input) {
        try {
            input = removeLeadingPlus(input);
            if (isMultipleRoll(input)) {
                dice.roll(getInnerDiceExpression(input));
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
    }

}
