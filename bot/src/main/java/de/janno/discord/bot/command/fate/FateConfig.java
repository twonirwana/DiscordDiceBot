package de.janno.discord.bot.command.fate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class FateConfig extends Config {
    @NonNull
    private final String type;

    @JsonCreator
    public FateConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                      @JsonProperty("type") @NonNull String type,
                      @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                      @JsonProperty("resultImage") ResultImage resultImage) {
        super(answerTargetChannelId, answerFormatType, resultImage);
        this.type = type;
    }

    @Override
    public String toShortString() {
        return String.format("[%s, %s, %s]", type, getTargetChannelShortString(), getAnswerFormatType());
    }
}
