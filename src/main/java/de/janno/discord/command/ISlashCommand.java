package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ISlashCommand {

    String getName();

    ApplicationCommandRequest getApplicationCommand();

    Mono<Void> handleSlashCommandEvent(@NonNull SlashCommandEvent event);


}
