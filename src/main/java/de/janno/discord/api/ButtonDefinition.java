package de.janno.discord.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ButtonDefinition {
    @NonNull
    String label;
    @NonNull
    String id;
    @NonNull
    @Builder.Default
    ButtonDefinition.Style style = Style.PRIMARY;

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
