package de.janno.discord.bot.command.poolTarget;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.IConfig;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;
import java.util.stream.Collectors;

@Value
public class PoolTargetConfig implements IConfig {
    int diceSides;
    int maxNumberOfButtons;
    @NonNull
    Set<Integer> rerollSet;
    @NonNull
    Set<Integer> botchSet;
    String rerollVariant;
    Long answerTargetChannelId;

    @Override
    public String toShortString() {
        return ImmutableList.of(
                String.valueOf(diceSides),
                String.valueOf(maxNumberOfButtons),
                rerollSet.stream().map(String::valueOf).collect(Collectors.joining(PoolTargetCommand.SUBSET_DELIMITER)),
                botchSet.stream().map(String::valueOf).collect(Collectors.joining(PoolTargetCommand.SUBSET_DELIMITER)),
                rerollVariant,
                targetChannelToString(answerTargetChannelId)
        ).toString();
    }
}
