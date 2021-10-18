package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.Metrics;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class HelpCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public ApplicationCommandRequest getApplicationCommand() {
        return ApplicationCommandRequest.builder()
                .name(getName())
                .description("Help to the commands and links for further information")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event) {
        Metrics.incrementSlashMetricCounter(getName(), ImmutableList.of());
        return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                .ephemeral(true)
                .addEmbed(EmbedCreateSpec.builder()
                        .addField("Command help", "type '/count_successes help', '/custom_dice help' or '/fate help' to get help for the commands", false)
                        .addField("Full documentation", "https://github.com/twonirwana/DiscordDiceBot/blob/main/README.md", false)
                        .addField("Discord Server", "https://discord.gg/e43BsqKpFr", false)
                        .build())
                .build());
    }
}
