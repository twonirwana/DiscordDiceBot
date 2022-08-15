package de.janno.discord.bot.persistance;

import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.EmptyData;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MessageDataDAO {

    Optional<MessageDataDTO> getDataForMessage(long channelId, long messageId);

    Set<Long> getAllMessageIdsForConfig(UUID configUUID);

    void deleteDataForMessage(long channelId, long messageId);

    void deleteDataForChannel(long channelId);

     void saveMessageData(MessageDataDTO messageData);

    void updateCommandConfigOfMessage(long channelId, long messageId, @NonNull String stateDataClassId, @Nullable String stateData);
}
