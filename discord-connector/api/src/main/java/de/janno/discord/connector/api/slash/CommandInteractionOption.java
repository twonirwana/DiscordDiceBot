package de.janno.discord.connector.api.slash;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
@Builder
public class CommandInteractionOption {

    @NonNull
    String name;

    String stringValue;

    Long longValue;

    Boolean booleanValue;

    Long channelIdValue;

    @Singular
    List<CommandInteractionOption> options;

    public Optional<Long> getLongSubOptionWithName(@NonNull String name) {
        return options.stream()
                .filter(o -> name.equals(o.getName()))
                .findFirst()
                .map(CommandInteractionOption::getLongValue);
    }

    public Optional<String> getStringSubOptionWithName(@NonNull String name) {
        return options.stream()
                .filter(o -> name.equals(o.getName()))
                .findFirst()
                .map(CommandInteractionOption::getStringValue);
    }

    public Optional<Long> getChannelIdSubOptionWithName(@NonNull String name) {
        return options.stream()
                .filter(o -> name.equals(o.getName()))
                .findFirst()
                .map(CommandInteractionOption::getChannelIdValue);
    }
}
