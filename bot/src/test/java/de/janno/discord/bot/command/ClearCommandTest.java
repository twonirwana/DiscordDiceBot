package de.janno.discord.bot.command;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.persistance.*;
import de.janno.discord.connector.api.SlashEventAdaptor;
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
    private Expect expect;

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);
        when(slashEventAdaptor.reply(any(), anyBoolean())).thenReturn(Mono.empty());
        when(slashEventAdaptor.getChannelId()).thenReturn(0L);
        when(persistenceManager.deleteMessageDataForChannel(anyLong())).thenReturn(ImmutableSet.of(1L, 2L));
        when(slashEventAdaptor.deleteMessageById(anyLong())).thenReturn(Mono.empty());

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);
        StepVerifier.create(res).verifyComplete();


        verify(persistenceManager).deleteMessageDataForChannel(0L);
        verify(persistenceManager).deleteAllChannelConfig(0L);
        verify(persistenceManager).deleteAllMessageConfigForChannel(0L);
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

    @Test
    public void mockTest() {
        long channelId = 1;
        long otherChannelId = 2;
        long guildId = 1;
        long messageId = 1;
        String configClassId = "configClassId";
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new ClearCommand(persistenceManager);

        UUID configUUID = UUID.randomUUID();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUID, guildId, channelId, "commandId", "configClass", "config"));
        UUID config2UUID = UUID.randomUUID();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(config2UUID, guildId, otherChannelId, "commandId", "configClass", "config"));

        persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, "commandId", "stateDataClassId", "stateData"));
        persistenceManager.saveMessageData(new MessageDataDTO(config2UUID, guildId, otherChannelId, messageId, "commandId", "stateDataClassId", "stateData"));

        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), guildId, channelId, null, "commandId", configClassId, "config"));
        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(), guildId, otherChannelId, null, "commandId", configClassId, "config"));

        SlashEventAdaptorMock slashEventAdaptorMock = new SlashEventAdaptorMock(List.of());

        underTest.handleSlashCommandEvent(slashEventAdaptorMock, UUID::randomUUID, Locale.ENGLISH).block();

        assertThat(persistenceManager.getMessageConfig(configUUID)).isEmpty();
        assertThat(persistenceManager.getMessageConfig(config2UUID)).isPresent();

        assertThat(persistenceManager.getMessageData(channelId, messageId)).isEmpty();
        assertThat(persistenceManager.getMessageData(otherChannelId, messageId)).isPresent();

        assertThat(persistenceManager.getChannelConfig(channelId, configClassId)).isEmpty();
        assertThat(persistenceManager.getChannelConfig(otherChannelId, configClassId)).isPresent();

    }

}