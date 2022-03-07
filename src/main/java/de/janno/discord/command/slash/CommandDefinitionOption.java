package de.janno.discord.command.slash;


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
    Boolean required = false;
    @Singular
    List<CommandDefinitionOptionChoice> choices;
    @Singular
    List<CommandDefinitionOption> options;
    Double minValue;
    Double maxValue;

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
            switch (value) {
                case 1:
                    return SUB_COMMAND;
                case 2:
                    return SUB_COMMAND_GROUP;
                case 3:
                    return STRING;
                case 4:
                    return INTEGER;
                case 5:
                    return BOOLEAN;
                case 6:
                    return USER;
                case 7:
                    return CHANNEL;
                case 8:
                    return ROLE;
                case 9:
                    return MENTIONABLE;
                case 10:
                    return NUMBER;
                case 11:
                    return ATTACHMENT;
                default:
                    return UNKNOWN;
            }
        }

        public int getValue() {
            return value;
        }
    }

}
