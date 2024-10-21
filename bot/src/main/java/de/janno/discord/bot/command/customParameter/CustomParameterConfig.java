package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollConfig;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true) //ignore legacy diceSystem field
public class CustomParameterConfig extends RollConfig {
    @NonNull
    private final String baseExpression;

    @NonNull
    @JsonIgnore
    private final List<Parameter> parameters;

    public CustomParameterConfig(
            @JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
            @JsonProperty("baseExpression") @NonNull String baseExpression,
            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
            @JsonProperty("answerInteractionType") AnswerInteractionType answerInteractionType,
            @JsonProperty("resultImage") ResultImage resultImage,
            @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
            @JsonProperty("configLocale") Locale configLocale,
            @JsonProperty("callStarterConfigAfterFinish") UUID callStarterConfigAfterFinish,
            @JsonProperty("name") String name) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale, callStarterConfigAfterFinish, name);
        this.baseExpression = baseExpression;
        this.parameters = CustomParameterCommand.createParameterListFromBaseExpression(baseExpression);
    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s, %s]".formatted(baseExpression.replace("\n", " "), getTargetChannelShortString(), getAnswerFormatType(), getDiceStyleAndColor());
    }

    @Override
    public String toCommandOptionsString() {
        return "%s: %s %s".formatted(CustomParameterCommand.EXPRESSION_OPTION_NAME, baseExpression.replace("\n", "\\n"), super.toCommandOptionsString());
    }
}
