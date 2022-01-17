package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import lombok.NonNull;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.List;
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

    public static Set<Integer> getSetFromCommandOptions(ApplicationCommandInteractionOption options, String optionId, String delimiter) {
        return options.getOption(optionId)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
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
}
