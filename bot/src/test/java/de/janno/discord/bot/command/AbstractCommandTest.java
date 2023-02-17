package de.janno.discord.bot.command;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.MessageState;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ParallelFlux;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCommandTest {


    @Test
    void deleteMessageAndData_notExist() {
        PersistenceManagerImpl messageDataDAO = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        TestCommand underTest = new TestCommand(messageDataDAO);
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", UUID.randomUUID(), new AtomicLong(), Set.of(2L)) {
            @Override
            public @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
                return Flux.fromIterable(messageIds).parallel().map(id -> new MessageState(id, false, false, true, OffsetDateTime.now().minusMinutes(id)));
            }

        };
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();

        underTest.deleteOldAndConcurrentMessageAndData(1L, configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllMessageIdsForConfig(configUUID)).containsExactly(1L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactly();
    }

    @Test
    void deleteMessageAndData() {
        PersistenceManagerImpl messageDataDAO = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        TestCommand underTest = new TestCommand(messageDataDAO);
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", UUID.randomUUID(), new AtomicLong(), Set.of(2L));
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();


        underTest.deleteOldAndConcurrentMessageAndData(6L, configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllMessageIdsForConfig(configUUID)).containsExactlyInAnyOrder(2L, 6L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactlyInAnyOrder(
                "getMessagesState: [1, 2, 3, 4, 5, 7, 8, 9]",
                "deleteMessageById: 1",
                "deleteMessageById: 3",
                "deleteMessageById: 4",
                "deleteMessageById: 5",
                "deleteMessageById: 7",
                "deleteMessageById: 8",
                "deleteMessageById: 9");
    }

    @Test
    void deleteMessageAndData_deleteCache() throws InterruptedException {
        PersistenceManagerImpl messageDataDAO = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        TestCommand underTest = new TestCommand(messageDataDAO);
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(200));
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", UUID.randomUUID(), new AtomicLong(), Set.of(2L));
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();

        underTest.deleteOldAndConcurrentMessageAndData(6L, configUUID, 1L, buttonEventAdaptorMock).subscribe();
        Thread.sleep(100);
        underTest.deleteOldAndConcurrentMessageAndData(6L, configUUID, 1L, buttonEventAdaptorMock).block();
        Thread.sleep(200);

        assertThat(messageDataDAO.getAllMessageIdsForConfig(configUUID)).containsExactlyInAnyOrder(2L, 6L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactlyInAnyOrder(
                "getMessagesState: [1, 2, 3, 4, 5, 7, 8, 9]",
                "getMessagesState: [2]",
                "deleteMessageById: 1",
                "deleteMessageById: 3",
                "deleteMessageById: 4",
                "deleteMessageById: 5",
                "deleteMessageById: 7",
                "deleteMessageById: 8",
                "deleteMessageById: 9");
    }
}