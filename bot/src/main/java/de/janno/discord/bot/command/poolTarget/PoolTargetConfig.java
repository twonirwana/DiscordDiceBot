package de.janno.discord.bot.command.poolTarget;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class PoolTargetConfig extends Config {
    private final int diceSides;
    private final int maxNumberOfButtons;
    @NonNull
    private final Set<Integer> rerollSet;
    @NonNull
    private final Set<Integer> botchSet;
    @NonNull
    private final String rerollVariant;

    @JsonCreator
    public PoolTargetConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("diceSides") int diceSides,
                            @JsonProperty("maxNumberOfButtons") int maxNumberOfButtons,
                            @JsonProperty("rerollSet") @NonNull Set<Integer> rerollSet,
                            @JsonProperty("botchSet") @NonNull Set<Integer> botchSet,
                            @JsonProperty("rerollVariant") String rerollVariant,
                            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                            @JsonProperty("resultImage") ResultImage resultImage,
                            @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor) {
        super(answerTargetChannelId, answerFormatType, resultImage, diceStyleAndColor, null);
        this.diceSides = diceSides;
        this.maxNumberOfButtons = maxNumberOfButtons;
        this.rerollSet = rerollSet;
        this.botchSet = botchSet;
        this.rerollVariant = Optional.ofNullable(rerollVariant).orElse(PoolTargetCommand.ALWAYS_REROLL);
    }

    @Override
    public String toShortString() {
        return ImmutableList.of(
                String.valueOf(diceSides),
                String.valueOf(maxNumberOfButtons),
                rerollSet.stream().map(String::valueOf).collect(Collectors.joining(PoolTargetCommand.SUBSET_DELIMITER)),
                botchSet.stream().map(String::valueOf).collect(Collectors.joining(PoolTargetCommand.SUBSET_DELIMITER)),
                rerollVariant,
                getTargetChannelShortString(),
                getAnswerFormatType(),
                getDiceStyleAndColor()
        ).toString();
    }

    @Override
    public String toCommandOptionsString() {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s: %s", PoolTargetCommand.SIDES_OF_DIE_OPTION, diceSides));
        out.add(String.format("%s: %s", PoolTargetCommand.MAX_DICE_OPTION, maxNumberOfButtons));
        out.add(String.format("%s: %s", PoolTargetCommand.REROLL_VARIANT_OPTION, rerollVariant));
        if (!rerollSet.isEmpty()) {
            out.add(String.format("%s: %s", PoolTargetCommand.REROLL_SET_OPTION, rerollSet.stream().sorted().map(Object::toString).collect(Collectors.joining(", "))));
        }
        if (!botchSet.isEmpty()) {
            out.add(String.format("%s: %s", PoolTargetCommand.BOTCH_SET_OPTION, botchSet.stream().sorted().map(Object::toString).collect(Collectors.joining(", "))));
        }
        return "%s %s".formatted(String.join(" ", out), super.toCommandOptionsString());
    }
}
