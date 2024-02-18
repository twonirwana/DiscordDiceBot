package de.janno.discord.bot.command;

import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.DiscordAdapter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Slf4j
public class MessageDeletionHelper {

    private final static ConcurrentSkipListSet<MessageIdAndChannelId> MESSAGE_STATE_IDS_TO_DELETE = new ConcurrentSkipListSet<>();

    public static Mono<Void> deleteOldMessageAndData(
            PersistenceManager persistenceManager,
            long newMessageId,
            @Nullable Long currentMessageId,
            @NonNull UUID configUUID,
            long channelId,
            @NonNull DiscordAdapter discordAdapter) {

        Set<Long> ids = persistenceManager.getAllMessageIdsForConfig(configUUID).stream()
                //this will already delete directly
                .filter(id -> !Objects.equals(id, currentMessageId))
                //we don't want to delete the new message
                .filter(id -> id != newMessageId)
                //we don't want to check the state of messages where the data is already scheduled to be deleted
                .filter(id -> !MESSAGE_STATE_IDS_TO_DELETE.contains(new MessageIdAndChannelId(id, channelId)))
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
                        return discordAdapter.deleteMessageById(ms.getMessageId())
                                .then(deleteMessageDataWithDelay(persistenceManager, channelId, ms.getMessageId()));
                    } else if (!ms.isExists()) {
                        return deleteMessageDataWithDelay(persistenceManager, channelId, ms.getMessageId());
                    } else {
                        return Mono.empty();
                    }
                }).then();
    }

    public static Mono<Void> deleteMessageDataWithDelay(PersistenceManager persistenceManager, long channelId, long messageId) {
        MessageIdAndChannelId messageIdAndChannelId = new MessageIdAndChannelId(messageId, channelId);
        MESSAGE_STATE_IDS_TO_DELETE.add(messageIdAndChannelId);
        final Duration delay = Duration.ofMillis(io.avaje.config.Config.getLong("command.delayMessageDataDeletionMs", 10000));
        return Mono.defer(() -> Mono.just(messageIdAndChannelId)
                .delayElement(delay)
                //add throttle?
                .doOnNext(mc -> {
                    MESSAGE_STATE_IDS_TO_DELETE.remove(mc);
                    persistenceManager.deleteStateForMessage(mc.channelId(), mc.messageId());
                }).ofType(Void.class));
    }

    public static List<Long> getMessageWaitingToBeDeleted(long channelId) {
        return MESSAGE_STATE_IDS_TO_DELETE.stream()
                .filter(mc -> mc.channelId() == channelId)
                .map(MessageIdAndChannelId::messageId)
                .toList();
    }

    private record MessageIdAndChannelId(long messageId, long channelId) implements Comparable<MessageIdAndChannelId> {
        @Override
        public int compareTo(@NonNull MessageIdAndChannelId o) {
            return this.toString().compareTo(o.toString());
        }
    }
}
