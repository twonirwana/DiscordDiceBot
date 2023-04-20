package de.janno.discord.bot.command.channelConfig;

import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ChannelConfigMockTest {

    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() {
        try {
            FileUtils.cleanDirectory(new File("imageCache/"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }


    @Test
    void saveConfigDelete_default() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
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
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("delete_direct_roll_config")
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nDeleted direct roll channel config");
        assertThat(slashEvent3.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=1d6 ⇒ 1, descriptionOrContent=, fields=[], file=cea2a67e61a8b605c6702aac213960f86922331b5cac795649502b363dde97aa.png, type=EMBED)");
    }

    @Test
    void channelAlias_createListDelete() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("delete")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nexisting alias:\natt->2d20+10");
        assertThat(slashEvent3.getActions()).containsExactlyInAnyOrder("reply: `commandString`\ndeleted alias");
        assertThat(slashEvent4.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nexisting alias:\n");
    }

    @Test
    void userChannelAlias_createListDelete() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("delete")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nexisting alias:\natt->2d20+10");
        assertThat(slashEvent3.getActions()).containsExactlyInAnyOrder("reply: `commandString`\ndeleted alias");
        assertThat(slashEvent4.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nexisting alias:\n");
    }

    @Test
    void userChannelAlias_2UserCreateAlias() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .build()),1L);
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000")).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+1").build())
                        .build())
                .build()),2L);
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000001")).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .build()),1L);
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000002")).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .build()),2L);
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000003")).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(slashEvent3.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nexisting alias:\natt->2d20+10");
        assertThat(slashEvent4.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nexisting alias:\natt->2d20+1");
    }
}
