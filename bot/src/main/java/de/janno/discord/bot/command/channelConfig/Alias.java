package de.janno.discord.bot.command.channelConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

@Value
public class Alias {

    @NonNull String name;
    @NonNull String value;

    @JsonCreator
    public Alias(@NonNull @JsonProperty("name") String name, @NonNull @JsonProperty("value") String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "%s->%s".formatted(name, value).replace("\n", " ");
    }
}
