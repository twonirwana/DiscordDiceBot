package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.EmptyData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;


@Value
@EqualsAndHashCode(callSuper = true)
public class CustomParameterStateData extends EmptyData {

    @NonNull List<String> selectedParameterValues;
    @Nullable
    String lockedForUserName;


    @JsonCreator
    public CustomParameterStateData(
            @JsonProperty("selectedParameterValues") @NonNull List<String> selectedParameterValues,
            @JsonProperty("lockedForUserName") @Nullable String lockedForUserName) {
        this.selectedParameterValues = selectedParameterValues;
        this.lockedForUserName = lockedForUserName;
    }

    @JsonIgnore
    @Override
    public String getShortStringValues() {
        return String.format("%s, %s", selectedParameterValues,
                (Optional.ofNullable(lockedForUserName).orElse("")));
    }
}
