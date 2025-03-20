package de.janno.discord.connector.api;

import lombok.Value;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Value
public class Requester {
    String userName;
    String channelName;
    String guildName;
    String shard;
    Locale userLocal;
    UUID configUUID;

    public String toLogString() {
        String name = Optional.ofNullable(guildName)
                .or(() -> Optional.ofNullable(channelName))
                .or(() -> Optional.ofNullable(userName))
                .orElse("");
        if (configUUID != null) {
            return "%s: %s".formatted(configUUID, name);
        }
        return name;
    }
}
