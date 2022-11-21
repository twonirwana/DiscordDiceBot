package de.janno.discord.bot.persistance;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDataDAOImplTest {
    MessageDataDAOImpl underTest = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);


    @Test
    void getAllMessageIdsForConfig() {
        UUID uuid = UUID.randomUUID();
        Flux.range(1, 3)
                .map(i -> new MessageDataDTO(uuid, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(underTest::saveMessageData)
                .blockLast();
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));

        Set<Long> res = underTest.getAllMessageIdsForConfig(uuid);
        System.out.println(res);
        assertThat(res).containsExactlyInAnyOrder(3L, 2L, 1L);
    }

    @Test
    void deleteDataForMessage() {
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));

        underTest.deleteDataForMessage(2L, 4L);

        assertThat(underTest.getDataForMessage(2L, 4L)).isEmpty();
        assertThat(underTest.getDataForMessage(2L, 5L)).isPresent();
    }

    @Test
    void deleteDataForChannel() {
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 3L, 6L, "testCommand", "testConfigClass", "configClass"));

        Set<Long> res = underTest.deleteDataForChannel(2L);

        assertThat(res).containsExactly(4L, 5L);
        assertThat(underTest.getDataForMessage(2L, 4L)).isEmpty();
        assertThat(underTest.getDataForMessage(2L, 5L)).isEmpty();
        assertThat(underTest.getDataForMessage(3L, 6L)).isPresent();
    }

}