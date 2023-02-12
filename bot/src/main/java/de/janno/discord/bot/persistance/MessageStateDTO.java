package de.janno.discord.bot.persistance;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Value
@AllArgsConstructor
public class MessageStateDTO {

    @NonNull
    UUID configUUID;
    Long guildId;
    long channelId;
    long messageId;
    @NonNull
    String commandId;
    @NonNull
    String stateDataClassId;
    @Nullable
    String stateData;

}
