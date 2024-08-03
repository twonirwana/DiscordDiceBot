package de.janno.discord.bot.command.customDice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.RollConfig;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true) //ignore legacy diceSystem field
public class CustomDiceConfig extends RollConfig {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions;

    @JsonCreator
    public CustomDiceConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("buttonIdLabelAndDiceExpressions") @NonNull List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions,
                            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                            @JsonProperty("answerInteractionType") AnswerInteractionType answerInteractionType,
                            @JsonProperty("resultImage") ResultImage resultImage,
                            @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                            @JsonProperty("configLocale") Locale configLocale
    ) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale);
        this.buttonIdLabelAndDiceExpressions = buttonIdLabelAndDiceExpressions;
    }

    @Override
    public String toShortString() {
        String buttons = buttonIdLabelAndDiceExpressions.stream()
                .map(ButtonIdLabelAndDiceExpression::toShortString)
                .collect(Collectors.joining(", "));
        return "[%s, %s, %s, %s]".formatted(buttons, getTargetChannelShortString(), getAnswerFormatType(), getDiceStyleAndColor());
    }


    @Override
    public String toCommandOptionsString() {
        String buttons = buttonIdLabelAndDiceExpressions.stream()
                .map(b -> {
                    if (b.getDiceExpression().equals(b.getLabel())) {
                        return "%s%s".formatted(b.isNewLine() ? ";" : "", b.getDiceExpression());
                    }
                    return "%s%s@%s".formatted(b.isNewLine() ? ";" : "", b.getDiceExpression(), b.getLabel());
                })
                .collect(Collectors.joining(";")).replace("\n", "\\n");
        return "%s: %s %s".formatted(CustomDiceCommand.BUTTONS_OPTION_NAME, String.join(" ", buttons), super.toCommandOptionsString());
    }
}
