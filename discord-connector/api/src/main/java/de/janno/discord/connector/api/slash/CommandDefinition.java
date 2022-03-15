package de.janno.discord.connector.api.slash;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class CommandDefinition {

    String name;
    String description;
    @Singular
    List<CommandDefinitionOption> options;

}
