package de.janno.discord.api;

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
