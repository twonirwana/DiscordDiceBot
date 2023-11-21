package de.janno.discord.bot.command.directRoll;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
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

public class HiddenRollCommandMockTest {
    PersistenceManager persistenceManager;

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
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=1d6 ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)"
        );

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=1d6 ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED), targetChannelId: null"
        );
    }

    @Test
    void roll_NoWarn() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("20")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=20 ⇒ 20, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)"
        );

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=20 ⇒ 20, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=EMBED), targetChannelId: null"
        );
    }


    @Test
    void help() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("help")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=null, descriptionOrContent=Type /h and a dice expression, configuration with /channel_config\n" +
                        DiceEvaluatorAdapter.getHelp() +
                        ", fields=[EmbedOrMessageDefinition.Field(name=Example, value=`/h expression:1d6`, inline=false), EmbedOrMessageDefinition.Field(name=Full documentation, value=https://github.com/twonirwana/DiscordDiceBot, inline=false), EmbedOrMessageDefinition.Field(name=Discord Server for Help and News, value=https://discord.gg/e43BsqKpFr, inline=false)], componentRowDefinitions=[], hasImage=false, type=EMBED)");
    }

    @Test
    void invalidExpression() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("d")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString\n" +
                        "The following expression is invalid: `d`. The error is: Operator d has right associativity but the right value was: empty. Use `/r expression:help` to get more information on how to use the command.");
    }

    @Test
    void roll_default_withLabel() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=test ⇒ 1, descriptionOrContent=1d6, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)"
        );

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=test ⇒ 1, descriptionOrContent=1d6, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED), targetChannelId: null"
        );
    }

    @Test
    void roll_config_full_imageNone() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
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

        assertThat(configCommandEvent.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=Roll ⇒ 1, descriptionOrContent=[1], fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)"
        );

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Roll ⇒ 1, descriptionOrContent=[1], fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=EMBED), targetChannelId: null"

        );
    }

    @Test
    void roll_config_withoutExpression() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
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

        assertThat(configCommandEvent.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=Roll ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)"
        );

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Roll ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED), targetChannelId: null"
        );
    }

    @Test
    void roll_config_withoutExpression_withLabel() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
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

        assertThat(configCommandEvent.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=test ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)");

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=test ⇒ 1, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED), targetChannelId: null"
        );
    }

    @Test
    void roll_config_compact() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
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

        assertThat(configCommandEvent.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**1d6 ⇒ 1**__  [1], fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)"
        );

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**1d6 ⇒ 1**__  [1], fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE), targetChannelId: null"
        );
    }

    @Test
    void roll_config_minimal() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
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

        assertThat(configCommandEvent.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=null, descriptionOrContent=1d6 ⇒ 1, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)"
        );

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=1d6 ⇒ 1, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE), targetChannelId: null"
        );
    }

    @Test
    void channelAlias() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("att")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(configCommandEvent.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=2d20+10 ⇒ 36, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)");

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=2d20+10 ⇒ 36, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED), targetChannelId: null"
        );
    }

    @Test
    void userChannelAlias() {
        HiddenDirectRollCommand underTest = new HiddenDirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock configCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(configCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock hiddenRollCommandEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("att")
                .build()));
        underTest.handleSlashCommandEvent(hiddenRollCommandEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(configCommandEvent.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved new alias");
        assertThat(hiddenRollCommandEvent.getActions()).containsExactlyInAnyOrder(
                "replyWithEmbedOrMessageDefinition: EmbedOrMessageDefinition(title=2d20+10 ⇒ 36, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED)",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Made a hidden roll, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)");

        List<EmbedOrMessageDefinition> replyMessages = hiddenRollCommandEvent.getAllReplays();
        assertThat(replyMessages).hasSize(1);
        EmbedOrMessageDefinition replayMessage = replyMessages.get(0);

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("h", "reveal", replayMessage);
        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveButtons",
                "getMessageDefinitionOfEventMessageWithoutButtons",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=2d20+10 ⇒ 36, descriptionOrContent=, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Reveal, id=hreveal, style=PRIMARY, disabled=false)])], hasImage=true, type=EMBED), targetChannelId: null"

        );
    }


}
