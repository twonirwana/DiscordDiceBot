package de.janno.discord.connector.api.message;

import lombok.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

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

    @Override
    public String toString() {
        return "EmbedOrMessageDefinition(" +
                "title=" + title +
                ", descriptionOrContent=" + descriptionOrContent +
                ", fields=" + fields +
                ", file=" + Optional.ofNullable(file).map(File::getName).orElse("null") +
                ", type=" + type +
                ')';
    }

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
