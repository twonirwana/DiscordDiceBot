package de.janno.discord.discord4j;

class ApplicationCommandHelperTest {
//todo
    /*
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
                                .value("v1")
                                .build())
                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name("c2")
                                .value("v2")
                                .build())
                        .build())
                .build();
        assertThat(commandDefinition).isEqualTo(ApplicationCommandHelper.applicationCommandData2CommandDefinition(applicationCommandData));

    }
*/
}