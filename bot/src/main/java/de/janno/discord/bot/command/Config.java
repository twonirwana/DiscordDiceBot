package de.janno.discord.bot.command;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * A configuration for a dice system. It will be created with the slash command and not modified afterwards.
 */
@EqualsAndHashCode
@Getter
@AllArgsConstructor
@ToString
public class Config implements Serializable {
    private final Long answerTargetChannelId;

    public String toShortString() {
        return String.format("[%s]", getTargetChannelShortString());
    }

    protected String getTargetChannelShortString() {
        return answerTargetChannelId == null ? "local" : "target";
    }

}
