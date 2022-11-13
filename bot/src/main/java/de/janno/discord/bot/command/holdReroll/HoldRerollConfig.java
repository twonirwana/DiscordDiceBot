package de.janno.discord.bot.command.holdReroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.AnswerFormatType;
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
@ToString(callSuper = true)
public class HoldRerollConfig extends Config {

    private final int sidesOfDie;
    @NonNull
    private final Set<Integer> rerollSet;
    @NonNull
    private final Set<Integer> successSet;
    @NonNull
    private final Set<Integer> failureSet;

    @JsonCreator
    public HoldRerollConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("sidesOfDie") int sidesOfDie,
                            @JsonProperty("rerollSet") @NonNull Set<Integer> rerollSet,
                            @JsonProperty("successSet") @NonNull Set<Integer> successSet,
                            @JsonProperty("failureSet") @NonNull Set<Integer> failureSet,
                            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType) {
        super(answerTargetChannelId, answerFormatType);
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
                getTargetChannelShortString(),
                getAnswerFormatType()
        ).toString();
    }
}
