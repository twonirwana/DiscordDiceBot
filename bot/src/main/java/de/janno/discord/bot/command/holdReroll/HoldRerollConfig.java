package de.janno.discord.bot.command.holdReroll;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Set;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.holdReroll.HoldRerollCommand.SUBSET_DELIMITER;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class HoldRerollConfig extends Config {

    private final int sidesOfDie;
    @NonNull
    private final Set<Integer> rerollSet;
    @NonNull
    private final Set<Integer> successSet;
    @NonNull
    private final Set<Integer> failureSet;

    public HoldRerollConfig(Long answerTargetChannelId, int sidesOfDie, @NonNull Set<Integer> rerollSet, @NonNull Set<Integer> successSet, @NonNull Set<Integer> failureSet) {
        super(answerTargetChannelId);
        this.sidesOfDie = sidesOfDie;
        this.rerollSet = rerollSet;
        this.successSet = successSet;
        this.failureSet = failureSet;
    }

    @Override
    public String toShortString() {
        return ImmutableList.of(
                String.valueOf(sidesOfDie),
                rerollSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                successSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                failureSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                getTargetChannelShortString()
        ).toString();
    }
}
