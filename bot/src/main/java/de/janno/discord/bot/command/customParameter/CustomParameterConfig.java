package de.janno.discord.bot.command.customParameter;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CustomParameterConfig extends Config {
    @NonNull
    private final String baseExpression;


    public CustomParameterConfig(Long answerTargetChannelId, @NonNull String baseExpression) {
        super(answerTargetChannelId);
        this.baseExpression = baseExpression;
    }

    public Collection<CustomIdIndexWithValue> getIdComponents() {
        return ImmutableList.of(
                new CustomIdIndexWithValue(CustomIdIndex.BASE_EXPRESSION, baseExpression),
                new CustomIdIndexWithValue(CustomIdIndex.ANSWER_TARGET_CHANNEL, Optional.ofNullable(getAnswerTargetChannelId()).map(Objects::toString).orElse(""))
        );
    }

    @Override
    public String toShortString() {
        return ImmutableList.of(baseExpression, getTargetChannelShortString()).toString();
    }
}
