package de.janno.discord.connector.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class EmbedDefinition {
    String description;
    @Singular
    List<Field> fields;

    @Value
    public static class Field {
        @NonNull
        String name;
        @NonNull
        String value;
        boolean inline;
    }
}
