package de.janno.discord.connector.api;

import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ComponentInteractEventHandler {

    Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event);

    boolean matchingComponentCustomId(String buttonCustomId);
}
