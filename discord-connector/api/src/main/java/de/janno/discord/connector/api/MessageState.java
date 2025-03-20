package de.janno.discord.connector.api;

import lombok.Value;

import java.time.OffsetDateTime;

@Value
public class MessageState {
    long messageId;
    boolean pinned;
    boolean exists;
    boolean canBeDeleted;
    /**
     * null in legacy states
     */
    OffsetDateTime creationTime;
}
