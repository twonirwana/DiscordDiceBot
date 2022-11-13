package de.janno.discord.bot.dice;


import com.google.common.collect.ImmutableList;
import de.janno.evaluator.ExpressionException;
import de.janno.evaluator.dice.random.GivenNumberSupplier;
import de.janno.evaluator.dice.random.NumberSupplier;
import de.janno.evaluator.dice.random.RandomNumberSupplier;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceUtils {
    public static final String MINUS = "\u2212";
    private final NumberSupplier numberSupplier;

    public DiceUtils() {
        numberSupplier = new RandomNumberSupplier();
    }

    public DiceUtils(long seed) {
        numberSupplier = new RandomNumberSupplier(seed);
    }

    public DiceUtils(Integer... resultNumbers) {
        numberSupplier = new GivenNumberSupplier(resultNumbers);
    }

    public static int numberOfDiceResultsGreaterEqual(List<Integer> results, int target) {
        return (int) results.stream()
                .filter(i -> i >= target)
                .count();
    }

    public static int numberOfDiceResultsEqual(List<Integer> results, Set<Integer> targets) {
        return (int) results.stream()
                .filter(targets::contains)
                .count();
    }

    public static String convertFateNumberToString(List<Integer> results) {
        return "[" + results.stream().map(i -> {
            if (i < 0) {
                return MINUS;
            } else if (i > 0) {
                return "＋";
            } else {
                return "▢";
            }
        }).collect(Collectors.joining(",")) + "]";
    }

    public static int fateResult(List<Integer> results) {
        return results.stream().mapToInt(i -> i).sum();
    }

    public int rollDice(int diceSides) {
        try {
            return numberSupplier.get(0, diceSides);
        } catch (ExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> rollFate() {
        return IntStream.range(0, 4)
                .mapToObj(i -> rollDice(3) - 2)
                .collect(Collectors.toList());
    }

    public List<Integer> rollDiceOfType(int numberOfDice, int diceSides) {
        return IntStream.range(0, numberOfDice)
                .mapToObj(i -> rollDice(diceSides))
                .collect(Collectors.toList());
    }

    public List<Integer> explodingReroll(int sidesOfDie, List<Integer> results, Set<Integer> resultNumbersToReroll) {
        if (resultNumbersToReroll.isEmpty()) {
            return results;
        }
        ImmutableList.Builder<Integer> resultBuilder = ImmutableList.builder();
        resultBuilder.addAll(results);
        int numberOfDiceToReroll = numberOfDiceResultsEqual(results, resultNumbersToReroll);
        int counter = 0;
        while (numberOfDiceToReroll > 0 && counter < 10) {
            List<Integer> rerolls = rollDiceOfType(numberOfDiceToReroll, sidesOfDie);
            resultBuilder.addAll(rerolls);
            numberOfDiceToReroll = numberOfDiceResultsEqual(rerolls, resultNumbersToReroll);
            counter++;
        }
        return resultBuilder.build();
    }
}
