package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.StateData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Value
@EqualsAndHashCode(callSuper = true)
public class CustomParameterStateData extends StateData {

    @NonNull List<SelectedParameter> selectedParameterValues;
    @Nullable
    String lockedForUserName;

    @JsonCreator
    public CustomParameterStateData(
            @JsonProperty("selectedParameterValues") @NonNull List<SelectedParameter> selectedParameterValues,
            @JsonProperty("lockedForUserName") @Nullable String lockedForUserName) {
        this.selectedParameterValues = selectedParameterValues;
        this.lockedForUserName = lockedForUserName;
    }

    @JsonIgnore
    @Override
    public String getShortStringValues() {
        return String.format("%s, %s", selectedParameterValues.stream()
                        .map(SelectedParameter::getShortString).collect(Collectors.joining(", ")),
                Optional.ofNullable(lockedForUserName).orElse(""));
    }

    @JsonIgnore
    public Optional<SelectedParameter> getNextUnselectedParameterExpression() {
        return selectedParameterValues.stream()
                .filter(sp -> !sp.isFinished())
                .findFirst();
    }
}
