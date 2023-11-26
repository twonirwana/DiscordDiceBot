package de.janno.discord.bot.command.customDice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class CustomDiceConfig extends Config {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions;
    @NonNull
    private final DiceParserSystem diceParserSystem;

    @JsonCreator
    public CustomDiceConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("buttonIdLabelAndDiceExpressions") @NonNull List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions,
                            @JsonProperty("diceParserSystem") DiceParserSystem diceParserSystem,
                            @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                            @JsonProperty("resultImage") ResultImage resultImage,
                            @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                            @JsonProperty("configLocale") Locale configLocale) {
        super(answerTargetChannelId, answerFormatType, resultImage, diceStyleAndColor, configLocale);
        this.buttonIdLabelAndDiceExpressions = buttonIdLabelAndDiceExpressions;
        this.diceParserSystem = Optional.ofNullable(diceParserSystem).orElse(DiceParserSystem.DICEROLL_PARSER);
    }

    @Override
    public String toShortString() {
        String buttons = buttonIdLabelAndDiceExpressions.stream()
                .map(ButtonIdLabelAndDiceExpression::toShortString)
                .collect(Collectors.joining(", "));
        return "[%s, %s, %s, %s, %s]".formatted(buttons, getTargetChannelShortString(), diceParserSystem, getAnswerFormatType(), getDiceStyleAndColor());
    }


    @Override
    public String toCommandOptionsString() {
        String buttons = buttonIdLabelAndDiceExpressions.stream()
                .map(b -> {
                    if (b.getDiceExpression().equals(b.getLabel())) {
                        return b.getDiceExpression();
                    }
                    return "%s@%s".formatted(b.getDiceExpression(), b.getLabel());
                })
                .collect(Collectors.joining(";"));
        return "%s: %s %s".formatted(CustomDiceCommand.BUTTONS_OPTION_NAME, String.join(" ", buttons), super.toCommandOptionsString());
    }
}
