package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.reroll.Config;
import de.janno.discord.bot.dice.DiceSystem;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class CustomParameterConfig extends Config {
    @NonNull
    private final String baseExpression;
    @NonNull
    private final DiceSystem diceSystem;

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
            @JsonProperty("configLocale") Locale configLocale) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale);
        this.baseExpression = baseExpression;
        //todo handle legacy config
        this.diceSystem = DiceSystem.DICE_EVALUATOR;
        this.parameters = CustomParameterCommand.createParameterListFromBaseExpression(baseExpression);
    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s, %s, %s]".formatted(baseExpression.replace("\n", " "), getTargetChannelShortString(), diceSystem, getAnswerFormatType(), getDiceStyleAndColor());
    }

    @Override
    public String toCommandOptionsString() {
        return "%s: %s %s".formatted(CustomParameterCommand.EXPRESSION_OPTION_NAME, baseExpression.replace("\n", "\\n"), super.toCommandOptionsString());
    }
}
