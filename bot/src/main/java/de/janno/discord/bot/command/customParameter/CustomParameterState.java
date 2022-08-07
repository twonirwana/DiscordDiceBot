package de.janno.discord.bot.command.customParameter;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CustomParameterState extends State {
    static final String SELECTED_PARAMETER_DELIMITER = "\t";
    @NonNull
    private final List<String> selectedParameterValues;
    @Nullable
    private final String lockedForUserName;

    public CustomParameterState(String buttonValue,
                                @NonNull List<String> selectedParameterValues,
                                @Nullable String lockedForUserName) {
        super(buttonValue);
        this.selectedParameterValues = selectedParameterValues;
        this.lockedForUserName = lockedForUserName;
    }

    public Collection<CustomIdIndexWithValue> getIdComponents() {
        return ImmutableList.of(
                new CustomIdIndexWithValue(CustomIdIndex.SELECTED_PARAMETER, String.join(SELECTED_PARAMETER_DELIMITER, selectedParameterValues))
        );
    }

    @Override
    public String toShortString() {
        return ImmutableList.<String>builder()
                .addAll(selectedParameterValues)
                .add(Optional.ofNullable(lockedForUserName).orElse(""))
                .build().toString();
    }
}
