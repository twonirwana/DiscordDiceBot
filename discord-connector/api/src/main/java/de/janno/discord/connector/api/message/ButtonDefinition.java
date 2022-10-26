package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
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

    boolean disabled;

    ButtonDefinition(@NonNull String label, @NonNull String id, Style style, boolean disabled) {
        Preconditions.checkArgument(label.length() <= 80, String.format("Label '%s' is to long", label));
        Preconditions.checkArgument(id.length() <= 100, String.format("ID '%s' is to long", id));
        this.label = label;
        this.id = id;
        this.style = Objects.requireNonNullElse(style, Style.PRIMARY);
        this.disabled = disabled;
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
