package de.janno.discord.command.slash;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandDefinitionOptionChoice {
    String name;
    Object value;
}
