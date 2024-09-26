package de.janno.discord.bot.persistance;

import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class ChannelCommandConfigUUID {
    @NonNull
    String command;
    @NonNull
    LocalDateTime creationDate;
    @NonNull
    UUID configUUID;

}
