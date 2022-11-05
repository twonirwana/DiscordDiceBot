package de.janno.discord.bot.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;

import static de.janno.discord.bot.command.AbstractCommand.ANSWER_TYPE_EMBED;

/**
 * A configuration for a dice system. It will be created with the slash command and not modified afterwards.
 */
@EqualsAndHashCode
@Getter
@ToString
public class Config implements Serializable {

    private final Long answerTargetChannelId;

    @NonNull
    private final String answerDisplayType;

    @JsonCreator
    public Config(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                  @JsonProperty("answerDisplayType") String answerDisplayType) {
        this.answerTargetChannelId = answerTargetChannelId;
        this.answerDisplayType = answerDisplayType == null ? ANSWER_TYPE_EMBED : answerDisplayType;
    }

    public String toShortString() {
        return String.format("[%s, %s]", getTargetChannelShortString(), answerDisplayType);
    }

    protected String getTargetChannelShortString() {
        return answerTargetChannelId == null ? "local" : "target";
    }

}
