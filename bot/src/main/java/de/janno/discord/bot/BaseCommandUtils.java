package de.janno.discord.bot;

import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import lombok.NonNull;

import java.util.UUID;

public class BaseCommandUtils {

    /**
     * Create a new message data with empty state, delete other states for this and save it
     */
    public static MessageDataDTO createCleanupAndSaveEmptyMessageData(@NonNull UUID configUUID,
                                                                      Long guildId,
                                                                      long channelId,
                                                                      long messageId,
                                                                      String commandId,
                                                                      PersistenceManager persistenceManager) {
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, guildId, channelId, messageId, commandId, Mapper.NO_PERSISTED_STATE, null);
        //should not be needed but sometimes there is a retry and then there is already a state
        persistenceManager.deleteStateForMessage(channelId, messageId);
        persistenceManager.saveMessageData(messageDataDTO);
        return messageDataDTO;
    }
}
