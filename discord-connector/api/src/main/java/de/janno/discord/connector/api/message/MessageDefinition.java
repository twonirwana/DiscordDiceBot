package de.janno.discord.connector.api.message;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
//Todo replace with EmbedOrMessageDefinition
public class MessageDefinition {
    @NonNull
    String content;
    @Singular
    List<ComponentRowDefinition> componentRowDefinitions;
}
