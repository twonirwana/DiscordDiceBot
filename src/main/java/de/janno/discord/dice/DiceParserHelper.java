package de.janno.discord.dice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import dev.diceroll.parser.ResultTree;
import lombok.extern.slf4j.Slf4j;

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

    public String validateDiceExpressions(List<String> expressions, String helpCommand) {
        if (expressions.isEmpty()) {
            return String.format("You must configure at least one button with a dice expression. Use %s to get more information on how to use the command.", helpCommand);
        }

        List<String> invalidDiceExpressions = expressions.stream()
                .filter(s -> !validExpression(s))
                .collect(Collectors.toList());
        if (!invalidDiceExpressions.isEmpty()) {
            return String.format("The following dice expression are invalid: %s. Use %s to get more information on how to use the command.", String.join(",", invalidDiceExpressions), helpCommand);
        }

        List<String> toLongExpression = expressions.stream()
                .filter(s -> s.length() > 80)
                .collect(Collectors.toList());
        if (!toLongExpression.isEmpty()) {
            return String.format("The following dice expression are to long: %s. A expression must be 80 or less characters long", String.join(",", invalidDiceExpressions));
        }
        return null;
    }

    public List<DiceResult> roll(String input) {
        try {
            if (isMultipleRoll(input)) {
                return rollMultipleWithDiceParser(input);
            } else {
                return ImmutableList.of(rollWithDiceParser(input));
            }
        } catch (Throwable t) {
            log.error(String.format("Error in %s:", input), t);
            return ImmutableList.of(new DiceResult("Error", "Could not execute the dice expression: " + input));
        }
    }

    private List<DiceResult> rollMultipleWithDiceParser(String input) {
        int numberOfRolls = getNumberOfMultipleRolls(input);
        String innerExpression = getInnerDiceExpression(input);
        return IntStream.range(0, numberOfRolls)
                .mapToObj(i -> rollWithDiceParser(innerExpression))
                .collect(ImmutableList.toImmutableList());
    }

    private DiceResult rollWithDiceParser(String input) {
        try {
            ResultTree resultTree = dice.detailedRoll(input);
            String title = String.format("%s = %d", input, resultTree.getValue());
            String details = String.format("[%s]", getBaseResults(resultTree).stream().sorted().map(String::valueOf).collect(Collectors.joining(", ")));
            return new DiceResult(title, details);
        } catch (Throwable t) {
            log.error(String.format("DiceParser error in %s:", input), t);
            return new DiceResult("Error", "Could not execute the dice expression: " + input);
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

}
