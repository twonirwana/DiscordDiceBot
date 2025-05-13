package de.janno.discord.connector.api;

import lombok.Value;

@Value
public class ChildrenChannelCreationEvent {
    long childrenChannelId;
    long parentChannelId;
}
