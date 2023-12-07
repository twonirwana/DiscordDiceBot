package de.janno.discord.bot.command.sumCustomSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

@Value
public class ExpressionAndLabel {
    @NonNull String expression;
    @NonNull String label;

    @JsonCreator
    public ExpressionAndLabel(@JsonProperty("expression") @NonNull String expression, @JsonProperty("label") @NonNull String label) {
        this.expression = expression;
        this.label = label;
    }

    @JsonIgnore
    public String toString() {
        return expression.equals(label) ? expression : "%s@%s".formatted(expression, label);
    }
}
