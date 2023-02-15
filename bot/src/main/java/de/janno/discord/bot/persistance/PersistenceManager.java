package de.janno.discord.bot.persistance;

import lombok.NonNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PersistenceManager {

    @NonNull Optional<MessageConfigDTO> getMessageConfig(@NonNull UUID configUUID);

    @NonNull Optional<MessageConfigDTO> getConfigFromMessage(long channelId, long messageId);

    void saveMessageConfig(@NonNull MessageConfigDTO messageConfigDTO);

    @NonNull Optional<MessageDataDTO> getStateForMessage(long channelId, long messageId);

    @NonNull Set<Long> getAllMessageIdsForConfig(@NonNull UUID configUUID);

    void deleteStateForMessage(long channelId, long messageId);

    @NonNull Set<Long> deleteMessageDataForChannel(long channelId);

    void saveMessageData(@NonNull MessageDataDTO messageState);

    Set<Long> getAllGuildIds();

    @NonNull Optional<ChannelConfigDTO> getChannelConfig(long channelId, String configClassId);

    void saveChannelConfig(@NonNull ChannelConfigDTO channelConfigDTO);

    void deleteChannelConfig(long channelId, String configClassId);
}
