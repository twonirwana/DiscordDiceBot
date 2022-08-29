package de.janno.discord.bot.dice;

import java.util.Objects;
import java.util.function.BiFunction;

public enum BooleanOperator {
    GREATER(">", ">", (a, b) -> a > b),
    GREATER_EQUAL(">=", "≥", (a, b) -> a >= b),
    LESSER("<", "<", (a, b) -> a < b),
    LESSER_EQUAL("<=", "≤", (a, b) -> a <= b),
    EQUAL("=", "=", Objects::equals),
    NOT_EQUAL("<>", "≠", (a, b) -> !Objects.equals(a, b));

    final String expression;
    final String pretty;
    final BiFunction<Integer, Integer, Boolean> function;
    BooleanOperator(String expression, String pretty, BiFunction<Integer, Integer, Boolean> function) {
        this.expression = expression;
        this.pretty = pretty;
        this.function = function;
    }
}
