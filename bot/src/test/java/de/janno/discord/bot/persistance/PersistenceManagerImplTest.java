package de.janno.discord.bot.persistance;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.ChildrenChannelCreationEvent;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        underTest.markMessageDataAsDeleted(2L, 4L);

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
        UUID config8 = UUID.randomUUID();

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
        underTest.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 3L, 3L, "testCommand", "testConfigClass", "configClass", "name4", 1L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config8, 1L, 3L, "testCommand", "testConfigClass", "configClass", "name5", 1L));
        Thread.sleep(10);

        List<SavedNamedConfigId> res = underTest.getLastUsedNamedCommandsOfUserAndGuild(0L, 1L);
        assertThat(res.stream().map(SavedNamedConfigId::id)).containsExactly(config8, config7, config4, config2);
        assertThat(res.stream().map(SavedNamedConfigId::name)).containsExactly("name5", "name3", "name1", "name2");
    }

    @Test
    void getLastUsedNamedCommandsOfUser() throws InterruptedException {
        UUID config1 = UUID.randomUUID();
        UUID config2 = UUID.randomUUID();
        UUID config3 = UUID.randomUUID();
        UUID config4 = UUID.randomUUID();
        UUID config5 = UUID.randomUUID();
        UUID config6 = UUID.randomUUID();
        UUID config7 = UUID.randomUUID();
        UUID config8 = UUID.randomUUID();

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
        underTest.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 3L, 3L, "testCommand", "testConfigClass", "configClass", "name4", 1L));
        Thread.sleep(10);
        underTest.saveMessageConfig(new MessageConfigDTO(config8, 1L, 3L, "testCommand", "testConfigClass", "configClass", "name5", 1L));
        Thread.sleep(10);

        List<SavedNamedConfigId> res = underTest.getLastUsedNamedCommandsOfUserAndGuild(0L, null);
        assertThat(res.stream().map(SavedNamedConfigId::id)).containsExactly(config7, config4, config2);
        assertThat(res.stream().map(SavedNamedConfigId::name)).containsExactly("name3", "name1", "name2");
    }

    @Test
    @Disabled("Only use for performance tests")
    void getLastUsedNamedCommandsOfUserAndGuildSpeed() throws SQLException {
        String url = "jdbc:h2:XXX;AUTO_SERVER=TRUE";
        DatabaseConnector databaseConnector = new DatabaseConnector(url, null, null);
        final ImmutableList.Builder<Long> resultBuilder = ImmutableList.builder();
        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT DISTINCT CREATION_USER_ID FROM MESSAGE_CONFIG MC WHERE MC.CREATION_USER_ID IS NOT NULL limit 1000")) {
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    resultBuilder.add(resultSet.getLong("CREATION_USER_ID"));
                }
            }
        }
        List<Long> userIds = resultBuilder.build();
        underTest = new PersistenceManagerImpl(url, null, null);
        List<Long> guildIds = underTest.getAllGuildIds().stream().sorted().limit(1000).toList();
        long resultCount = 0;

        Random rand = new Random(0);

        List<GuildUser> warmup = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int guildIndex = rand.nextInt(guildIds.size());
            int userIndex = rand.nextInt(guildIds.size());
            warmup.add(new GuildUser(guildIds.get(guildIndex), userIds.get(userIndex)));
        }


        for (GuildUser guildUser : warmup) {
            resultCount += underTest.getLastUsedNamedCommandsOfUserAndGuild(guildUser.userId, guildUser.guildId).size();
        }

        List<GuildUser> guildUsers = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int guildIndex = rand.nextInt(guildIds.size());
            int userIndex = rand.nextInt(guildIds.size());
            guildUsers.add(new GuildUser(guildIds.get(guildIndex), userIds.get(userIndex)));
        }

        Stopwatch stopwatch = Stopwatch.createStarted();

        resultCount = guildUsers.stream()
                .mapToInt(guildUser -> underTest.getLastUsedNamedCommandsOfUserAndGuild(guildUser.userId, guildUser.guildId).size())
                .sum();
        stopwatch.stop();
        System.out.printf("%dms with %d results, avg: %.4fms\n", stopwatch.elapsed(TimeUnit.MILLISECONDS), resultCount, ((double) stopwatch.elapsed(TimeUnit.MILLISECONDS)) / resultCount);

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

    @Test
    void deleteOldMessageConfigThatAreMarked() throws InterruptedException {
        UUID config1 = UUID.randomUUID();
        underTest.saveMessageConfig(new MessageConfigDTO(config1, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        UUID config2 = UUID.randomUUID();
        underTest.saveMessageConfig(new MessageConfigDTO(config2, 2L, 2L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        long markCount = underTest.markDeleteAllForGuild(List.of(1L));
        Thread.sleep(20);

        io.avaje.config.Config.setProperty("db.delayMessageConfigDeletionMs", "1");
        underTest.deleteOldMessageConfigThatAreMarked();

        assertThat(markCount).isEqualTo(1);
        assertThat(underTest.getMessageConfig(config1)).isEmpty();
        assertThat(underTest.getMessageConfig(config2)).isPresent();
    }

    @Test
    void deleteOldChannelConfigThatAreMarked() throws InterruptedException {
        UUID config1 = UUID.randomUUID();
        underTest.saveChannelConfig(new ChannelConfigDTO(config1, 1L, 1L, null, "testCommand2", "testConfigClass1", "configClass2", "name2"));
        UUID config2 = UUID.randomUUID();
        underTest.saveChannelConfig(new ChannelConfigDTO(config2, 2L, 2L, null, "testCommand2", "testConfigClass2", "configClass2", "name2"));
        long markCount = underTest.markDeleteAllForGuild(List.of(1L, 3L));
        Thread.sleep(20);

        io.avaje.config.Config.setProperty("db.delayChannelConfigDeletionMs", "1");
        underTest.deleteOldChannelConfigThatAreMarked();

        assertThat(markCount).isEqualTo(1);
        assertThat(underTest.getChannelConfig(1L, "testConfigClass1")).isEmpty();
        assertThat(underTest.getChannelConfig(2L, "testConfigClass2")).isPresent();
    }

    @Test
    void deleteOldMessageDataThatAreMarked() throws InterruptedException {
        UUID messageData1 = UUID.randomUUID();
        underTest.saveMessageData(new MessageDataDTO(messageData1, 1L, 1L, 4L, "testCommand", "testConfigClass", "configClass"));
        UUID messageData2 = UUID.randomUUID();
        underTest.saveMessageData(new MessageDataDTO(messageData2, 2L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));
        long markCount = underTest.markDeleteAllForGuild(List.of(1L, 3L));
        Thread.sleep(20);

        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "1");
        underTest.deleteOldMessageDataThatAreMarked();

        assertThat(markCount).isEqualTo(1);
        assertThat(underTest.getMessageData(1L, 4L)).isEmpty();
        assertThat(underTest.getMessageData(2L, 5L)).isPresent();
    }

    @Test
    void undoMarkDelete() throws InterruptedException {
        UUID config1 = UUID.randomUUID();
        underTest.saveMessageConfig(new MessageConfigDTO(config1, 1L, 2L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        UUID config2 = UUID.randomUUID();
        underTest.saveMessageConfig(new MessageConfigDTO(config2, 2L, 2L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        UUID channelConfig1 = UUID.randomUUID();
        underTest.saveChannelConfig(new ChannelConfigDTO(channelConfig1, 1L, 1L, null, "testCommand2", "testConfigClass1", "configClass2", "name2"));
        UUID channelConfig2 = UUID.randomUUID();
        underTest.saveChannelConfig(new ChannelConfigDTO(channelConfig2, 2L, 2L, null, "testCommand2", "testConfigClass2", "configClass2", "name2"));
        UUID messageData1 = UUID.randomUUID();
        underTest.saveMessageData(new MessageDataDTO(messageData1, 1L, 1L, 4L, "testCommand", "testConfigClass", "configClass"));
        UUID messageData2 = UUID.randomUUID();
        underTest.saveMessageData(new MessageDataDTO(messageData2, 2L, 2L, 5L, "testCommand", "testConfigClass", "configClass"));

        long markCount = underTest.markDeleteAllForGuild(List.of(1L, 3L));
        Thread.sleep(20);

        underTest.undoMarkDelete(1L);

        io.avaje.config.Config.setProperty("db.delayMessageConfigDeletionMs", "1");
        underTest.deleteOldMessageConfigThatAreMarked();
        io.avaje.config.Config.setProperty("db.delayChannelConfigDeletionMs", "1");
        underTest.deleteOldChannelConfigThatAreMarked();
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "1");
        underTest.deleteOldMessageDataThatAreMarked();


        assertThat(markCount).isEqualTo(3);
        assertThat(underTest.getMessageConfig(config1)).isPresent();
        assertThat(underTest.getMessageConfig(config2)).isPresent();
        assertThat(underTest.getChannelConfig(1L, "testConfigClass1")).isPresent();
        assertThat(underTest.getChannelConfig(2L, "testConfigClass2")).isPresent();
        assertThat(underTest.getMessageData(1L, 4L)).isEmpty();
        assertThat(underTest.getMessageData(2L, 5L)).isPresent();
    }

    record GuildUser(Long guildId, Long userId) {
    }


}