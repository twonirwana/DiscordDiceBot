package de.janno.discord.api;

import de.janno.discord.api.Answer;
import reactor.core.publisher.Mono;

public interface IDiscordAdapter {
    Mono<Void> createResultMessageWithEventReference(Answer answer);
}
