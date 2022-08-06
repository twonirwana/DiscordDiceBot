package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.command.IConfig;
import lombok.NonNull;
import lombok.Value;

import java.util.stream.Stream;

@Value
public class CountSuccessesConfig implements IConfig {
    int diceSides;
    int target;
    @NonNull
    String glitchOption;
    int maxNumberOfButtons;
    Long answerTargetChannelId;

    @Override
    public String toShortString() {
        return Stream.of(String.valueOf(getDiceSides()),
                String.valueOf(getTarget()),
                getGlitchOption(),
                String.valueOf(getMaxNumberOfButtons()),
                targetChannelToString(answerTargetChannelId)
        ).toList().toString();
    }
}
