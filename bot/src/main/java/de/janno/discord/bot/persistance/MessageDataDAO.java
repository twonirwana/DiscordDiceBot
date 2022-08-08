package de.janno.discord.bot.persistance;

import de.janno.discord.bot.command.StateData;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MessageDataDAO {

    Optional<MessageData> getDataForMessage(long channelId, long messageId);

    Set<Long> getAllMessageIdsForConfig(UUID configUUID);

    void deleteDataForMessage(long channelId, long messageId);

    void deleteDataForChannel(long channelId);

    void saveMessageData(MessageData messageData);

    void updateCommandConfigOfMessage(long channelId, long messageId, String stateId, StateData state);
}
