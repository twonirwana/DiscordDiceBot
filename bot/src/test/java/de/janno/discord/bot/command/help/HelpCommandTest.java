package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.connector.api.SlashEventAdaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(SnapshotExtension.class)
class HelpCommandTest {

    HelpCommand underTest = new HelpCommand();

    Expect expect;

    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);
        when(slashEventAdaptor.replyWithEmbedOrMessageDefinition(any(), anyBoolean())).thenReturn(Mono.empty());

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);
        StepVerifier.create(res).verifyComplete();
    }
}