package de.janno.discord.bot.command.customParameter;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.StateData;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Value
public class CustomParameterStateData implements StateData {

    static final String SELECTED_PARAMETER_DELIMITER = "\t";
    @NonNull List<String> selectedParameterValues;
    @Nullable
    String lockedForUserName;


    public Collection<CustomIdIndexWithValue> getIdComponents() {
        return ImmutableList.of(
                new CustomIdIndexWithValue(CustomIdIndex.SELECTED_PARAMETER, String.join(SELECTED_PARAMETER_DELIMITER, selectedParameterValues))
        );
    }

    @Override
    public String getShortStringValues() {
        return String.format("%s, %s", selectedParameterValues,
                (Optional.ofNullable(lockedForUserName).orElse("")));
    }
}
