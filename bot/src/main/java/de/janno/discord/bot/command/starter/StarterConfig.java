package de.janno.discord.bot.command.starter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.Config;
import lombok.*;


import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Getter
@ToString(callSuper = true)
public class StarterConfig implements Config {
    @NonNull
    private final List<Command> commands;
    @NonNull
    private final Locale configLocale;
    @NonNull
    private final UUID id;
    @NonNull
    private final String message;
    private final boolean startInNewMessage;
    
    String name;

    @JsonCreator
    public StarterConfig(
            @JsonProperty("id") @NonNull UUID id,
            @JsonProperty("commands") @NonNull List<Command> commands,
            @JsonProperty("configLocale") @NonNull Locale configLocale,
            @JsonProperty("message") @NonNull String message,
            @JsonProperty("name") String name,
            @JsonProperty("startInNewMessage") boolean startInNewMessage
    ) {
        this.id = id;
        this.commands = commands;
        this.configLocale = configLocale;
        this.message = message;
        this.name = name;
        this.startInNewMessage = startInNewMessage;
    }


    @Override
    public String toCommandOptionsString() {
        AtomicInteger counter = new AtomicInteger(1);
        String commandNames = commands.stream().map(Command::getName)
                .map(n -> "%s_%d: %s".formatted(StarterCommand.COMMAND_NAME_OPTION, counter.getAndIncrement(), n))
                .collect(Collectors.joining(" "));
        return "create %s %s: %s %s: %s %s: %s".formatted(commandNames, StarterCommand.COMMAND_MESSAGE_OPTION, message, StarterCommand.COMMAND_NAME_OPTION, name, StarterCommand.COMMAND_OPEN_IN_NEW_MESSAGE_OPTION, startInNewMessage);
    }

    public Object toShortString() {
        String commandsString = this.commands.stream()
                .map(Command::toShortString)
                .collect(Collectors.joining(", "));
        return "[%s, %s, %s, %s, %s]".formatted(id, commandsString, message, name, startInNewMessage);
    }

    @Value
    public static class Command {
        String name;
        UUID configUUID;

        @JsonCreator
        public Command(@JsonProperty("name") @NonNull String name,
                       @JsonProperty("configUUID") @NonNull UUID configUUID) {
            this.name = name;
            this.configUUID = configUUID;
        }

        public String toShortString() {
            return "%s:%s".formatted(name, configUUID);
        }
    }
}
