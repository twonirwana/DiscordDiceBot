package de.janno.discord.command;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.InteractionData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserData;

public class TestUtils {
    public static ComponentInteractionEvent createEventWithCustomId(GatewayDiscordClient gatewayDiscordClient, String commandName, String messageText, String buttonValue, String... config) {
        InteractionData data = InteractionData.builder()
                .id("id")
                .applicationId("1")
                .type(0)
                .token("token")
                .version(1)
                .message(MessageData.builder()
                        .author(UserData.builder()
                                .id("1")
                                .username("testUser")
                                .discriminator("discriminator")
                                .build())
                        .id("1")
                        .timestamp("10")
                        .tts(false)
                        .mentionEveryone(false)
                        .pinned(false)
                        .type(1)
                        .content(messageText)
                        .channelId("1")
                        .build())
                .data(ApplicationCommandInteractionData.builder()
                        .customId(String.join(",", commandName, buttonValue, String.join(",", config)))
                        .build())
                .build();
        return new ComponentInteractionEvent(gatewayDiscordClient, null, new Interaction(gatewayDiscordClient, data));
    }
}
