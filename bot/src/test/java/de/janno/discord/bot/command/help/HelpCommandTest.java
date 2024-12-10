package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.SlashEventAdaptorMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
        SlashEventAdaptorMock slashEventAdaptor = new SlashEventAdaptorMock(List.of());

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);
        StepVerifier.create(res).verifyComplete();

        expect.toMatchSnapshot(slashEventAdaptor.getSortedActions());
    }

    @Test
    void handleSlashCommandEvent_userInstall() {
        SlashEventAdaptorMock slashEventAdaptor = new SlashEventAdaptorMock(List.of());
        slashEventAdaptor.setUserInstallInteraction(true);

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);
        StepVerifier.create(res).verifyComplete();

        expect.toMatchSnapshot(slashEventAdaptor.getSortedActions());
    }
}