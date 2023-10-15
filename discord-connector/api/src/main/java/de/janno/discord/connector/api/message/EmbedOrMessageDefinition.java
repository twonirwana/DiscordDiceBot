package de.janno.discord.connector.api.message;

import lombok.*;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class EmbedOrMessageDefinition {


    String title;
    String descriptionOrContent;
    @Singular
    @NonNull
    List<Field> fields;
    Supplier<? extends InputStream> image;
    @Singular
    @NonNull
    List<ComponentRowDefinition> componentRowDefinitions;
    @Builder.Default
    Type type = Type.EMBED;

    @Override
    public String toString() {
        return "EmbedOrMessageDefinition(" +
                "title=" + title +
                ", descriptionOrContent=" + descriptionOrContent +
                ", fields=" + fields +
                ", componentRowDefinitions=" + componentRowDefinitions +
                ", hasImage=" + (image != null) +
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
