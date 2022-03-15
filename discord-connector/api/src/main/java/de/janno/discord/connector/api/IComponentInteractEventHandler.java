package de.janno.discord.connector.api;

import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface IComponentInteractEventHandler {

    Mono<Void> handleComponentInteractEvent(@NonNull IButtonEventAdaptor event);

    boolean matchingComponentCustomId(String buttonCustomId);
}
