package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandLocaleDescription;
import de.janno.discord.connector.api.slash.CommandLocaleName;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
@Builder
public class ComponentRowDefinition {

    @Singular
    List<ButtonDefinition> buttonDefinitions;

    public ComponentRowDefinition(@NonNull List<ButtonDefinition> buttonDefinitions) {
        this.buttonDefinitions = buttonDefinitions;
        Preconditions.checkArgument(buttonDefinitions.size() <= 5, "Too many components in %s, max is 5", buttonDefinitions);
    }
}
