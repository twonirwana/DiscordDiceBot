package de.janno.discord.bot.command.countSuccesses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
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
                                @JsonProperty("resultImage") ResultImage resultImage,
                                @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor) {
        super(answerTargetChannelId, answerFormatType, resultImage, diceStyleAndColor, null);
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
                getDiceStyleAndColor()
        ).toList().toString();
    }

    @Override
    public String toCommandOptionsString() {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s: %s", CountSuccessesCommand.ACTION_SIDE_OPTION, diceSides));
        out.add(String.format("%s: %s", CountSuccessesCommand.ACTION_TARGET_OPTION, target));
        out.add(String.format("%s: %s", CountSuccessesCommand.ACTION_GLITCH_OPTION, glitchOption));
        out.add(String.format("%s: %s", CountSuccessesCommand.ACTION_MAX_DICE_OPTION, maxNumberOfButtons));
        out.add(String.format("%s: %s", CountSuccessesCommand.ACTION_MIN_DICE_COUNT_OPTION, minDiceCount));
        if (!rerollSet.isEmpty()) {
            out.add(String.format("%s: %s", CountSuccessesCommand.ACTION_REROLL_SET_OPTION, rerollSet.stream().sorted().map(Object::toString).collect(Collectors.joining(", "))));
        }
        if (!botchSet.isEmpty()) {
            out.add(String.format("%s: %s", CountSuccessesCommand.ACTION_BOTCH_SET_OPTION, botchSet.stream().sorted().map(Object::toString).collect(Collectors.joining(", "))));
        }
        return "%s %s".formatted(String.join(" ", out), super.toCommandOptionsString());
    }
}
