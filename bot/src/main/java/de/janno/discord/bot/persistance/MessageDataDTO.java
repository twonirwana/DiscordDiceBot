package de.janno.discord.bot.persistance;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Value
public class MessageDataDTO {

    @NonNull
    UUID configUUID;
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

    public MessageDataDTO(@NonNull UUID configUUID, long channelId, long messageId, @NonNull String commandId, @NonNull String configClassId, @NonNull String config) {
        this.configUUID = configUUID;
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandId = commandId;
        this.configClassId = configClassId;
        this.config = config;
        this.stateDataClassId = Mapper.NO_PERSISTED_STATE;
        this.stateData = null;
    }

    public MessageDataDTO(@NonNull UUID configUUID, long channelId, long messageId, @NonNull String commandId, @NonNull String configClassId, @NonNull String config, @NonNull String stateDataClassId, @NonNull String stateData) {
        this.configUUID = configUUID;
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandId = commandId;
        this.configClassId = configClassId;
        this.config = config;
        this.stateDataClassId = stateDataClassId;
        this.stateData = stateData;
    }
}
