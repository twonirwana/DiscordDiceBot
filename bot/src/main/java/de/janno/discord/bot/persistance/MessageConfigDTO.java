package de.janno.discord.bot.persistance;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
@AllArgsConstructor
public class MessageConfigDTO {

    @NonNull
    UUID configUUID;
    Long guildId;
    long channelId;
    @NonNull
    String commandId;
    @NonNull
    String configClassId;
    @NonNull
    String config;
    String name;
    Long creationUserId;

}
