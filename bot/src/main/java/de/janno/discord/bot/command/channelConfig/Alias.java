package de.janno.discord.bot.command.channelConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

import java.util.Arrays;
import java.util.Optional;

import static de.janno.discord.bot.command.channelConfig.ChannelConfigCommand.REGEX_NAME_VALUE_DELIMITER;
import static de.janno.discord.bot.command.channelConfig.ChannelConfigCommand.REPLACE_NAME_VALUE_DELIMITER;

@Value
public class Alias {

    @NonNull
    String name;
    @NonNull
    String value;
    @NonNull
    Type type;

    @JsonCreator
    public Alias(@NonNull @JsonProperty("name") String name,
                 @NonNull @JsonProperty("value") String value,
                 @JsonProperty("type") Type type) {
        this.name = name;
        this.value = value;
        this.type = Optional.ofNullable(type).orElse(Type.Replace);
    }


    @Override
    public String toString() {
        return "%s%s%s".formatted(name, type == Type.Replace ? "->" : "=>", value).replace("\n", " ");
    }

    @JsonIgnore
    public String toCommandOptionsString() {
        return "%s%s%s".formatted(name, type == Type.Replace ? REPLACE_NAME_VALUE_DELIMITER : REGEX_NAME_VALUE_DELIMITER, value);
    }

    public enum Type {
        Replace,
        Regex;

        public static Type of(@NonNull String name) {
            return Arrays.stream(Type.values())
                    .filter(t -> t.name().equals(name))
                    .findFirst()
                    .orElse(null);
        }
    }
}
