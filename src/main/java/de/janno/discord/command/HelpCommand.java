package de.janno.discord.command;

import de.janno.discord.Metrics;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
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
                .description("Link to the documentation")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event) {
        Metrics.incrementMetricCounter(getName(), "slashEvent", null);
        return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                .ephemeral(true)
                .content("Full documentation can be found under: https://github.com/twonirwana/DiscordDiceBot/blob/main/README.md")
                .build());
    }
}
