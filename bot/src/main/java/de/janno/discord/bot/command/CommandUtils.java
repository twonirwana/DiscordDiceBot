package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CommandUtils {

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

    public static String markIn(@NonNull List<Integer> diceResults, @NonNull Set<Integer> toMark) {
        return String.format("[%s]", diceResults.stream().map(i -> {
            if (toMark.contains(i)) {
                return makeBold(i);
            }
            return String.valueOf(i);
        }).collect(Collectors.joining(",")));
    }

    public static Set<Integer> toSet(@NonNull String value, @NonNull String delimiter, @NonNull String emptyValue) {
        if (value.trim().equals(emptyValue)) {
            return ImmutableSet.of();
        }
        return Arrays.stream(value.split(delimiter))
                .filter(NumberUtils::isParsable)
                .map(Integer::parseInt)
                .collect(ImmutableSet.toImmutableSet());
    }

    public static Set<Integer> getSetFromCommandOptions(@NonNull CommandInteractionOption options,
                                                        @NonNull String optionId,
                                                        @NonNull String delimiter) {
        return options.getStringSubOptionWithName(optionId)
                .map(s -> s.split(delimiter))
                .map(Arrays::asList)
                .orElse(ImmutableList.of())
                .stream()
                .map(String::trim)
                .filter(NumberUtils::isParsable)
                .map(Integer::parseInt)
                .filter(i -> i > 0)
                .collect(ImmutableSet.toImmutableSet());
    }

    public static Optional<String> validateIntegerSetFromCommandOptions(@NonNull CommandInteractionOption options,
                                                                        @NonNull String optionId,
                                                                        @NonNull String delimiter) {
        Set<String> stringValues = options.getStringSubOptionWithName(optionId)
                .map(s -> s.split(delimiter))
                .map(Arrays::asList)
                .orElse(ImmutableList.of())
                .stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<String> notNumericValues = stringValues.stream()
                .filter(s -> !NumberUtils.isParsable(s))
                .collect(Collectors.toSet());
        if (!notNumericValues.isEmpty()) {
            return Optional.of(String.format("The parameter need to have numbers, seperated by '%s'. The following parameter where not numbers: '%s'",
                    delimiter,
                    String.join("', '", notNumericValues)));
        }
        Set<Integer> notPositiveNumber = stringValues.stream()
                .filter(NumberUtils::isParsable)
                .map(Integer::parseInt)
                .filter(i -> i <= 0)
                .collect(ImmutableSet.toImmutableSet());
        if (!notPositiveNumber.isEmpty()) {
            return Optional.of(String.format("The parameter need to have numbers greater zero, seperated by '%s'. The following parameter where not greater zero: '%s'",
                    delimiter, notPositiveNumber.stream().map(String::valueOf).collect(Collectors.joining("', '")))
            );
        }
        return Optional.empty();
    }
}
