package de.janno.discord.bot.command.sumCustomSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.reroll.Config;
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
@JsonIgnoreProperties(ignoreUnknown = true) //ignore legacy diceSystem field
public class SumCustomSetConfig extends Config {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> labelAndExpression;
    private final boolean alwaysSumResult;
    private final boolean hideExpressionInStatusAndAnswer;
    private final boolean systemButtonNewLine;
    private final String prefix;
    private final String postfix;

    @JsonCreator
    public SumCustomSetConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                              @JsonProperty("labelAndExpression") @NonNull List<ButtonIdLabelAndDiceExpression> labelAndExpression,
                              @JsonProperty("alwaysSumResult") Boolean alwaysSumResult,
                              @JsonProperty("hideExpressionInStatusAndAnswer") Boolean hideExpressionInStatusAndAnswer,
                              @JsonProperty("systemButtonNewLine") Boolean systemButtonNewLine,
                              @JsonProperty("prefix") String prefix,
                              @JsonProperty("postfix") String postfix,
                              @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                              @JsonProperty("answerInteractionType") AnswerInteractionType answerInteractionType,
                              @JsonProperty("resultImage") ResultImage resultImage,
                              @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                              @JsonProperty("configLocale") Locale configLocale
    ) {
        super(answerTargetChannelId, answerFormatType, answerInteractionType, resultImage, diceStyleAndColor, configLocale);
        this.labelAndExpression = labelAndExpression;
        this.alwaysSumResult = alwaysSumResult == null || alwaysSumResult;
        this.hideExpressionInStatusAndAnswer = Optional.ofNullable(hideExpressionInStatusAndAnswer).orElse(false);
        this.systemButtonNewLine = Optional.ofNullable(systemButtonNewLine).orElse(false);
        this.postfix = postfix;
        this.prefix = prefix;
    }

    @Override
    public String toShortString() {
        String buttons = labelAndExpression.stream()
                .map(ButtonIdLabelAndDiceExpression::toShortString)
                .collect(Collectors.joining(", "));
        String statusAndAnswerType = hideExpressionInStatusAndAnswer ? "labelAnswer" : "expressionAnswer";
        String systemButtonNewLineString = systemButtonNewLine ? "newSystemButtonLine" : "sameSystemButtonLine";
        return "[%s, %s, %s, %s, %s, %s, %s, %s, %s]".formatted(buttons,
                getTargetChannelShortString(),
                alwaysSumResult,
                getAnswerFormatType(),
                getDiceStyleAndColor(),
                statusAndAnswerType,
                systemButtonNewLineString,
                Optional.ofNullable(prefix).orElse("-"),
                Optional.ofNullable(postfix).orElse("-"));
    }

    @Override
    public String toCommandOptionsString() {
        String buttons = labelAndExpression.stream()
                .map(b -> {
                    if (b.getDiceExpression().equals(b.getLabel())) {
                        return "%s%s".formatted(b.isNewLine() ? ";" : "", b.getDiceExpression());
                    }
                    return "%s%s@%s%s".formatted(b.isNewLine() ? ";" : "", b.getDiceExpression(), Optional.ofNullable(b.getEmoji()).orElse(""), b.getLabel());
                })
                .collect(Collectors.joining(";"));
        if (systemButtonNewLine) {
            buttons += ";;";
        }
        return "%s: %s %s: %s %s: %s %s%s%s".formatted(SumCustomSetCommand.BUTTONS_COMMAND_OPTIONS_NAME, String.join(" ", buttons).replace("\n", "\\n"),
                SumCustomSetCommand.ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME, alwaysSumResult,
                SumCustomSetCommand.HIDE_EXPRESSION_IN_ANSWER_OPTIONS_NAME, hideExpressionInStatusAndAnswer,
                Optional.ofNullable(prefix).map(p -> "%s: %s ".formatted(SumCustomSetCommand.PREFIX_OPTIONS_NAME, p)).orElse(""),
                Optional.ofNullable(postfix).map(p -> "%s: %s ".formatted(SumCustomSetCommand.POSTFIX_OPTIONS_NAME, p)).orElse(""),
                super.toCommandOptionsString());
    }

    @JsonIgnore
    @Override
    public boolean alwaysSumResultUp() {
        return alwaysSumResult;
    }
}
