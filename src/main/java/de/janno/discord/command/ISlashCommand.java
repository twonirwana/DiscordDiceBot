package de.janno.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ISlashCommand {

    String getName();

    ApplicationCommandRequest getApplicationCommand();

    Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event);


}
