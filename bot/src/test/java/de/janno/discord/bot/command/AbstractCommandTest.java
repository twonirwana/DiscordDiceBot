package de.janno.discord.bot.command;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.MessageState;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

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
        MessageDataDAOImpl messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        TestCommand underTest = new TestCommand(messageDataDAO);
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", new AtomicLong(), Set.of(2L)){
            @Override
            public @NonNull Flux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
                return Flux.fromIterable(messageIds).map(id -> new MessageState(id, false, false, true, OffsetDateTime.now().minusMinutes(id)));
            }

        };
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();

        underTest.deleteMessageAndData(configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllAfterTheNewestMessageIdsForConfig(configUUID)).containsExactly();
        assertThat(buttonEventAdaptorMock.getActions()).containsExactlyInAnyOrder();
    }

    @Test
    void deleteMessageAndData() {
        MessageDataDAOImpl messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        TestCommand underTest = new TestCommand(messageDataDAO);
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", new AtomicLong(), Set.of(2L));
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();


        underTest.deleteMessageAndData(configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllAfterTheNewestMessageIdsForConfig(configUUID)).containsExactly(5L, 4L, 3L, 2L, 1L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactlyInAnyOrder("deleteMessageById: 8",
                "deleteMessageById: 7",
                "deleteMessageById: 6",
                "deleteMessageById: 5",
                "deleteMessageById: 4",
                "deleteMessageById: 3",
                "deleteMessageById: 1");
    }

    @Test
    void deleteMessageAndData_olderThen5min() {
        MessageDataDAOImpl messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        TestCommand underTest = new TestCommand(messageDataDAO);
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", new AtomicLong(), Set.of(8L));
        UUID configUUID = UUID.randomUUID();
        Flux.range(6, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();

        underTest.deleteMessageAndData(configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllAfterTheNewestMessageIdsForConfig(configUUID)).containsExactly(8L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactlyInAnyOrder("deleteMessageById: 13",
                "deleteMessageById: 12",
                "deleteMessageById: 11",
                "deleteMessageById: 10",
                "deleteMessageById: 9",
                "deleteMessageById: 7",
                "deleteMessageById: 6");
    }
}