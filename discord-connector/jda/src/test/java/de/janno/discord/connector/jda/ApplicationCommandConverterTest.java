package de.janno.discord.connector.jda;

import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationCommandConverterTest {

    @Test
    void notEqual_differentName() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .build();

        CommandData commandData = new CommandDataImpl("b", "description");

        assertThat(commandData)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2CommandData(commandDefinition));
    }

    @Test
    void notEqual_differentDescription() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .build();

        CommandData commandData = new CommandDataImpl("a", "description2");

        assertThat(commandData)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2CommandData(commandDefinition));

    }

    @Test
    void notEqual_moreOptions1() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .option(CommandDefinitionOption.builder()
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .name("a2_o1")
                        .description("a2_o1")
                        .build())
                .build();


        CommandData commandData = new CommandDataImpl("b", "description");


        assertThat(commandData)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2CommandData(commandDefinition));

    }

    @Test
    void notEqual_DifferentOptionType() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .option(CommandDefinitionOption.builder()
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .name("a2_o1")
                        .description("d2_o1")
                        .build())
                .build();

        CommandData commandData = new CommandDataImpl("a", "description")
                .addSubcommandGroups(new SubcommandGroupData("a2_o1", "d2_o1"));

        assertThat(commandData)
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2CommandData(commandDefinition));

    }

    @Test
    void notEqual_DifferentOptionName() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .option(CommandDefinitionOption.builder()
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .name("a1_o1")
                        .description("a2_o1")
                        .build())
                .build();


        CommandData commandData = new CommandDataImpl("a", "description")
                .addSubcommandGroups(new SubcommandGroupData("a2_o1", "a2_o1"));

        assertThat(commandData)
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2CommandData(commandDefinition));
    }

    @Test
    void notEqual_DifferentOptionDescription() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .option(CommandDefinitionOption.builder()
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .name("a2_o1")
                        .description("a1_o1")
                        .build())
                .build();

        CommandData commandData = new CommandDataImpl("a", "description")
                .addSubcommandGroups(new SubcommandGroupData("a2_o1", "a1_o1"));

        assertThat(commandData)
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2CommandData(commandDefinition));
    }

    @Test
    void equal() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .option(CommandDefinitionOption.builder()
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .name("a1_o1")
                        .description("a1_o1")
                        .option(CommandDefinitionOption.builder()
                                .type(CommandDefinitionOption.Type.STRING)
                                .name("c1")
                                .description("v1")
                                .build())
                        .option(CommandDefinitionOption.builder()
                                .type(CommandDefinitionOption.Type.STRING)
                                .name("c2")
                                .description("v2")
                                .build())
                        .build())
                .build();

        CommandData commandData = new CommandDataImpl("a", "description")
                .addSubcommands(new SubcommandData("a1_o1", "a1_o1")
                        .addOption(OptionType.STRING, "c1", "v1")
                        .addOption(OptionType.STRING, "c2", "v2")
                );
        assertThat(commandData)
                .usingRecursiveComparison()
                .isEqualTo(ApplicationCommandConverter.commandDefinition2CommandData(commandDefinition));

    }
}