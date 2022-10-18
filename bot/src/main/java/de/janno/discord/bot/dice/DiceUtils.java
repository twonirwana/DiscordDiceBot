package de.janno.discord.bot.dice;


import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceUtils {
    public static final String MINUS = "\u2212";
    private static final RandomGenerator randomNumberGenerator = new Sfc64Random();
    private final Function<Integer, Integer> numberSupplier;

    public DiceUtils() {
        numberSupplier = diceSides -> (int) (randomNumberGenerator.nextInt(diceSides) + 1);
    }

    public DiceUtils(Integer... resultNumbers) {
        Deque<Integer> results = new ArrayDeque<>(ImmutableList.copyOf(resultNumbers));
        numberSupplier = diceSides -> results.pop();
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
        return numberSupplier.apply(diceSides);
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
        if(resultNumbersToReroll.isEmpty()){
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
