package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class CustomParameterConfig extends Config {
    @NonNull
    private final String baseExpression;


    public CustomParameterConfig(
            @JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
            @JsonProperty("baseExpression") @NonNull String baseExpression) {
        super(answerTargetChannelId);
        this.baseExpression = baseExpression;
    }

    @Override
    public String toShortString() {
        return ImmutableList.of(baseExpression, getTargetChannelShortString()).toString();
    }
}
