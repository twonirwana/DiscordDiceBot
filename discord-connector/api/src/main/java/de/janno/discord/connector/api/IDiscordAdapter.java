package de.janno.discord.connector.api;

import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface IDiscordAdapter {

    Mono<Void> reply(@NonNull String message);

}
