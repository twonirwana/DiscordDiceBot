package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.SlashEventAdaptor;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClearCommandTest {

    final PersistenceManager persistenceManager = mock(PersistenceManager.class);
    final ClearCommand underTest = new ClearCommand(persistenceManager);

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("clear");
    }

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);
        when(slashEventAdaptor.reply(any(), anyBoolean())).thenReturn(Mono.empty());
        when(slashEventAdaptor.getChannelId()).thenReturn(0L);
        when(persistenceManager.deleteMessageDataForChannel(anyLong())).thenReturn(ImmutableSet.of(1L, 2L));
        when(slashEventAdaptor.deleteMessageById(anyLong())).thenReturn(Mono.empty());

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
        StepVerifier.create(res).verifyComplete();


        verify(persistenceManager).deleteMessageDataForChannel(0L);
        verify(slashEventAdaptor).deleteMessageById(1L);
        verify(slashEventAdaptor).deleteMessageById(2L);
    }

}