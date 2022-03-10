package de.janno.discord.connector.api;

import reactor.core.publisher.Mono;

public interface IDiscordAdapter {
    Mono<Void> createResultMessageWithEventReference(Answer answer);
}
