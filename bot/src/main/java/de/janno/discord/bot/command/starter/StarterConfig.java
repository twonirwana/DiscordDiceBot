package de.janno.discord.bot.command.starter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.Config;
import lombok.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    @JsonCreator
    public StarterConfig(
            @JsonProperty("id") @NonNull UUID id,
            @JsonProperty("commands") @NonNull List<Command> commands,
            @JsonProperty("configLocale") @NonNull Locale configLocale,
            @JsonProperty("message") @NonNull String  message) {
        this.id = id;
        this.commands = commands;
        this.configLocale = configLocale;
        this.message = message;
    }


    @Override
    public String toCommandOptionsString() {
        //todo
        return "";
    }

    public Object toShortString() {
        //todo
        return "";
    }

    @Value
    public static class Command {
        String name;
        String commandId;
        UUID configUUID;

        @JsonCreator
        public Command(@JsonProperty("name") @NonNull String name,
                       @JsonProperty("commandId") @NonNull String commandId,
                       @JsonProperty("configUUID") @NonNull UUID configUUID) {
            this.name = name;
            this.commandId = commandId;
            this.configUUID = configUUID;
        }
    }
}
