package de.janno.discord.connector.api.slash;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandDefinitionOptionChoice {
    String name;
    String value;
}
