package de.janno.discord.command.slash;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
@Builder
public class CommandInteractionOption {
    String name;
    String stringRepresentationValue;

    String stringValue;

    Long longValue;

    Boolean booleanValue;

    List<CommandInteractionOption> options;

    public Optional<Long> getLongSubOptionWithName(@NonNull String name) {
        return options.stream()
                .filter(o -> name.equals(o.getName()))
                .findFirst()
                .map(CommandInteractionOption::getLongValue);
    }

    public Optional<String> getStingSubOptionWithName(@NonNull String name) {
        return options.stream()
                .filter(o -> name.equals(o.getName()))
                .findFirst()
                .map(CommandInteractionOption::getStringValue);
    }
}
