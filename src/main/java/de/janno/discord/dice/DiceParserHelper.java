package de.janno.discord.dice;

import com.google.common.collect.ImmutableList;
import dev.diceroll.parser.Dice;
import dev.diceroll.parser.ResultTree;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DiceParserHelper {

    public static DiceResult rollWithDiceParser(String input) {
        try {
            ResultTree resultTree = Dice.detailedRoll(input);
            String title = input + " = " + resultTree.getValue();
            String details = "[" + getBaseResults(resultTree).stream().sorted().map(String::valueOf).collect(Collectors.joining(", ")) + "]";
            return new DiceResult(title, details);
        } catch (Throwable t) {
            log.error(String.format("DiceParser error in %s:", input), t);
            return new DiceResult("Error", "Could not execute the dice expression: " + input);
        }
    }

    public static boolean validExpression(String input) {
        try {
            Dice.roll(input);
            return true;
        } catch (Throwable t) {
            return false;
        }
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

    public static String validateDiceExpressions(List<String> expressions, String helpCommand) {
        if (expressions.isEmpty()) {
            return String.format("You must configure at least one button with a dice expression. Use %s to get more information on how to use the command.", helpCommand);
        }

        List<String> invalidDiceExpressions = expressions.stream()
                .filter(s -> !DiceParserHelper.validExpression(s))
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

}
