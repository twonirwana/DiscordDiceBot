package de.janno.discord.connector.api.message;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ComponentRowDefinition {

    @Singular
    List<ButtonDefinition> buttonDefinitions;
}
