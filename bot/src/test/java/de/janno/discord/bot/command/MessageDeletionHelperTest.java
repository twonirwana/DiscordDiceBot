package de.janno.discord.bot.command;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.MessageState;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ParallelFlux;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
class MessageDeletionHelperTest {
    private Expect expect;

    @Test
    void deleteMessageAndData_notExist() {
        PersistenceManagerImpl persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", UUID.fromString("00000000-0000-0000-0000-000000000000"), new AtomicLong(), Set.of(2L)) {
            @Override
            public @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
                return Flux.fromIterable(messageIds).parallel().map(id -> new MessageState(id, false, false, true, OffsetDateTime.now().minusMinutes(id)));
            }

        };
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(persistenceManager::saveMessageData)
                .blockLast();

        MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, 1L, 0L, configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(persistenceManager.getAllMessageIdsForConfig(configUUID)).containsExactly(1L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactly();
    }

    @Test
    void deleteMessageAndData() {
        PersistenceManagerImpl persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", UUID.fromString("00000000-0000-0000-0000-000000000000"), new AtomicLong(), Set.of(2L));
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(persistenceManager::saveMessageData)
                .blockLast();

        MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, 6L, 0L, configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(persistenceManager.getAllMessageIdsForConfig(configUUID)).containsExactlyInAnyOrder(2L, 6L);
        expect.toMatchSnapshot(buttonEventAdaptorMock.getSortedActions());
    }

    @Test
    void deleteMessageAndData_deleteCache() throws InterruptedException {
        PersistenceManagerImpl persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);

        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", UUID.fromString("00000000-0000-0000-0000-000000000000"), new AtomicLong(), Set.of(2L));
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(persistenceManager::saveMessageData)
                .blockLast();

        MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, 6L, 0L, configUUID, 1L, buttonEventAdaptorMock).subscribe();
        Thread.sleep(100);
        MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, 6L, 0L, configUUID, 1L, buttonEventAdaptorMock).block();
        Thread.sleep(200);

        assertThat(persistenceManager.getAllMessageIdsForConfig(configUUID)).containsExactlyInAnyOrder(2L, 6L);
        expect.toMatchSnapshot(buttonEventAdaptorMock.getSortedActions());
    }
}