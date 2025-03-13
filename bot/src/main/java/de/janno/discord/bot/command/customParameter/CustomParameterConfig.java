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

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true) //ignore legacy diceSystem field
public class CustomParameterConfig extends RollConfig {
    @NonNull
    private final String baseExpression;
    @NonNull
    private final InputType inputType;

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
            @JsonProperty("name") String name,
            @JsonProperty("inputType") InputType inputType) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale, callStarterConfigAfterFinish, name);
        this.baseExpression = baseExpression;
        this.inputType = Optional.ofNullable(inputType).orElse(InputType.button_legacy);
        this.parameters = CustomParameterCommand.createParameterListFromBaseExpression(baseExpression, inputType);
    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s, %s, %s]".formatted(baseExpression.replace("\n", " "), getTargetChannelShortString(), getAnswerFormatType(), getDiceStyleAndColor(), inputType.name());
    }

    @Override
    public String toCommandOptionsString() {
        return "%s: %s %s: %s %s".formatted(CustomParameterCommand.EXPRESSION_OPTION_NAME, baseExpression.replace("\n", "\\n"), CustomParameterCommand.INPUT_TYPE_NAME, inputType.name(), super.toCommandOptionsString());
    }

    public enum InputType {
        button,
        button_legacy, //for handling old parameter option button ids
        dropdown;

        public static InputType fromString(String inputType) {
            return Arrays.stream(InputType.values())
                    .filter(i -> i.name().equals(inputType))
                    .findFirst().orElse(null);
        }
    }
}
