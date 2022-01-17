package de.janno.discord.dice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.command.Answer;
import dev.diceroll.parser.ResultTree;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class DiceParserHelper {

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
        //   return ImmutableList.of(resultTree.getExpression().description() + "=" + resultTree.getValue());
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
            ResultTree resultTree = dice.detailedRoll(input);
            String title = String.format("%s = %d", input, resultTree.getValue());
            String details = String.format("[%s]", getBaseResults(resultTree).stream().sorted().map(String::valueOf).collect(Collectors.joining(", ")));
            return new RollWithDetails(title, details);
        } catch (Throwable t) {
            log.error(String.format("DiceParser error in %s:", input), t);
            return new RollWithDetails("Error", String.format("Could not execute the dice expression: %s", input));
        }
    }

    public boolean validExpression(String input) {
        try {
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
