package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Objects;

@Value
@Builder
public class ButtonDefinition {
    @NonNull
    String label;
    @NonNull
    String id;

    ButtonDefinition.Style style;

    ButtonDefinition(@NonNull String label, @NonNull String id, Style style) {
        Preconditions.checkArgument(label.length() <= 80, "Label '{}' is to long", label);
        Preconditions.checkArgument(id.length() <= 100, "ID '{}' is to long", id);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "ID must no be empty or null", id);
        this.label = label;
        this.id = id;
        this.style = Objects.requireNonNullElse(style, Style.PRIMARY);
    }

    public enum Style {
        PRIMARY(1),
        SECONDARY(2),
        SUCCESS(3),
        DANGER(4),
        LINK(5);

        private final int data;

        Style(int i) {
            this.data = i;
        }

        public int getValue() {
            return this.data;
        }
    }
}
