package de.janno.discord.dice;


import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceUtils {
    public static final String MINUS = "\u2212";
    private static final Random randomNumberGenerator = new Random();

    private final Function<Integer, Integer> numberSupplier;

    public DiceUtils() {
        numberSupplier = diceSides -> (int) (randomNumberGenerator.nextDouble() * diceSides + 1);
    }

    public DiceUtils(Deque<Integer> results) {
        numberSupplier = diceSides -> results.pop();
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


    public static String makeBold(int i) {
        return "**" + i + "**";
    }

    public static String makeUnderlineBold(int i) {
        return "__**" + i + "**__";
    }

    public static String makeUnderline(int i) {
        return "__" + i + "__";
    }

    public static String makeItalics(int i) {
        return "*" + i + "*";
    }

    public static String makeBoldItalics(int i) {
        return "***" + i + "***";
    }
}
