package de.janno.discord.discord4j;

import de.janno.discord.command.slash.CommandDefinition;
import de.janno.discord.command.slash.CommandDefinitionOption;
import de.janno.discord.command.slash.CommandDefinitionOptionChoice;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationCommandHelperTest {

    @Test
    void notEqual_differentName() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .build();
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("b")
                .description("description")
                .build();

        assertThat(commandDefinition).isNotEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));
    }

    @Test
    void notEqual_differentDescription() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a")
                .description("description")
                .build();
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description2")
                .build();

        assertThat(commandDefinition).isNotEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));

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
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description")
                .build();

        assertThat(commandDefinition).isNotEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));

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
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(2)
                        .name("a2_o1")
                        .description("a2_o1")
                        .build())
                .build();

        assertThat(commandDefinition).isNotEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));

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
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(1)
                        .name("a2_o1")
                        .description("a2_o1")
                        .build())
                .build();

        assertThat(commandDefinition).isNotEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));
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
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(1)
                        .name("a2_o1")
                        .description("a2_o1")
                        .build())
                .build();

        assertThat(commandDefinition).isNotEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));
    }

    @Test
    void notEqual_DifferentOptionChoice() {
        CommandDefinition commandDefinition = CommandDefinition.builder()
                .name("a1_o1")
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

        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(1)
                        .name("a1_o1")
                        .description("a1_o1")
                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name("c1")
                                .value("v2")
                                .build())
                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name("c2")
                                .value("v1")
                                .build())
                        .build())
                .build();
        assertThat(commandDefinition).isNotEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));

    }

}