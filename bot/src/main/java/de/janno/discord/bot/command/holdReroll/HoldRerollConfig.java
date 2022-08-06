package de.janno.discord.bot.command.holdReroll;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.IConfig;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.holdReroll.HoldRerollCommand.SUBSET_DELIMITER;

@Value
public class HoldRerollConfig implements IConfig {
    int sidesOfDie;
    @NonNull
    Set<Integer> rerollSet;
    @NonNull
    Set<Integer> successSet;
    @NonNull
    Set<Integer> failureSet;
    Long answerTargetChannelId;

    @Override
    public String toShortString() {
        return ImmutableList.of(
                String.valueOf(sidesOfDie),
                rerollSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                successSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                failureSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                targetChannelToString(answerTargetChannelId)
        ).toString();
    }
}
