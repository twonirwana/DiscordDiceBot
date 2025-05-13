package de.janno.discord.bot.persistance;

import de.janno.discord.connector.api.ChildrenChannelCreationEvent;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PersistenceManager {

    @NonNull
    Optional<MessageConfigDTO> getMessageConfig(@NonNull UUID configUUID);

    /**
     * @param name can be null
     */
    void deleteAllMessageConfigForChannel(long channelId, String name);

    void saveMessageConfig(@NonNull MessageConfigDTO messageConfigDTO);

    @NonNull
    Optional<MessageDataDTO> getMessageData(long channelId, long messageId);

    @NonNull
    Set<Long> getAllActiveMessageIdsForConfig(@NonNull UUID configUUID);

    void markAsDeleted(long channelId, long messageId);

    void deleteStateForMessage(long channelId, long messageId);

    /**
     * @param name can be null
     */
    @NonNull
    Set<Long> deleteMessageDataForChannel(long channelId, String name);

    void saveMessageData(@NonNull MessageDataDTO messageState);

    Set<Long> getAllGuildIds();

    @NonNull
    Optional<ChannelConfigDTO> getChannelConfig(long channelId, @NonNull String configClassId);

    @NonNull
    Optional<ChannelConfigDTO> getUserChannelConfig(long channelId, long userId, @NonNull String configClassId);

    void saveChannelConfig(@NonNull ChannelConfigDTO channelConfigDTO);

    void deleteChannelConfig(long channelId, String configClassId);

    void deleteUserChannelConfig(long channelId, long userId, String configClassId);

    void deleteAllChannelConfig(long channelId);

    Optional<MessageConfigDTO> getNewestMessageDataInChannel(long channelId, LocalDateTime since, Set<String> commandIds);

    void deleteMessageConfig(UUID configUUID);

    /**
     * If there are commands with the same name, the last used one will be returned
     */
    List<SavedNamedConfigId> getLastUsedNamedCommandsOfUserAndGuild(long userId, Long guildId);

    List<String> getNamedCommandsChannel(long channelId);

    void copyChannelConfig(@NonNull ChildrenChannelCreationEvent event);
}
