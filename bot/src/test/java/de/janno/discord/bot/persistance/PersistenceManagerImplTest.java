package de.janno.discord.bot.persistance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersistenceManagerImplTest {
    PersistenceManagerImpl underTest;

    @BeforeEach
    void setup() {
        underTest = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void getAllMessageIdsForConfig() {
        UUID uuid = UUID.randomUUID();
        Flux.range(1, 3)
                .map(i -> new MessageStateDTO(uuid, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(underTest::saveMessageState)
                .blockLast();
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));

        Set<Long> res = underTest.getAllMessageIdsForConfig(uuid);
        System.out.println(res);
        assertThat(res).containsExactlyInAnyOrder(3L, 2L, 1L);
    }

    @Test
    void deleteDataForMessage() {
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));

        underTest.deleteStateForMessage(2L, 4L);

        assertThat(underTest.getStateForMessage(2L, 4L)).isEmpty();
        assertThat(underTest.getStateForMessage(2L, 5L)).isPresent();
    }

    @Test
    void deleteDataForChannel() {
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 3L, 6L, "testCommand", "testConfigClass", "configClass"));

        Set<Long> res = underTest.deleteMessageDataForChannel(2L);

        assertThat(res).containsExactly(4L, 5L);
        assertThat(underTest.getStateForMessage(2L, 4L)).isEmpty();
        assertThat(underTest.getStateForMessage(2L, 5L)).isEmpty();
        assertThat(underTest.getStateForMessage(3L, 6L)).isPresent();
    }

    @Test
    void getAllGuildIds() {
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), null, 2L, 5L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 2L, 3L, 6L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 2L, 4L, 7L, "testCommand", "testConfigClass", "configClass"));

        Set<Long> res = underTest.getAllGuildIds();

        assertThat(res).containsExactly(1L, 2L);
    }

    @Test
    void updateCommandConfigOfMessage() {
        underTest.saveMessageState(new MessageStateDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));

        underTest.updateOrCrateStateOfMessage(2L, 4L, "testConfigClass2", "state2");

        Optional<MessageStateDTO> res = underTest.getStateForMessage(2L, 4L);
        assertThat(res.map(MessageStateDTO::getStateData)).contains("state2");
        assertThat(res.map(MessageStateDTO::getStateDataClassId)).contains("testConfigClass2");
    }

    @Test
    void saveChannelData() {
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass");
        ChannelConfigDTO channelConfigDTO2 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 3L, "testCommand", "testConfigClass", "configClass");
        underTest.saveChannelConfig(channelConfigDTO1);
        underTest.saveChannelConfig(channelConfigDTO2);

        assertThat(underTest.getChannelConfig(2L, "testConfigClass")).contains(channelConfigDTO1);
        assertThat(underTest.getChannelConfig(2L, "testConfigClassB")).isEmpty();
        assertThat(underTest.getChannelConfig(4L, "testConfigClass")).isEmpty();
        assertThat(underTest.getChannelConfig(3L, "testConfigClass")).contains(channelConfigDTO2);
    }

    @Test
    void deleteChannelData() {
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass");
        underTest.saveChannelConfig(channelConfigDTO1);

        assertThat(underTest.getChannelConfig(2L, "testConfigClass")).contains(channelConfigDTO1);

        underTest.deleteChannelConfig(2L, "testConfigClass2");
        underTest.deleteChannelConfig(3L, "testConfigClass");
        assertThat(underTest.getChannelConfig(2L, "testConfigClass")).contains(channelConfigDTO1);

        underTest.deleteChannelConfig(2L, "testConfigClass");

        assertThat(underTest.getChannelConfig(2L, "testConfigClass")).isEmpty();
    }


    @Test
    void noTwoConfigsForSameConfigTypeAndChannel() {
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass");
        ChannelConfigDTO channelConfigDTO2 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass2");
        underTest.saveChannelConfig(channelConfigDTO1);
        assertThrows(RuntimeException.class, () -> underTest.saveChannelConfig(channelConfigDTO2));
    }

}