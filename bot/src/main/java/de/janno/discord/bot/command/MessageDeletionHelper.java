package de.janno.discord.bot.command;

import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.DiscordAdapter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class MessageDeletionHelper {


    public static Mono<Void> deleteOldMessageAndData(
            PersistenceManager persistenceManager,
            long newMessageId,
            @Nullable Long currentMessageId,
            @NonNull UUID configUUID,
            long channelId,
            @NonNull DiscordAdapter discordAdapter) {
        Set<Long> ids = persistenceManager.getAllActiveMessageIdsForConfig(configUUID).stream()
                //this will already delete directly
                .filter(id -> !Objects.equals(id, currentMessageId))
                //we don't want to delete the new message
                .filter(id -> id != newMessageId)
                .collect(Collectors.toSet());

        if (ids.size() > 5) { //there should be not many old message data
            log.warn(String.format("ConfigUUID %s had %d to many messageData persisted", configUUID, ids.size()));
        }

        if (ids.isEmpty()) {
            return Mono.empty();
        }

        return discordAdapter.getMessagesState(ids)
                .flatMap(ms -> {
                    if (ms.isCanBeDeleted() && !ms.isPinned() && ms.isExists() && ms.getCreationTime() != null) {
                        return Mono.defer(() -> discordAdapter.deleteMessageById(ms.getMessageId()))
                                .then(Mono.defer(() -> markAsDeleted(persistenceManager, channelId, ms.getMessageId())));
                    } else if (!ms.isExists()) {
                        return Mono.defer(() -> markAsDeleted(persistenceManager, channelId, ms.getMessageId()));
                    } else {
                        return Mono.empty();
                    }
                })
                .then();
    }


    public static Mono<Void> markAsDeleted(PersistenceManager persistenceManager, long channelId, long messageId) {
        persistenceManager.markAsDeleted(channelId, messageId);
        return Mono.empty();
    }

}
