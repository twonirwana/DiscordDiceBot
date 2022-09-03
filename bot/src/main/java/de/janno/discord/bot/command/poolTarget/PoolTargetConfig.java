package de.janno.discord.bot.command.poolTarget;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class PoolTargetConfig extends Config {
    private final int diceSides;
    private final int maxNumberOfButtons;
    @NonNull
    private final Set<Integer> rerollSet;
    @NonNull
    private final Set<Integer> botchSet;
    private final String rerollVariant;

    @JsonCreator
    public PoolTargetConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("diceSides") int diceSides,
                            @JsonProperty("maxNumberOfButtons") int maxNumberOfButtons,
                            @JsonProperty("rerollSet") @NonNull Set<Integer> rerollSet,
                            @JsonProperty("botchSet") @NonNull Set<Integer> botchSet,
                            @JsonProperty("rerollVariant") String rerollVariant) {
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
