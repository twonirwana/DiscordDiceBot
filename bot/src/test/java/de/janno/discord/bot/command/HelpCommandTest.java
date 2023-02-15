package de.janno.discord.bot.command;

import de.janno.discord.connector.api.SlashEventAdaptor;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HelpCommandTest {

    HelpCommand underTest = new HelpCommand();

    @Test
    void getCommandId() {
        assertThat(underTest.getCommandId()).isEqualTo("help");
    }

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);
        when(slashEventAdaptor.replyEmbed(any(), anyBoolean())).thenReturn(Mono.empty());

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
        StepVerifier.create(res).verifyComplete();
    }
}