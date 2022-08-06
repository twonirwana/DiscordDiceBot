package de.janno.discord.bot.command.customParameter;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.IConfig;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import static de.janno.discord.bot.command.customParameter.CustomParameterCommand.PARAMETER_VARIABLE_PATTERN;
import static de.janno.discord.bot.command.customParameter.CustomParameterCommand.RANGE_REPLACE_REGEX;

@Value
public class CustomParameterConfig implements IConfig {
    @NonNull
    String baseExpression;
    Long answerTargetChannelId;
    @NonNull
    @EqualsAndHashCode.Exclude
    String firstParameterName;
    @NonNull
    @EqualsAndHashCode.Exclude
    String firstParameterExpression;

    public CustomParameterConfig(@NonNull String[] customIdComponents) {
        this(customIdComponents[CustomIdIndex.BASE_EXPRESSION.index], CustomParameterCommand.getOptionalLongFromArray(customIdComponents, 2));
    }

    public CustomParameterConfig(@NonNull String baseExpression, Long answerTargetChannelId) {
        this.baseExpression = baseExpression;
        this.answerTargetChannelId = answerTargetChannelId;
        this.firstParameterExpression = getNextParameterExpression(baseExpression);
        this.firstParameterName = cleanupExpressionForDisplay(firstParameterExpression);
    }

    private static String cleanupExpressionForDisplay(String expression) {
        return expression
                .replaceAll(RANGE_REPLACE_REGEX, "")
                .replace("{", "*{")
                .replace("}", "}*");
    }

    private static @NonNull String getNextParameterExpression(@NonNull String expression) {
        Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(0);
        }
        throw new IllegalStateException(String.format("Expression '%s' missing a parameter definition like {name}", expression));
    }

    public Collection<CustomIdIndexWithValue> getIdComponents() {
        return ImmutableList.of(
                new CustomIdIndexWithValue(CustomIdIndex.BASE_EXPRESSION, baseExpression),
                new CustomIdIndexWithValue(CustomIdIndex.ANSWER_TARGET_CHANNEL, Optional.ofNullable(answerTargetChannelId).map(Objects::toString).orElse(""))
        );
    }

    @Override
    public String toShortString() {
        return ImmutableList.of(baseExpression, targetChannelToString(answerTargetChannelId)).toString();
    }

    public String baseExpressionWithoutRange() {
        return cleanupExpressionForDisplay(baseExpression);
    }
}
