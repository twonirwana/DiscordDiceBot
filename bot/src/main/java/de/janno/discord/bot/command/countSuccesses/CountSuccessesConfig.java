package de.janno.discord.bot.command.countSuccesses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class CountSuccessesConfig extends Config {
    private final int diceSides;
    private final int target;
    @NonNull
    private final String glitchOption;
    private final int maxNumberOfButtons;

    @JsonCreator
    public CountSuccessesConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                                @JsonProperty("diceSides") int diceSides,
                                @JsonProperty("target") int target,
                                @JsonProperty("glitchOption") @NonNull String glitchOption,
                                @JsonProperty("maxNumberOfButtons") int maxNumberOfButtons) {
        super(answerTargetChannelId);
        this.diceSides = diceSides;
        this.target = target;
        this.glitchOption = glitchOption;
        this.maxNumberOfButtons = maxNumberOfButtons;
    }

    @Override
    public String toShortString() {
        return Stream.of(String.valueOf(getDiceSides()),
                String.valueOf(getTarget()),
                getGlitchOption(),
                String.valueOf(getMaxNumberOfButtons()),
                getTargetChannelShortString()
        ).toList().toString();
    }
}
