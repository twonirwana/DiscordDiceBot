package de.janno.discord.bot.command.poolTarget;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
public class PoolTargetConfig extends Config {
    private  final int diceSides;
    private   final int maxNumberOfButtons;
    @NonNull
    private   final Set<Integer> rerollSet;
    @NonNull
    private   final Set<Integer> botchSet;
    private   final String rerollVariant;

    public PoolTargetConfig(Long answerTargetChannelId, int diceSides, int maxNumberOfButtons, @NonNull Set<Integer> rerollSet, @NonNull Set<Integer> botchSet, String rerollVariant) {
        super(answerTargetChannelId);
        this.diceSides = diceSides;
        this.maxNumberOfButtons = maxNumberOfButtons;
        this.rerollSet = rerollSet;
        this.botchSet = botchSet;
        this.rerollVariant = rerollVariant;
    }

    @Override
    public String toShortString() {
        return ImmutableList.of(
                String.valueOf(diceSides),
                String.valueOf(maxNumberOfButtons),
                rerollSet.stream().map(String::valueOf).collect(Collectors.joining(PoolTargetCommand.SUBSET_DELIMITER)),
                botchSet.stream().map(String::valueOf).collect(Collectors.joining(PoolTargetCommand.SUBSET_DELIMITER)),
                rerollVariant,
                getTargetChannelShortString()
        ).toString();
    }
}
