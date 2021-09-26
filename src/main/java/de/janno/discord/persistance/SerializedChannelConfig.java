package de.janno.discord.persistance;

import lombok.Value;

@Value
public class SerializedChannelConfig {
    String type;
    String channelId;
    String config;
}
