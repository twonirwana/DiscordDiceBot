package de.janno.discord.bot.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;


import java.util.Optional;

@Value
public class ButtonIdLabelAndDiceExpression {
    @NonNull
    String buttonId;
    @NonNull
    String label;
    @NonNull
    String diceExpression;
    boolean newLine;
    boolean directRoll;
    
    String emoji;

    @JsonCreator
    public ButtonIdLabelAndDiceExpression(
            @JsonProperty("buttonId") @NonNull String buttonId,
            @JsonProperty("label") @NonNull String label,
            @JsonProperty("diceExpression") @NonNull String diceExpression,
            @JsonProperty("newLine") Boolean newLine,
            @JsonProperty("directRoll") Boolean directRoll,
            @JsonProperty("emoji") String emoji) {
        this.buttonId = buttonId;
        this.label = label;
        this.diceExpression = diceExpression;
        this.newLine = Optional.ofNullable(newLine).orElse(false);
        this.directRoll = Optional.ofNullable(directRoll).orElse(false);
        this.emoji = emoji;
    }

    public String toShortString() {
        if (diceExpression.equals(label)) {
            return diceExpression.replace("\n", " ");
        }
        return String.format("%s%s@%s%s", newLine ? "⏎" : "", diceExpression, directRoll ? "!" : "", label).replace("\n", " ");
    }
}

