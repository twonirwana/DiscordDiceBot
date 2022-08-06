package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CountSuccessesConfig extends Config {
    final int diceSides;
    final int target;
    @NonNull
    final String glitchOption;
    final int maxNumberOfButtons;

    public CountSuccessesConfig(Long answerTargetChannelId, int diceSides, int target, @NonNull String glitchOption, int maxNumberOfButtons) {
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
