package de.janno.discord.bot.command;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.persistance.*;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SnapshotExtension.class)
class ClearCommandTest {

    PersistenceManager persistenceManager = mock(PersistenceManager.class);
    ClearCommand underTest = new ClearCommand(persistenceManager);
    Expect expect;

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);
        when(slashEventAdaptor.reply(any(), anyBoolean())).thenReturn(Mono.empty());
        when(slashEventAdaptor.getChannelId()).thenReturn(0L);
        when(persistenceManager.deleteMessageDataForChannel(anyLong(), isNull())).thenReturn(ImmutableSet.of(1L, 2L));
        when(slashEventAdaptor.deleteMessageById(anyLong())).thenReturn(Mono.empty());

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);
        StepVerifier.create(res).verifyComplete();


        verify(persistenceManager).deleteMessageDataForChannel(0L, null);
        verify(persistenceManager).deleteAllChannelConfig(0L);
        verify(persistenceManager).deleteAllMessageConfigForChannel(0L, null);
        verify(slashEventAdaptor).deleteMessageById(1L);
        verify(slashEventAdaptor).deleteMessageById(2L);
    }

    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }

    @AfterEach
    void cleanup() {
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataIntervalInMilliSec", "0");
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataStartDelayMilliSec", "0");
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");
    }


    @Test
    public void mockTest() throws InterruptedException {
        long channelId = 1;
        long otherChannelId = 2;
        long guildId = 1;
        long messageId = 1;
        long otherMessageId = 2;
        String configClassId = "configClassId";
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataIntervalInMilliSec", "1");
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataStartDelayMilliSec", "0");
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");

        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new ClearCommand(persistenceManager);

        UUID configUUID = UUID.randomUUID();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUID, guildId, channelId, "commandId", "configClass", "config", null, null));
        UUID config2UUID = UUID.randomUUID();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(config2UUID, guildId, otherChannelId, "commandId", "configClass", "config", null, null));

        persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, "commandId", "stateDataClassId", "stateData"));
        persistenceManager.saveMessageData(new MessageDataDTO(config2UUID, guildId, otherChannelId, otherMessageId, "commandId", "stateDataClassId", "stateData"));

        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), guildId, channelId, null, "commandId", configClassId, "config", null));
        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), guildId, otherChannelId, null, "commandId", configClassId, "config", null));

        SlashEventAdaptorMock slashEventAdaptorMock = new SlashEventAdaptorMock(List.of());

        underTest.handleSlashCommandEvent(slashEventAdaptorMock, UUID::randomUUID, Locale.ENGLISH).block();
        Thread.sleep(10);

        assertThat(persistenceManager.getMessageConfig(configUUID)).isEmpty();
        assertThat(persistenceManager.getMessageConfig(config2UUID)).isPresent();

        assertThat(persistenceManager.getMessageData(channelId, messageId)).isEmpty();
        assertThat(persistenceManager.getMessageData(otherChannelId, otherMessageId)).isPresent();

        assertThat(persistenceManager.getChannelConfig(channelId, configClassId)).isEmpty();
        assertThat(persistenceManager.getChannelConfig(otherChannelId, configClassId)).isPresent();

    }

    @Test
    public void mockTest_name() throws InterruptedException {
        long channelId = 1;
        long otherChannelId = 2;
        long guildId = 1;
        long messageId = 1;
        long otherMessageId = 2;
        long message2Id = 3;
        String configClassId = "configClassId";
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataIntervalInMilliSec", "1");
        io.avaje.config.Config.setProperty("db.deleteMarkMessageDataStartDelayMilliSec", "0");
        io.avaje.config.Config.setProperty("db.delayMessageDataDeletionMs", "0");

        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new ClearCommand(persistenceManager);

        UUID configUUID = UUID.randomUUID();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUID, guildId, channelId, "commandId", "configClass", "config", "commandName", null));
        UUID config2UUID = UUID.randomUUID();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(config2UUID, guildId, otherChannelId, "commandId", "configClass", "config", "commandName", null));
        UUID config3UUID = UUID.randomUUID();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(config3UUID, guildId, channelId, "commandId", "configClass", "config", "commandName2", null));

        persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, "commandId", "stateDataClassId", "stateData"));
        persistenceManager.saveMessageData(new MessageDataDTO(config2UUID, guildId, otherChannelId, otherMessageId, "commandId", "stateDataClassId", "stateData"));
        persistenceManager.saveMessageData(new MessageDataDTO(config3UUID, guildId, channelId, message2Id, "commandId", "stateDataClassId", "stateData"));

        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), guildId, channelId, null, "commandId", configClassId, "config", "aliasName"));
        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), guildId, otherChannelId, null, "commandId", configClassId, "config", "aliasName"));
        assertThat(persistenceManager.getMessageConfig(config3UUID)).isPresent();

        SlashEventAdaptorMock slashEventAdaptorMock = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder().name("name").stringValue("commandName").build()));

        underTest.handleSlashCommandEvent(slashEventAdaptorMock, UUID::randomUUID, Locale.ENGLISH).block();
        Thread.sleep(20);

        assertThat(persistenceManager.getMessageConfig(configUUID)).isEmpty();
        assertThat(persistenceManager.getMessageConfig(config2UUID)).isPresent();
        assertThat(persistenceManager.getMessageConfig(config3UUID)).isPresent();

        assertThat(persistenceManager.getMessageData(channelId, messageId)).isEmpty();
        assertThat(persistenceManager.getMessageData(otherChannelId, otherMessageId)).isPresent();
        assertThat(persistenceManager.getMessageData(channelId, message2Id)).isPresent();

        assertThat(persistenceManager.getChannelConfig(channelId, configClassId)).isPresent();
        assertThat(persistenceManager.getChannelConfig(otherChannelId, configClassId)).isPresent();

    }

    @Test
    void getAutoCompleteAnswer_empty() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new ClearCommand(persistenceManager);

       List<AutoCompleteAnswer> res =  underTest.getAutoCompleteAnswer(new AutoCompleteRequest("name", "", List.of()), Locale.ENGLISH, 1L, 2L, 3L);

       assertThat(res).isEmpty();
    }

    @Test
    void getAutoCompleteAnswer_hit() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new ClearCommand(persistenceManager);
        persistenceManager.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 2L, 1L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        persistenceManager.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 2L, 1L, "testCommand", "testConfigClass", "configClass", "name1", 0L));
        persistenceManager.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 2L, 1L, "testCommand", "testConfigClass", "configClass", "name2", 0L));
        persistenceManager.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 2L, 2L, "testCommand", "testConfigClass", "configClass", "name3", 0L));
        persistenceManager.saveMessageConfig(new MessageConfigDTO(UUID.randomUUID(), 1L, 3L, "testCommand", "testConfigClass", "configClass", "name4", null));

        List<AutoCompleteAnswer> res =  underTest.getAutoCompleteAnswer(new AutoCompleteRequest("name", "", List.of()), Locale.ENGLISH, 1L, 2L, 3L);

        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("name1", "name2");
    }

}