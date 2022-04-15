package de.janno.discord.bot.dice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedDefinition;
import dev.diceroll.parser.ResultTree;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class DiceParserHelper {

    public static final String HELP =
            """
                    ```
                          Name     |   Syntax  |  Example \s
                    ---------------------------------------
                    Single Die     |'d'        |'d6'      \s
                    ---------------------------------------
                    Multiple Dice  |'d'        |'3d20'    \s
                    ---------------------------------------
                    Keep Dice      |'dk'       |'3d6k2'   \s
                    ---------------------------------------
                    Keep Low Dice  |'dl'       |'3d6l2'   \s
                    ---------------------------------------
                    Multiply Die   |'dX'       |'d10X'    \s
                     --------------------------------------
                    Multiply Dice  |'dX'       |'2d10X'   \s
                    ---------------------------------------
                    Fudge Dice     |'dF'       |'dF'      \s
                    ---------------------------------------
                    Multiple Fudge |'dF'       |'3dF'     \s
                     Dice          |           |          \s
                     --------------------------------------
                    Weighted Fudge |'dF.'      |'dF.1'    \s
                     Die           |           |          \s
                     --------------------------------------
                    Weighted       |'dF.'      |'2dF.1'   \s
                     Fudge Dice    |           |          \s
                    ---------------------------------------
                    Exploding Dice |'d!'       |'4d6!'    \s
                    ---------------------------------------
                    Exploding Dice |'d!>'      |'3d6!>5'  \s
                     (Target)      |           |          \s
                    ---------------------------------------
                    Compounding    |'d!!'      |'3d6!!'   \s
                     Dice          |           |          \s
                    ---------------------------------------
                    Compounding    |'d!!>'     |'3d6!!>5' \s
                     Dice (Target) |           |          \s
                    ---------------------------------------
                    Target Pool    |'d[>,<,=]' |'3d6=6'   \s
                     Dice          |           |          \s
                    ---------------------------------------
                    Target Pool    |'()[>,<,=]'|'(4d8-2)>6'
                    Dice Expression|           |          \s
                    ---------------------------------------
                    Multiple Rolls |'x[]'      |`3x[3d6]` \s
                    ---------------------------------------
                    Label          |'x@l'      |`1d20@Att'\s
                    ---------------------------------------
                    Integer        |''         |'42'      \s
                    ---------------------------------------
                    Add            |' + '      |'2d6 + 2' \s
                    ---------------------------------------
                    Subtract       |' - '      |'2 - 1'   \s
                    ---------------------------------------
                    Multiply       |' * '      |'1d4*2d6' \s
                    ---------------------------------------
                    Divide         |' / '      |'4 / 2'   \s
                    ---------------------------------------
                    Negative       |'-'        |'-1d6'    \s
                    ---------------------------------------
                    Order          |'asc, desc'|'10d10asc'\s
                    ---------------------------------------
                    Min/Max        |'min, max' |'2d6min3d4'
                    ```
                     it is also possible to use **/r** to directly use a dice expression without buttons
                    see https://github.com/twonirwana/DiscordDiceBot/blob/main/README.md for more details""";


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
                String innerExpression = getInnerDiceExpression(input);
                List<EmbedDefinition.Field> fields = IntStream.range(0, numberOfRolls)
                        .mapToObj(i -> rollWithDiceParser(innerExpression))
                        .map(r -> new EmbedDefinition.Field(r.roll, r.details(), false))
                        .collect(ImmutableList.toImmutableList());
                String title = Strings.isNullOrEmpty(label) ? "Multiple Results" : label;
                return EmbedDefinition.builder()
                        .title(title)
                        .fields(fields).build();
            } else {
                RollWithDetails rollWithDetails = rollWithDiceParser(input);
                String title = Strings.isNullOrEmpty(label) ? rollWithDetails.roll() : String.format("%s: %s", label, rollWithDetails.roll());
                return EmbedDefinition.builder()
                        .title(title)
                        .description(rollWithDetails.details())
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

    private RollWithDetails rollWithDiceParser(String input) {
        try {
            input = removeLeadingPlus(input);
            ResultTree resultTree = dice.detailedRoll(input);
            String title = String.format("%s = %d", input, resultTree.getValue());
            String details = String.format("[%s]", getBaseResults(resultTree).stream().map(String::valueOf).collect(Collectors.joining(", ")));
            return new RollWithDetails(title, details);
        } catch (ArithmeticException t) {
            log.error(String.format("Executing '%s' resulting in: %s", input, t.getMessage()));
            return new RollWithDetails("Arithmetic Error", String.format("Executing '%s' resulting in: %s", input, t.getMessage()));
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

    private record RollWithDetails(@NonNull String roll, @NonNull String details) {
    }

}
