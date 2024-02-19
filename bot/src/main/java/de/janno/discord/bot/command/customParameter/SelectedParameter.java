package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

@Value
public class SelectedParameter {
    @NonNull
    //parameter with {} and range
    String parameterExpression;
    @NonNull
    String name;
    @Nullable
    String selectedValue;
    @Nullable
    String labelOfSelectedValue;

    @JsonCreator
    public SelectedParameter(@JsonProperty("parameterExpression") @NonNull String parameterExpression,
                             @JsonProperty("name") @NonNull String name,
                             @JsonProperty("selectedValue") @Nullable String selectedValue,
                             @JsonProperty("labelOfSelectedValue") @Nullable String labelOfSelectedValue) {
        this.parameterExpression = parameterExpression;
        this.name = name;
        this.selectedValue = selectedValue;
        this.labelOfSelectedValue = labelOfSelectedValue;
    }


    @JsonIgnore
    public String getShortString() {
        return String.format("%s=%s", parameterExpression,
                (Optional.ofNullable(labelOfSelectedValue).orElse(""))).replace("\n", " ");
    }

    @JsonIgnore
    public SelectedParameter copy() {
        return new SelectedParameter(parameterExpression, name, selectedValue, labelOfSelectedValue);
    }
}
