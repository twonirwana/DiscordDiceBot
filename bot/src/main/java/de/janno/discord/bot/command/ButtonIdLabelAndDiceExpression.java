package de.janno.discord.bot.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

@Value
public class ButtonIdLabelAndDiceExpression {
    @NonNull
    String buttonId;
    @NonNull
    String label;
    @NonNull
    String diceExpression;

    @JsonCreator
    public ButtonIdLabelAndDiceExpression(
            @JsonProperty("buttonId") @NonNull String buttonId,
            @JsonProperty("label") @NonNull String label,
            @JsonProperty("diceExpression") @NonNull String diceExpression) {
        this.buttonId = buttonId;
        this.label = label;
        this.diceExpression = diceExpression;
    }

    public String toShortString() {
        if (diceExpression.equals(label)) {
            return diceExpression;
        }
        return String.format("%s@%s", diceExpression, label);
    }
}

