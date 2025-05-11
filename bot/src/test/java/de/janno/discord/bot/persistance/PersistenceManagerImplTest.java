package de.janno.discord.bot.persistance;

import de.janno.discord.connector.api.ChildrenChannelCreationEvent;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
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
    void getAllActiveMessageIdsForConfig() {
        UUID uuid = UUID.randomUUID();
        Flux.range(1, 3)
                .map(i -> new MessageDataDTO(uuid, 1L, 1L, i, "testCommand", "testConfigClass", "configClass"))
                .delayElements(Duration.ofMillis(10))
                .doOnNext(underTest::saveMessageData)
                .blockLast();
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));

        Set<Long> res = underTest.getAllActiveMessageIdsForConfig(uuid);
        System.out.println(res);
        assertThat(res).containsExactlyInAnyOrder(3L, 2L, 1L);
    }

    @Test
    void deleteDataForMessage() {
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));

        underTest.deleteStateForMessage(2L, 4L);

        assertThat(underTest.getMessageData(2L, 4L)).isEmpty();
        assertThat(underTest.getMessageData(2L, 5L)).isPresent();
    }

    @AfterEach
    void cleanup() {
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataIntervalInMilliSec", "0");
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataStartDelayMilliSec", "0");
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");
    }


    @Test
    void markDeleteThenDeleteMessage() throws InterruptedException {
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataIntervalInMilliSec", "50");
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataStartDelayMilliSec", "50");

        underTest = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        UUID uuid = UUID.randomUUID();
        underTest.saveMessageData(new MessageDataDTO(uuid, 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.markAsDeleted(2L, 4L);

        Optional<MessageDataDTO> res = underTest.getMessageData(2L, 4L);
        assertThat(res.map(MessageDataDTO::getConfigUUID)).contains(uuid);

        Thread.sleep(120);

        Optional<MessageDataDTO> resAfterTime = underTest.getMessageData(2L, 4L);
        assertThat(resAfterTime.map(MessageDataDTO::getConfigUUID)).isEmpty();
    }

    @Test
    void deleteMessageDataForChannel() throws InterruptedException {
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");
        UUID config1 = UUID.randomUUID();
        UUID config2 = UUID.randomUUID();
        UUID config3 = UUID.randomUUID();
        underTest.saveMessageData(new MessageDataDTO(config1, 1L, 2L, 4L, "testCommand", "testStateClass", "stateData"));
        underTest.saveMessageData(new MessageDataDTO(config2, 1L, 2L, 5L, "testCommand", "testStateClass", "stateData"));
        underTest.saveMessageData(new MessageDataDTO(config3, 1L, 3L, 6L, "testCommand", "testStateClass", "stateData"));
        underTest.saveMessageConfig(new MessageConfigDTO(config1, 1L, 2L, "testCommand", "testConfigClass", "configClass", null, null));
        underTest.saveMessageConfig(new MessageConfigDTO(config2, 1L, 2L, "testCommand", "testConfigClass", "configClass", null, null));
        underTest.saveMessageConfig(new MessageConfigDTO(config3, 1L, 3L, "testCommand", "testConfigClass", "configClass", null, null));
        Thread.sleep(10);

        Set<Long> res = underTest.deleteMessageDataForChannel(2L, null);
        underTest.deleteOldMessageDataThatAreMarked();

        assertThat(res).containsExactly(4L, 5L);
        assertThat(underTest.getMessageData(2L, 4L)).isEmpty();
        assertThat(underTest.getMessageData(2L, 5L)).isEmpty();
        assertThat(underTest.getMessageData(3L, 6L)).isPresent();
        //not delete by this methode but by deleteAllMessageConfigForChannel
        assertThat(underTest.getMessageConfig(config1)).isPresent();
        assertThat(underTest.getMessageConfig(config2)).isPresent();
        assertThat(underTest.getMessageConfig(config3)).isPresent();
    }

    @Test
    void deleteMessageDataForChannel_name() throws InterruptedException {
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");
        UUID config1 = UUID.randomUUID();
        UUID config2 = UUID.randomUUID();
        UUID config3 = UUID.randomUUID();
        underTest.saveMessageData(new MessageDataDTO(config1, 1L, 2L, 4L, "testCommand", "testStateClass", "stateData"));
        underTest.saveMessageData(new MessageDataDTO(config2, 1L, 2L, 5L, "testCommand", "testStateClass", "stateData"));
        underTest.saveMessageData(new MessageDataDTO(config3, 1L, 3L, 6L, "testCommand", "testStateClass", "stateData"));
        underTest.saveMessageConfig(new MessageConfigDTO(config1, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", null));
        underTest.saveMessageConfig(new MessageConfigDTO(config2, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name2", null));
        underTest.saveMessageConfig(new MessageConfigDTO(config3, 1L, 3L, "testCommand", "testConfigClass", "configClass", "name1", null));
        Thread.sleep(10);

        Set<Long> res = underTest.deleteMessageDataForChannel(2L, "name1");
        assertThat(res).containsExactly(4L);
        underTest.deleteOldMessageDataThatAreMarked();

        assertThat(underTest.getMessageData(2L, 4L)).isEmpty();
        assertThat(underTest.getMessageData(2L, 5L)).isPresent();
        assertThat(underTest.getMessageData(3L, 6L)).isPresent();

        //not delete by this methode but by deleteAllMessageConfigForChannel
        assertThat(underTest.getMessageConfig(config1)).isPresent();
        assertThat(underTest.getMessageConfig(config2)).isPresent();
        assertThat(underTest.getMessageConfig(config3)).isPresent();
    }

    @Test
    void deleteAllMessageConfigForChannel() {
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");
        UUID config1 = UUID.randomUUID();
        UUID config2 = UUID.randomUUID();
        UUID config3 = UUID.randomUUID();
        underTest.saveMessageConfig(new MessageConfigDTO(config1, 1L, 2L, "testCommand", "testConfigClass", "configClass", null, null));
        underTest.saveMessageConfig(new MessageConfigDTO(config2, 1L, 2L, "testCommand", "testConfigClass", "configClass", null, null));
        underTest.saveMessageConfig(new MessageConfigDTO(config3, 1L, 3L, "testCommand", "testConfigClass", "configClass", null, null));


        underTest.deleteAllMessageConfigForChannel(2L, null);

        assertThat(underTest.getMessageConfig(config1)).isEmpty();
        assertThat(underTest.getMessageConfig(config2)).isEmpty();
        assertThat(underTest.getMessageConfig(config3)).isPresent();
    }

    @Test
    void deleteAllMessageConfigForChannel_name() {
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");
        UUID config1 = UUID.randomUUID();
        UUID config2 = UUID.randomUUID();
        UUID config3 = UUID.randomUUID();
        underTest.saveMessageConfig(new MessageConfigDTO(config1, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", null));
        underTest.saveMessageConfig(new MessageConfigDTO(config2, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name2", null));
        underTest.saveMessageConfig(new MessageConfigDTO(config3, 1L, 3L, "testCommand", "testConfigClass", "configClass", "name1", null));


        underTest.deleteAllMessageConfigForChannel(2L, "name1");


        assertThat(underTest.getMessageConfig(config1)).isEmpty();
        assertThat(underTest.getMessageConfig(config2)).isPresent();
        assertThat(underTest.getMessageConfig(config3)).isPresent();
    }

    @Test
    void getAllGuildIds() {
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 1L, 2L, 4L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), null, 2L, 5L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 2L, 3L, 6L, "testCommand", "testConfigClass", "configClass"));
        underTest.saveMessageData(new MessageDataDTO(UUID.randomUUID(), 2L, 4L, 7L, "testCommand", "testConfigClass", "configClass"));

        Set<Long> res = underTest.getAllGuildIds();

        assertThat(res).containsExactly(1L, 2L);
    }

    @Test
    void saveChannelData() {
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "testCommand", "testConfigClass", "configClass", null);
        ChannelConfigDTO channelConfigDTO2 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 3L, null, "testCommand", "testConfigClass", "configClass", null);
        underTest.saveChannelConfig(channelConfigDTO1);
        underTest.saveChannelConfig(channelConfigDTO2);

        assertThat(underTest.getChannelConfig(2L, "testConfigClass")).contains(channelConfigDTO1);
        assertThat(underTest.getChannelConfig(2L, "testConfigClassB")).isEmpty();
        assertThat(underTest.getChannelConfig(4L, "testConfigClass")).isEmpty();
        assertThat(underTest.getChannelConfig(3L, "testConfigClass")).contains(channelConfigDTO2);
    }

    @Test
    void deleteChannelData() {
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "testCommand", "testConfigClass", "configClass", null);
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
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "testCommand", "testConfigClass", "configClass", null);
        ChannelConfigDTO channelConfigDTO2 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "testCommand", "testConfigClass", "configClass2", null);
        underTest.saveChannelConfig(channelConfigDTO1);
        assertThrows(RuntimeException.class, () -> underTest.saveChannelConfig(channelConfigDTO2));
    }

    @Test
    void twoConfigsForSameConfigTypeAndChannelButDifferentUser() {
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, 1L, "testCommand", "testConfigClass", "configClass", null);
        ChannelConfigDTO channelConfigDTO2 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, 2L, "testCommand", "testConfigClass", "configClass2", null);
        underTest.saveChannelConfig(channelConfigDTO1);
        underTest.saveChannelConfig(channelConfigDTO2);
    }

    @Test
    void saveWithForbiddenUserKey() {
        ChannelConfigDTO channelConfigDTO1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, -1L, "testCommand", "testConfigClass", "configClass2", null);
        assertThrows(RuntimeException.class, () -> underTest.saveChannelConfig(channelConfigDTO1));
    }

    @Test
    void getNamedCommandsChannel() {
        underTest.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", null));
        underTest.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass", "name2", null));
        underTest.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 1L, 3L, "testCommand", "testConfigClass", "configClass", "name1", null));
        underTest.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", null));
        underTest.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 1L, 2L, "testCommand", "testConfigClass", "configClass", null, null));


        List<String> res = underTest.getNamedCommandsChannel(2L);


        assertThat(res).containsExactly("name1", "name2");

    }

    @Test
    void getLastUsedNamedCommandsOfUserAndGuild() throws InterruptedException {
        UUID config1 = UUID.randomUUID();
        UUID config2 = UUID.randomUUID();
        UUID config3 = UUID.randomUUID();
        UUID config4 = UUID.randomUUID();
        UUID config5 = UUID.randomUUID();
        UUID config6 = UUID.randomUUID();
        UUID config7 = UUID.randomUUID();

        underTest.saveMessageConfig(new MessageConfigDTO(config1, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config2, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name2", 0L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config3, 1L, 3L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config4, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config5, 1L, 2L, "testCommand", "testConfigClass", "configClass", null, 0L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config6, 3L, 2L, "testCommand", "testConfigClass", "configClass", null, 0L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config7, 3L, 3L, "testCommand", "testConfigClass", "configClass", "name3", 0L));
        Thread.sleep(10);


        List<SavedNamedConfigId> res = underTest.getLastUsedNamedCommandsOfUserAndGuild(0L, 1L);

        assertThat(res.stream().map(SavedNamedConfigId::id)).containsExactly(config4, config2, config7);
        assertThat(res.stream().map(SavedNamedConfigId::name)).containsExactly("name1", "name2", "name3");

    }

    @Test
    void testCopyChannelConfig_noConfig() {
        underTest.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, 3L, "testCommand", "testConfigClass", "configClass", "name1"));

        underTest.copyChannelConfig(new ChildrenChannelCreationEvent(4L, 3L));

        assertThat(underTest.getChannelConfig(3L, "testConfigClass")).isEmpty();
        assertThat(underTest.getChannelConfig(4L, "testConfigClass")).isEmpty();
    }

    @Test
    void testCopyChannelConfig_copyConfig() {
        underTest.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, 0L, "testCommand", "testConfigClass", "configClass", "name"));

        ChannelConfigDTO config1 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 3L, 1L, "testCommand1", "testConfigClass1", "configClass1", "name1");
        ChannelConfigDTO config2 = new ChannelConfigDTO(UUID.randomUUID(), 1L, 3L, null, "testCommand2", "testConfigClass2", "configClass2", "name2");
        underTest.saveChannelConfig(config1);
        underTest.saveChannelConfig(config2);

        underTest.copyChannelConfig(new ChildrenChannelCreationEvent(4L, 3L));

        Optional<ChannelConfigDTO> copied1 = underTest.getUserChannelConfig(4L, 1L, "testConfigClass1");
        Optional<ChannelConfigDTO> copied2 = underTest.getChannelConfig(4L, "testConfigClass2");

        assertThat(copied1).isPresent();
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(copied1.get().getConfigUUID()).isNotNull().isNotEqualTo(config1.getConfigUUID());
            a.assertThat(copied1.get().getGuildId()).isEqualTo(1L);
            a.assertThat(copied1.get().getChannelId()).isEqualTo(4L);
            a.assertThat(copied1.get().getCommandId()).isEqualTo("testCommand1");
            a.assertThat(copied1.get().getConfigClassId()).isEqualTo("testConfigClass1");
            a.assertThat(copied1.get().getConfig()).isEqualTo("configClass1");
            a.assertThat(copied1.get().getName()).isEqualTo("name1");
            a.assertThat(copied1.get().getUserId()).isEqualTo(1L);
        });
        assertThat(copied2).isPresent();

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(copied2.get().getConfigUUID()).isNotNull().isNotEqualTo(config2.getConfigUUID());
            a.assertThat(copied2.get().getGuildId()).isEqualTo(1L);
            a.assertThat(copied2.get().getChannelId()).isEqualTo(4L);
            a.assertThat(copied2.get().getCommandId()).isEqualTo("testCommand2");
            a.assertThat(copied2.get().getConfigClassId()).isEqualTo("testConfigClass2");
            a.assertThat(copied2.get().getConfig()).isEqualTo("configClass2");
            a.assertThat(copied2.get().getName()).isEqualTo("name2");
            a.assertThat(copied2.get().getUserId()).isNull();
        });
    }


}