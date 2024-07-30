package de.janno.discord.bot.command.directRoll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class HiddenRollCommandMockTest {
    PersistenceManager persistenceManager;
    private Expect expect;

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_default() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event2").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_NoWarn() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("20")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event2").toMatchSnapshot(buttonEvent.getSortedActions());
    }


    @Test
    void help() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("help")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());
    }

    @Test
    void invalidExpression() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("d")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());
    }

    @Test
    void roll_default_withLabel() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.scenario("event1").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event2").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_config_full_imageNone() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("always_sum_result")
                        .booleanValue(false)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("answer_format")
                        .stringValue("without_expression")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("dice_image_style")
                        .stringValue("none")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(configCommandEvent.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event3").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_config_withoutExpression() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("always_sum_result")
                        .booleanValue(false)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("answer_format")
                        .stringValue("without_expression")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("result_image")
                        .stringValue("polyhedral_3d_red_and_white")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(configCommandEvent.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event3").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_config_withoutExpression_withLabel() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("always_sum_result")
                        .booleanValue(false)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("answer_format")
                        .stringValue("without_expression")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("result_image")
                        .stringValue("polyhedral_3d_red_and_white")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(configCommandEvent.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event3").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_config_compact() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("always_sum_result")
                        .booleanValue(false)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("answer_format")
                        .stringValue("compact")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("result_image")
                        .stringValue("polyhedral_3d_red_and_white")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(configCommandEvent.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event3").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_config_minimal() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("always_sum_result")
                        .booleanValue(false)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("answer_format")
                        .stringValue("minimal")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("result_image")
                        .stringValue("polyhedral_3d_red_and_white")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(configCommandEvent.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event3").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void channelAlias() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("att")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(configCommandEvent.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event3").toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void userChannelAlias() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("att")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(configCommandEvent.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(hiddenRollCommandEvent.getSortedActions());

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.getFirst();

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("event3").toMatchSnapshot(buttonEvent.getSortedActions());
    }

}
