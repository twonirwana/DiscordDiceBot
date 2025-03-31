package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ComponentRowDefinition {
    @Singular
    List<ComponentDefinition> componentDefinitions;

    public ComponentRowDefinition(@NonNull List<ComponentDefinition> componentDefinitions) {
        this.componentDefinitions = componentDefinitions;
        Preconditions.checkArgument(componentDefinitions.size() <= 5, "Too many components in %s, max is 5", componentDefinitions);
    }
}
