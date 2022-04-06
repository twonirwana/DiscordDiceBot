package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedDefinition;
import reactor.core.publisher.Mono;

public interface IDiscordAdapter {
    Mono<Void> createResultMessageWithEventReference(EmbedDefinition answer);
}
