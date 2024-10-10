package de.janno.discord.connector.api;

import lombok.Value;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Value
public class Requester {
    String userName;
    @Nullable
    String channelName;
    @Nullable
    String guildName;
    String shard;
    @Nullable
    Locale userLocal;
    @Nullable
    UUID configUUID;

    public String toLogString() {
        String name = Optional.ofNullable(guildName)
                .or(() -> Optional.ofNullable(channelName))
                .or(() -> Optional.ofNullable(userName))
                .orElse("");
        if (configUUID != null) {
            return "%s:%s".formatted(configUUID, name);
        }
        return name;
    }
}
