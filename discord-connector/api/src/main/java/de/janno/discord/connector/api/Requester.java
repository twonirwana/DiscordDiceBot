package de.janno.discord.connector.api;

import lombok.Value;

import java.util.Locale;

@Value
public class Requester {
    String userName;
    String channelName;
    String guildName;
    String shard;
    Locale userLocal;


    public String toLogString() {
        return String.format("'%s'.'%s'", guildName, channelName);
    }
}
