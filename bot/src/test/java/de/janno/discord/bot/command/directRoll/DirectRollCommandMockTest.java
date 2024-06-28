package de.janno.discord.bot.command.directRoll;

import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
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
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectRollCommandMockTest {
    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if(cacheDirectory.exists()){
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


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=1d6 ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED, userReference=true, sendToOtherChannelId=null)");
    }

    @Test
    void roll_multiLine() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("d[a\\nb\\nc,\\nd,e\\n]@\\nAttack\\nDown\\n")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                """
                        sendMessage: EmbedOrMessageDefinition(title=
                        Attack
                        Down
                         ⇒ a
                        b
                        c, descriptionOrContent=d[a
                        b
                        c,
                        d,e
                        ]: [a
                        b
                        c], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED, userReference=true, sendToOtherChannelId=null)""");
    }

    @Test
    void roll_warn() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("20")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString did not contain any random element, try for Example `d20` to roll a 20 sided die",
                "sendMessage: EmbedOrMessageDefinition(title=20 ⇒ 20, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED, userReference=true, sendToOtherChannelId=null)"
        );
    }


    @Test
    void help() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("help")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=null, descriptionOrContent=Type `/r` and a dice expression. It is possible to give the roll a label by adding it with a `@`. The output type, dice images etc. can be configuration with `/channel_config save_direct_roll_config`\n" +
                        DiceEvaluatorAdapter.getHelp() +
                        ", fields=[EmbedOrMessageDefinition.Field(name=Example, value=`/r expression: 1d6@Damage`, inline=false), EmbedOrMessageDefinition.Field(name=Full documentation, value=https://github.com/twonirwana/DiscordDiceBot, inline=false), EmbedOrMessageDefinition.Field(name=Discord Server for News, Help and Feature Requests, value=https://discord.gg/e43BsqKpFr, inline=false)], componentRowDefinitions=[], hasImage=false, type=EMBED, userReference=false, sendToOtherChannelId=null)");
    }

    @Test
    void invalidExpression() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("d")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString\n" +
                        "The following expression is invalid: __d__. The error is: Operator d has right associativity but the right value was: empty. Use `/r expression:help` to get more information on how to use the command.");
    }

    @Test
    void roll_default_withLabel() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=test ⇒ 1, descriptionOrContent=1d6, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED, userReference=true, sendToOtherChannelId=null)"
        );
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

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=Roll ⇒ 1, descriptionOrContent=[1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED, userReference=true, sendToOtherChannelId=null)"
        );
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

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=Roll ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED, userReference=true, sendToOtherChannelId=null)");
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

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=test ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED, userReference=true, sendToOtherChannelId=null)");
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

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**1d6 ⇒ 1**__  [1], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE, userReference=true, sendToOtherChannelId=null)"
        );
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

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=null, descriptionOrContent=1d6 ⇒ 1, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE, userReference=true, sendToOtherChannelId=null)"
        );
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

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=2d20+10 ⇒ 36, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED, userReference=true, sendToOtherChannelId=null)");
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

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("acknowledgeAndRemoveSlash",
                "sendMessage: EmbedOrMessageDefinition(title=2d20+10 ⇒ 36, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED, userReference=true, sendToOtherChannelId=null)");
    }


}
