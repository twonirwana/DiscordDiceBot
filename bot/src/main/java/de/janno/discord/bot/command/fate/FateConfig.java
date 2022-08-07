package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
@Getter
public class FateConfig extends Config {
    @NonNull
    private final String type;

    public FateConfig(Long answerTargetChannelId, @NonNull String type) {
        super(answerTargetChannelId);
        this.type = type;
    }

    @Override
    public String toShortString() {
        return String.format("[%s, %s]", type, getTargetChannelShortString());
    }
}
