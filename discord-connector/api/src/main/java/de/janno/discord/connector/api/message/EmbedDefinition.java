package de.janno.discord.connector.api.message;

import com.google.common.base.Strings;
import lombok.*;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class EmbedDefinition {
    private static final String MINUS = "\u2212";

    //todo ephemeral field

    String title;
    String description;
    @Singular
    List<Field> fields;

    public String toShortString() {
        List<String> fieldStringList = fields.stream()
                .map(Field::toShortString).toList();

        return String.format("%s %s %s", title,
                        Strings.isNullOrEmpty(description) ? "" : description,
                        fieldStringList.isEmpty() ? "" : fieldStringList.toString())
                .replace("▢", "0")
                .replace("＋", "+")
                .replace(MINUS, "-")
                .replace("*", "");
    }


    @Value
    public static class Field {
        @NonNull
        String name;
        @NonNull
        String value;
        boolean inline;

        public String toShortString() {
            return String.format("%s %s", name, value);
        }

    }
}
