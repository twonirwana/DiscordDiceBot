package de.janno.discord.connector.api;

import lombok.Value;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

@Value
public class MessageState {
    long messageId;
    boolean pinned;
    boolean exists;
    boolean canBeDeleted;
    @Nullable
    OffsetDateTime creationTime;
}
