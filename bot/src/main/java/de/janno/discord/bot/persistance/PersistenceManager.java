package de.janno.discord.bot.persistance;

import lombok.NonNull;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PersistenceManager {

    @NonNull
    Optional<MessageConfigDTO> getMessageConfig(@NonNull UUID configUUID);

    void deleteAllMessageConfigForChannel(long channelId);

    @NonNull
    Optional<MessageConfigDTO> getConfigFromMessage(long channelId, long messageId);

    void saveMessageConfig(@NonNull MessageConfigDTO messageConfigDTO);

    @NonNull
    Optional<MessageDataDTO> getMessageData(long channelId, long messageId);

    @NonNull
    Set<Long> getAllMessageIdsForConfig(@NonNull UUID configUUID);

    void deleteStateForMessage(long channelId, long messageId);

    @NonNull
    Set<Long> deleteMessageDataForChannel(long channelId);

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

    Optional<MessageConfigDTO> getLastMessageDataInChannel(long channelId, LocalDateTime since, @Nullable Long alreadyDeletedMessageId);

    void deleteMessageConfig(UUID configUUID);

    List<NamedCommand> getNamedCommandsForChannel(long userId, Long guildId);
}
