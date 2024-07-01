package de.janno.discord.connector.api;

import lombok.Value;

import java.util.Locale;
import java.util.Optional;

@Value
public class Requester {
    String userName;
    String channelName;
    String guildName;
    String shard;
    Locale userLocal;


    public String toLogString() {
        return Optional.ofNullable(guildName)
                .or(() -> Optional.ofNullable(channelName))
                .or(() -> Optional.ofNullable(userName))
                .orElse("");
    }
}
