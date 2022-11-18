package de.janno.discord.connector.api;

import lombok.Value;

@Value
public class Requester {
    String userName;
    String channelName;
    String guildName;
    String shard;

    public String toLogString() {
        return String.format("'%s'.'%s'", guildName, channelName);
    }
}
