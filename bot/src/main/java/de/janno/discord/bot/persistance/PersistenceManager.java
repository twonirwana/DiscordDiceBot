package de.janno.discord.bot.persistance;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PersistenceManager {

    @NonNull Optional<MessageDataDTO> getDataForMessage(long channelId, long messageId);

    @NonNull Set<Long> getAllMessageIdsForConfig(@NonNull UUID configUUID);

    void deleteDataForMessage(long channelId, long messageId);

    @NonNull Set<Long> deleteMessageDataForChannel(long channelId);

    void saveMessageData(@NonNull MessageDataDTO messageData);

    void updateCommandConfigOfMessage(long channelId, long messageId, @NonNull String stateDataClassId, @Nullable String stateData);

    Set<Long> getAllGuildIds();

    @NonNull Optional<ChannelConfigDTO> getChannelConfig(long channelId, String configClassId);

    void saveChannelConfig(@NonNull ChannelConfigDTO channelConfigDTO);

    void deleteChannelConfig(long channelId, String configClassId);
}
