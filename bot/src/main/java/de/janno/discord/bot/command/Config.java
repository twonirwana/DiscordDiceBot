package de.janno.discord.bot.command;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A configuration for a dice system. It will be created with the slash command and not modified afterwards.
 */
@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class Config {
    private final Long answerTargetChannelId;

    public String toShortString() {
        return String.format("[%s]", getTargetChannelShortString());
    }

    protected String getTargetChannelShortString() {
        return answerTargetChannelId == null ? "local" : "target";
    }

}
