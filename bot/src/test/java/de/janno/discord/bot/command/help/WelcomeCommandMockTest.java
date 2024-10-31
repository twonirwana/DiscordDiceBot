package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class WelcomeCommandMockTest {


  PersistenceManager persistenceManager;
    Expect expect;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void slashStartWelcome_firstButton() {
        WelcomeCommand underTest = getWelcomeCommand();

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        expect.scenario("slash").toMatchSnapshot(slashEvent.getSortedActions());

        Optional<ButtonEventAdaptorMock> buttonEvent = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEvent).isPresent();
        underTest.handleComponentInteractEvent(buttonEvent.get()).block();
        expect.scenario("button").toMatchSnapshot(buttonEvent.get().getSortedActions());
    }

    @Test
    void slashStartWelcome_legacyDnD5Id() {
        WelcomeCommand underTest = getWelcomeCommand();

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        expect.scenario("slash").toMatchSnapshot(slashEvent.getSortedActions());

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("welcome", "dnd5_image", new AtomicLong(0));
        underTest.handleComponentInteractEvent(buttonEvent).block();
        expect.scenario("button").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void slashStartWelcome_invalidId() {
        WelcomeCommand underTest = getWelcomeCommand();

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        expect.scenario("slash").toMatchSnapshot(slashEvent.getSortedActions());

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("welcome", "invalidId", new AtomicLong(0));
        underTest.handleComponentInteractEvent(buttonEvent).block();
        expect.scenario("button").toMatchSnapshot(buttonEvent.getSortedActions());
    }


    private WelcomeCommand getWelcomeCommand() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));

        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        ChannelConfigCommand channelConfigCommand = new ChannelConfigCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);

        return new WelcomeCommand(persistenceManager, rpgSystemCommandPreset, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }



}
