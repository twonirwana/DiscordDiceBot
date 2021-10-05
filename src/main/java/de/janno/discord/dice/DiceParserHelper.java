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
            String details = getBaseResults(resultTree).toString();
            return new DiceResult(title, details);
        } catch (Throwable t) {
            log.error(String.format("DiceParser error in %s:", input), t);
            return new DiceResult("Error", "Could not execute the dice expression: " + input);
        }
    }

    public static boolean validExpression(String input) {
        try {
            Dice.detailedRoll(input);
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    private static List<String> getBaseResults(ResultTree resultTree) {
        if (!resultTree.getResults().isEmpty()) {
            return resultTree.getResults().stream()
                    .flatMap(rt -> getBaseResults(rt).stream())
                    .collect(Collectors.toList());
        }
        //   return ImmutableList.of(resultTree.getExpression().description() + "=" + resultTree.getValue());
        return ImmutableList.of(String.valueOf(resultTree.getValue()));
    }

}
