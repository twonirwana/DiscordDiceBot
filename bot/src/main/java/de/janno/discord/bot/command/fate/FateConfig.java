package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.command.IConfig;
import lombok.NonNull;
import lombok.Value;

@Value
public class FateConfig implements IConfig {
    @NonNull
    String type;
    Long answerTargetChannelId;

    @Override
    public String toShortString() {
        return String.format("[%s, %s]", type, targetChannelToString(answerTargetChannelId));
    }
}
