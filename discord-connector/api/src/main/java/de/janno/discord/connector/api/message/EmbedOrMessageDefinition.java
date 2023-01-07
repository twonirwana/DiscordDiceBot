package de.janno.discord.connector.api.message;

import lombok.*;

import java.io.File;
import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class EmbedOrMessageDefinition {
    private static final String MINUS = "\u2212";

    //todo ephemeral field

    String title;
    String descriptionOrContent;
    @Singular
    List<Field> fields;
    File file;

    @Builder.Default
    Type type = Type.EMBED;

    public enum Type {
        MESSAGE,
        EMBED
    }

    @Value
    public static class Field {
        @NonNull
        String name;
        @NonNull
        String value;
        boolean inline;

    }
}
