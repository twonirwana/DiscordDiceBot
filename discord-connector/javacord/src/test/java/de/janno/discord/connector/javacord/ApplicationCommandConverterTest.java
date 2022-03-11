package de.janno.discord.connector.javacord;

import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionChoiceBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.javacord.api.interaction.SlashCommandOptionType.SUB_COMMAND_GROUP;

class ApplicationCommandConverterTest {

    @Test
    void notEqual_differentName() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .build();

        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("b")
                .setDescription("description");

        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));
    }

    @Test
    void notEqual_differentDescription() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .build();
        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("a")
                .setDescription("description2");

        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));

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
        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("a")
                .setDescription("description");

        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));

    }

    @Test
    void notEqual_DifferentOptionType() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .option(CommandDefinitionOption.builder()
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .name("a2_o1")
                        .description("a2_o1")
                        .build())
                .build();
        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("a")
                .setDescription("description")
                .addOption(new SlashCommandOptionBuilder()
                        .setType(SUB_COMMAND_GROUP)
                        .setName("a2_o1")
                        .setDescription("a2_o1")
                        .build());

        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));

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
        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("a")
                .setDescription("description")
                .addOption(new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("a2_o1")
                        .setDescription("a2_o1")
                        .build());

        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));
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

        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("a")
                .setDescription("description")
                .addOption(new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("a2_o1")
                        .setDescription("a2_o1")
                        .build());

        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));
    }

    @Test
    void notEqual_DifferentOptionChoice() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .option(CommandDefinitionOption.builder()
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .name("a1_o1")
                        .description("a1_o1")
                        .choice(CommandDefinitionOptionChoice.builder()
                                .name("c1")
                                .value("v1")
                                .build())
                        .choice(CommandDefinitionOptionChoice.builder()
                                .name("c2")
                                .value("v2")
                                .build())
                        .build())
                .build();

        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("a")
                .setDescription("description")
                .addOption(new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("a2_o1")
                        .setDescription("a2_o1")
                        .addChoice(new SlashCommandOptionChoiceBuilder()
                                .setName("c1")
                                .setValue("v2")
                                .build())
                        .addChoice(new SlashCommandOptionChoiceBuilder()
                                .setName("c2")
                                .setValue("v1")
                                .build())
                        .build());

        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isNotEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));

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
                        .choice(CommandDefinitionOptionChoice.builder()
                                .name("c1")
                                .value("v1")
                                .build())
                        .choice(CommandDefinitionOptionChoice.builder()
                                .name("c2")
                                .value("v2")
                                .build())
                        .build())
                .build();

        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
                .setName("a")
                .setDescription("description")
                .addOption(new SlashCommandOptionBuilder()
                        .setType(SlashCommandOptionType.SUB_COMMAND)
                        .setName("a1_o1")
                        .setDescription("a1_o1")
                        .addChoice(new SlashCommandOptionChoiceBuilder()
                                .setName("c1")
                                .setValue("v1")
                                .build())
                        .addChoice(new SlashCommandOptionChoiceBuilder()
                                .setName("c2")
                                .setValue("v2")
                                .build())
                        .build());
        assertThat(slashCommandBuilder)
                .usingRecursiveComparison()
                .isEqualTo(ApplicationCommandConverter.commandDefinition2SlashCommandBuilder(commandDefinition));

    }
}