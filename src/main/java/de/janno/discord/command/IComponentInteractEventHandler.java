package de.janno.discord.command;

import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface IComponentInteractEventHandler {

    Mono<Void> handleComponentInteractEvent(@NonNull ComponentInteractionEvent event);

    boolean matchingComponentCustomId(String buttonCustomId);
}
