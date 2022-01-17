package de.janno.discord.command;

import reactor.core.publisher.Mono;

public interface IDiscordAdapter {
    Mono<Void> createResultMessageWithEventReference(Answer answer);
}
