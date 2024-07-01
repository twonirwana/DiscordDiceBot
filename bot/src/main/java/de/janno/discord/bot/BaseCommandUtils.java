package de.janno.discord.bot;

import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class BaseCommandUtils {

    public static MessageDataDTO createEmptyMessageData(@NonNull UUID configUUID,
                                                        @Nullable Long guildId,
                                                        long channelId,
                                                        long messageId,
                                                        String commandId,
                                                        PersistenceManager persistenceManager) {
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, guildId, channelId, messageId, commandId, Mapper.NO_PERSISTED_STATE, null);
        //should not be needed but sometimes there is a retry ect and then there is already a state
        persistenceManager.deleteStateForMessage(channelId, messageId);
        persistenceManager.saveMessageData(messageDataDTO);
        return messageDataDTO;
    }
}
