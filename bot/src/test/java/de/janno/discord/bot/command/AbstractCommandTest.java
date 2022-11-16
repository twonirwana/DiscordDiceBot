package de.janno.discord.bot.command;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCommandTest {

    MessageDataDAOImpl messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    TestCommand underTest = new TestCommand(messageDataDAO);

    @Test
    void deleteMessageAndData_pinned() {
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", new AtomicLong(), Set.of(2L));
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();

        underTest.deleteMessageAndData(configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllAfterTheNewestMessageIdsForConfig(configUUID)).containsExactly(2L, 5L, 6L, 7L, 8L, 9L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactly("deleteMessage: 1", "deleteMessage: 3", "deleteMessage: 4");
    }

    @Test
    void deleteMessageAndData() {
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", new AtomicLong(), Set.of(2L));
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 9)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();


        underTest.deleteMessageAndData(configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllAfterTheNewestMessageIdsForConfig(configUUID)).containsExactly(2L, 5L, 6L, 7L, 8L, 9L);
        assertThat(buttonEventAdaptorMock.getActions()).containsExactly("deleteMessage: 1", "deleteMessage: 3", "deleteMessage: 4");
    }

    @Test
    void deleteMessageAndData_lessThen5() {
        ButtonEventAdaptorMock buttonEventAdaptorMock = new ButtonEventAdaptorMock("testCommand", "a", new AtomicLong(), Set.of());
        UUID configUUID = UUID.randomUUID();
        Flux.range(1, 4)
                .map(i -> new MessageDataDTO(configUUID, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(messageDataDAO::saveMessageData)
                .blockLast();

        underTest.deleteMessageAndData(configUUID, 1L, buttonEventAdaptorMock).block();

        assertThat(messageDataDAO.getAllAfterTheNewestMessageIdsForConfig(configUUID)).containsExactly(1L, 2L, 3L, 4L);
        assertThat(buttonEventAdaptorMock.getActions()).isEmpty();
    }
}