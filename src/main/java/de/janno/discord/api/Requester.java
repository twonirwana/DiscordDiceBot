package de.janno.discord.api;

import lombok.Value;

@Value
public class Requester {
    String userName;
    String channelName;
    String guildName;
}
