package de.janno.discord.dice;


import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiceUtils {
    public static final String MINUS = "\u2212";
    private static final Random randomNumberGenerator = new Random();
    private static final Map<String, Map<Integer, AtomicLong>> resultStaticMap = new ConcurrentHashMap<>();

    public static int rollDice(int diceSides) {
        return (int) (randomNumberGenerator.nextDouble() * diceSides + 1);
    }

    private static void addStatistic(String typ, int result) {
        resultStaticMap.putIfAbsent(typ, new ConcurrentHashMap<>());
        resultStaticMap.get(typ).putIfAbsent(result, new AtomicLong(0L));
        resultStaticMap.get(typ).get(result).incrementAndGet();
    }

    public static String getResultStaticMap() {
        return resultStaticMap.toString();
    }

    public static List<Integer> rollFate() {
        return IntStream.range(0, 4)
                .mapToObj(i -> rollDice(3) - 2)
                .peek(i -> addStatistic("fate", i))
                .collect(Collectors.toList());
    }

    public static List<Integer> rollDiceOfType(int numberOfDice, int diceSides) {
        return IntStream.range(0, numberOfDice)
                .mapToObj(i -> rollDice(diceSides))
                .peek(i -> addStatistic("d" + diceSides, i))
                .collect(Collectors.toList());
    }

    public static int numberOfDiceResultsGreaterEqual(List<Integer> results, int target) {
        return (int) results.stream()
                .filter(i -> i >= target)
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

    public static String makeGreaterEqualTargetValuesBold(List<Integer> diceResults, int target) {
        return "[" + diceResults.stream()
                .map(i -> {
                    if (i >= target) {
                        return makeBold(i);
                    }
                    return i + "";
                }).collect(Collectors.joining(",")) + "]";
    }


    public static String makeBold(int i) {
        return "**" + i + "**";
    }

    public static String makeUnderlineBold(int i) {
        return "__**" + i + "**__";
    }

}
