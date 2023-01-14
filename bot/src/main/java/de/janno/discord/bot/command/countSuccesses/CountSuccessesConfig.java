package de.janno.discord.bot.command.countSuccesses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class CountSuccessesConfig extends Config {
    private final int diceSides;
    private final int target;
    @NonNull
    private final String glitchOption;
    private final int maxNumberOfButtons;
    private final int minDiceCount;
    @NonNull
    private final ImmutableSet<Integer> rerollSet;
    @NonNull
    private final ImmutableSet<Integer> botchSet;

    @JsonCreator
    public CountSuccessesConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                                @JsonProperty("diceSides") int diceSides,
                                @JsonProperty("target") int target,
                                @JsonProperty("glitchOption") @NonNull String glitchOption,
                                @JsonProperty("maxNumberOfButtons") int maxNumberOfButtons,
                                @JsonProperty("minDiceCount") Integer minDiceCount,
                                @JsonProperty("rerollSet") Set<Integer> rerollSet,
                                @JsonProperty("botchSet") Set<Integer> botchSet,
                                @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                                @JsonProperty("resultImage") ResultImage resultImage) {
        super(answerTargetChannelId, answerFormatType, resultImage);
        this.diceSides = diceSides;
        this.target = target;
        this.glitchOption = glitchOption;
        this.maxNumberOfButtons = maxNumberOfButtons;
        this.minDiceCount = Objects.requireNonNullElse(minDiceCount, 1);
        this.rerollSet = ImmutableSet.copyOf(Objects.requireNonNullElse(rerollSet, Set.of()));
        this.botchSet = ImmutableSet.copyOf(Objects.requireNonNullElse(botchSet, Set.of()));
    }

    @Override
    public String toShortString() {
        return Stream.of(String.valueOf(getDiceSides()),
                String.valueOf(getTarget()),
                getGlitchOption(),
                String.valueOf(getMaxNumberOfButtons()),
                String.valueOf(getMinDiceCount()),
                rerollSet.stream().map(String::valueOf).collect(Collectors.joining(CountSuccessesCommand.SUBSET_DELIMITER)),
                botchSet.stream().map(String::valueOf).collect(Collectors.joining(CountSuccessesCommand.SUBSET_DELIMITER)),
                getTargetChannelShortString(),
                getAnswerFormatType(),
                getResultImage()
        ).toList().toString();
    }
}
