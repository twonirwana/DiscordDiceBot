package de.janno.discord.bot.command.directRoll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class DirectRollConfig extends Config {
    private final boolean alwaysSumResult;

    @JsonCreator
    public DirectRollConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("alwaysSumResult") Boolean alwaysSumResult,
                            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                            @JsonProperty("resultImage") ResultImage resultImage) {
        super(answerTargetChannelId, answerFormatType, resultImage);
        this.alwaysSumResult = alwaysSumResult == null || alwaysSumResult;

    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s, %s]".formatted(getTargetChannelShortString(), alwaysSumResult, getAnswerFormatType(), getResultImage());
    }
}
