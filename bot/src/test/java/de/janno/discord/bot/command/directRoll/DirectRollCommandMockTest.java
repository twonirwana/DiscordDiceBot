package de.janno.discord.bot.command.directRoll;

import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistanceManager;
import de.janno.discord.bot.persistance.PersistanceManagerImpl;
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

public class DirectRollCommandMockTest {
    PersistanceManager persistanceManager;

    @BeforeEach
    void setup() {
        try {
            FileUtils.cleanDirectory(new File("imageCache/"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        persistanceManager = new PersistanceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_default() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=1d6 ⇒ 3, descriptionOrContent=, fields=[], file=86da4f6e0c1e3d159e92de31ff146325f75ca17052630c1f619276947307302c.png, type=EMBED)");
    }

    @Test
    void help() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("help")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "replyEmbed: EmbedOrMessageDefinition(title=null, descriptionOrContent=Type /r and a dice expression, configuration with /direct_roll_config\n" +
                        DiceEvaluatorAdapter.getHelp() +
                        ", fields=[EmbedOrMessageDefinition.Field(name=Example, value=`/r expression:1d6`, inline=false), EmbedOrMessageDefinition.Field(name=Full documentation, value=https://github.com/twonirwana/DiscordDiceBot, inline=false), EmbedOrMessageDefinition.Field(name=Discord Server for Help and News, value=https://discord.gg/e43BsqKpFr, inline=false)], file=null, type=EMBED)");
    }

    @Test
    void invalidExpression() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("d")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString\n" +
                        "The following expression is invalid: 'd'. The error is: Operator d has right associativity but the right value was: empty. Use `/r expression:help` to get more information on how to use the command.");
    }

    @Test
    void roll_default_withLabel() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent).block();


        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=test ⇒ 3, descriptionOrContent=1d6, fields=[], file=86da4f6e0c1e3d159e92de31ff146325f75ca17052630c1f619276947307302c.png, type=EMBED)");
    }

    @Test
    void roll_config_full_imageNone() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);
        DirectRollConfigCommand directRollConfig = new DirectRollConfigCommand(persistanceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save")
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
                        .stringValue("none")
                        .build())
                .build()));
        directRollConfig.handleSlashCommandEvent(slashEvent1).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=Roll ⇒ 3, descriptionOrContent=[3], fields=[], file=null, type=EMBED)");
    }

    @Test
    void roll_config_withoutExpression() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);
        DirectRollConfigCommand directRollConfig = new DirectRollConfigCommand(persistanceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save")
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
        directRollConfig.handleSlashCommandEvent(slashEvent1).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=Roll ⇒ 3, descriptionOrContent=, fields=[], file=86da4f6e0c1e3d159e92de31ff146325f75ca17052630c1f619276947307302c.png, type=EMBED)");
    }

    @Test
    void roll_config_withoutExpression_withLabel() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);
        DirectRollConfigCommand directRollConfig = new DirectRollConfigCommand(persistanceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save")
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
        directRollConfig.handleSlashCommandEvent(slashEvent1).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@test")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=test ⇒ 3, descriptionOrContent=, fields=[], file=86da4f6e0c1e3d159e92de31ff146325f75ca17052630c1f619276947307302c.png, type=EMBED)");
    }

    @Test
    void roll_config_compact() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);
        DirectRollConfigCommand directRollConfig = new DirectRollConfigCommand(persistanceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save")
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
        directRollConfig.handleSlashCommandEvent(slashEvent1).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**1d6 ⇒ 3**__  [3], fields=[], file=null, type=MESSAGE)");
    }

    @Test
    void roll_config_minimal() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);
        DirectRollConfigCommand directRollConfig = new DirectRollConfigCommand(persistanceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save")
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
        directRollConfig.handleSlashCommandEvent(slashEvent1).block();


        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=1d6 ⇒ 3, fields=[], file=null, type=MESSAGE)");
    }

    @Test
    void saveConfigDelete_default() {
        DirectRollCommand directRollCommand = new DirectRollCommand(new RandomNumberSupplier(0), persistanceManager);
        DirectRollConfigCommand directRollConfig = new DirectRollConfigCommand(persistanceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save")
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
        directRollConfig.handleSlashCommandEvent(slashEvent1).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("delete")
                .build()));
        directRollConfig.handleSlashCommandEvent(slashEvent2).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent3).block();

        assertThat(slashEvent1.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nSaved direct roll channel config");
        assertThat(slashEvent2.getActions()).containsExactlyInAnyOrder("reply: `commandString`\nDeleted direct roll channel config");
        assertThat(slashEvent3.getActions()).containsExactlyInAnyOrder(
                "acknowledgeAndRemoveSlash",
                "createResultMessageWithEventReference: EmbedOrMessageDefinition(title=1d6 ⇒ 3, descriptionOrContent=, fields=[], file=86da4f6e0c1e3d159e92de31ff146325f75ca17052630c1f619276947307302c.png, type=EMBED)");
    }

}
