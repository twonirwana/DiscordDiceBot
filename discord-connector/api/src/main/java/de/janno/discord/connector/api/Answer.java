package de.janno.discord.connector.api;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.Value;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Value
public class Answer {
    private static final String MINUS = "\u2212";
    @NonNull
    String title;
    @Nullable
    String content;
    @NonNull
    List<Field> fields;

    public String toShortString() {
        List<String> fieldStringList = fields.stream()
                .map(Field::toShortString)
                .collect(Collectors.toList());

        return String.format("%s %s %s", title,
                        Strings.isNullOrEmpty(content) ? "" : content,
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
