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
    List<ButtonDefinition> buttonDefinitions;

    public ComponentRowDefinition(@NonNull List<ButtonDefinition> buttonDefinitions) {
        this.buttonDefinitions = buttonDefinitions;
        Preconditions.checkArgument(buttonDefinitions.size() <= 5, "Too many components in %s, max is 5", buttonDefinitions);
    }
}
