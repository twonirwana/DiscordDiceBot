package de.janno.discord.connector.api;

import lombok.Value;

@Value
public class Requester {
    String userName;
    String channelName;
    String guildName;
}
