package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.persistance.PersistanceManager;
import de.janno.discord.connector.api.SlashEventAdaptor;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClearCommandTest {

    final PersistanceManager persistanceManager = mock(PersistanceManager.class);
    final ClearCommand underTest = new ClearCommand(persistanceManager);

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("clear");
    }

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);
        when(slashEventAdaptor.reply(any(), anyBoolean())).thenReturn(Mono.empty());
        when(slashEventAdaptor.getChannelId()).thenReturn(0L);
        when(persistanceManager.deleteMessageDataForChannel(anyLong())).thenReturn(ImmutableSet.of(1L, 2L));
        when(slashEventAdaptor.deleteMessageById(anyLong())).thenReturn(Mono.empty());

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor);
        StepVerifier.create(res).verifyComplete();


        verify(persistanceManager).deleteMessageDataForChannel(0L);
        verify(slashEventAdaptor).deleteMessageById(1L);
        verify(slashEventAdaptor).deleteMessageById(2L);
    }

}