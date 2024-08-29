package de.janno.discord.bot.command.directRoll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
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

public class DirectRollCommandMockTest {
    PersistenceManager persistenceManager;
    Expect expect;

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
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void roll_multiLine() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("d[a\\nb\\nc,\\nd,e\\n]@\\nAttack\\nDown\\n")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void roll_warn() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("20")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }


    @Test
    void help() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("help")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void invalidExpression() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("d")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void roll_default_withLabel() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void roll_config_full_imageNone() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
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
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void roll_config_withoutExpression() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
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
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void roll_config_withoutExpression_withLabel() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
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
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void roll_config_compact() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
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
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void roll_config_minimal() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
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
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void channelAlias() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("att")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void userChannelAlias() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("att")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void channelRegexAlias() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("(\\d+)wod(\\d+)").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("$1d10>=$2c").build())
                        .option(CommandInteractionOption.builder().name("type").stringValue("Regex").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("8wod6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=8d10>=6c ⇒ 3, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED, userReference=true, sendToOtherChannelId=null)");
    }


}
