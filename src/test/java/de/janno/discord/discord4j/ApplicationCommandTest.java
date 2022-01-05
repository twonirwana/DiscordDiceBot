package de.janno.discord.discord4j;

import com.google.common.collect.ImmutableList;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationCommandTest {

    @Test
    void notEqual_differentName() {
        ApplicationCommand applicationCommand = new ApplicationCommand("a", "description", ImmutableList.of());
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("b")
                .description("description")
                .build();

        assertThat(applicationCommand.equalToApplicationCommandData(applicationCommandData)).isFalse();
    }

    @Test
    void notEqual_differentDescription() {
        ApplicationCommand applicationCommand = new ApplicationCommand("a", "description", ImmutableList.of());
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description2")
                .build();

        assertThat(applicationCommand.equalToApplicationCommandData(applicationCommandData)).isFalse();

    }

    @Test
    void notEqual_moreOptions1() {
        ApplicationCommand applicationCommand = new ApplicationCommand("a", "description", ImmutableList.of(ApplicationCommandOptionData.builder()
                .type(1)
                .name("a2_o1")
                .description("a2_o1")
                .build()));
        ApplicationCommandData applicationCommandData = ApplicationCommandData.builder()
                .id("id")
                .applicationId("appId")
                .name("a")
                .description("description")
                .build();

        assertThat(applicationCommand.equalToApplicationCommandData(applicationCommandData)).isFalse();

    }

    @Test
    void notEqual_DifferentOptionType() {
        ApplicationCommand applicationCommand = new ApplicationCommand("a", "description", ImmutableList.of(ApplicationCommandOptionData.builder()
                .type(1)
                .name("a2_o1")
                .description("a2_o1")
                .build()));
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

        assertThat(applicationCommand.equalToApplicationCommandData(applicationCommandData)).isFalse();

    }

    @Test
    void notEqual_DifferentOptionName() {
        ApplicationCommand applicationCommand = new ApplicationCommand("a", "description", ImmutableList.of(ApplicationCommandOptionData.builder()
                .type(1)
                .name("a1_o1")
                .description("a2_o1")
                .build()));
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

        assertThat(applicationCommand.equalToApplicationCommandData(applicationCommandData)).isFalse();
    }

    @Test
    void notEqual_DifferentOptionDescription() {
        ApplicationCommand applicationCommand = new ApplicationCommand("a", "description", ImmutableList.of(ApplicationCommandOptionData.builder()
                .type(1)
                .name("a2_o1")
                .description("a1_o1")
                .build()));
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

        assertThat(applicationCommand.equalToApplicationCommandData(applicationCommandData)).isFalse();
    }

    @Test
    void notEqual_DifferentOptionChoice() {
        ApplicationCommand applicationCommand = new ApplicationCommand("a", "description", ImmutableList.of(ApplicationCommandOptionData.builder()
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
                .build()));
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

        assertThat(applicationCommand.equalToApplicationCommandData(applicationCommandData)).isFalse();
    }

}