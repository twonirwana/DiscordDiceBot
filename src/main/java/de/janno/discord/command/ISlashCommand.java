package de.janno.discord.command;

import de.janno.discord.persistance.Trigger;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ISlashCommand {

    String getName();

    ApplicationCommandRequest getApplicationCommand();

    Mono<Trigger> handleSlashCommandEvent(@NonNull SlashCommandEvent event);

}
