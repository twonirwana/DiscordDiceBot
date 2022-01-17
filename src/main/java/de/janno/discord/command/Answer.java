package de.janno.discord.command;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static de.janno.discord.dice.DiceUtils.MINUS;

@Value
public class Answer {
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

        return String.format("%s-%s %s", title,
                Strings.isNullOrEmpty(content) ? "" : content,
                fieldStringList.isEmpty() ? "" : fieldStringList.toString());
    }

    @Value
    public static class Field {
        @NonNull
        String name;
        @NonNull
        String value;
        boolean inline;

        public String toShortString() {
            return String.format("%s %s", name, value)
                    .replace("▢", "0")
                    .replace("＋", "+")
                    .replace(MINUS, "-")
                    .replace("*", "");
        }
    }
}
