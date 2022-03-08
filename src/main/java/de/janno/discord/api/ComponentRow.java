package de.janno.discord.api;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ComponentRow {

    @Singular
    List<ButtonDefinition> buttonDefinitions;
}
