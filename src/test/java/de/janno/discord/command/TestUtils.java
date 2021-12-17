package de.janno.discord.command;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.InteractionData;

public class TestUtils {
    public static ComponentInteractionEvent createEventWithCustomId(GatewayDiscordClient gatewayDiscordClient, String commandName, String buttonValue, String... config) {
        InteractionData data = InteractionData.builder()
                .id("id")
                .applicationId("1")
                .type(0)
                .token("token")
                .version(1)
                .data(ApplicationCommandInteractionData.builder()
                        .customId(String.join(",", commandName, buttonValue, String.join(",", config)))
                        .build())
                .build();
        return new ComponentInteractionEvent(gatewayDiscordClient, null, new Interaction(gatewayDiscordClient, data));
    }
}
