package de.janno.discord.bot.persistance;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Value
@AllArgsConstructor
public class MessageDataDTO {

    @NonNull
    UUID configUUID;
    Long guildId;
    long channelId;
    long messageId;
    @NonNull
    String commandId;
    @NonNull
    String configClassId;
    @NonNull
    String config;
    @NonNull
    String stateDataClassId;
    @Nullable
    String stateData;

    public MessageDataDTO(@NonNull UUID configUUID, Long guildId, long channelId, long messageId, @NonNull String commandId, @NonNull String configClassId, @NonNull String config) {
        this.configUUID = configUUID;
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandId = commandId;
        this.configClassId = configClassId;
        this.config = config;
        this.stateDataClassId = Mapper.NO_PERSISTED_STATE;
        this.stateData = null;
    }
}
