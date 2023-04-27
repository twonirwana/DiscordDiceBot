package de.janno.discord.connector.api.slash;


import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
@Builder
public class CommandDefinitionOption {

    Type type;
    String name;
    String description;
    @Builder.Default
    boolean required = false;
    @Singular
    List<CommandDefinitionOptionChoice> choices;
    @Singular
    List<CommandDefinitionOption> options;
    Long minValue;
    Long maxValue;
    @Builder.Default
    boolean autoComplete = false;

    public enum Type {
        UNKNOWN(-1),
        SUB_COMMAND(1),
        SUB_COMMAND_GROUP(2),
        STRING(3),
        INTEGER(4),
        BOOLEAN(5),
        USER(6),
        CHANNEL(7),
        ROLE(8),
        MENTIONABLE(9),
        NUMBER(10),
        ATTACHMENT(11);

        private final int value;

        Type(final int value) {
            this.value = value;
        }

        public static Type of(final int value) {
            Arrays.stream(Type.values())
                    .filter(t -> t.value == value)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown type: %s}", value)));
            return switch (value) {
                case 1 -> SUB_COMMAND;
                case 2 -> SUB_COMMAND_GROUP;
                case 3 -> STRING;
                case 4 -> INTEGER;
                case 5 -> BOOLEAN;
                case 6 -> USER;
                case 7 -> CHANNEL;
                case 8 -> ROLE;
                case 9 -> MENTIONABLE;
                case 10 -> NUMBER;
                case 11 -> ATTACHMENT;
                default -> UNKNOWN;
            };
        }

        public int getValue() {
            return value;
        }
    }

}
