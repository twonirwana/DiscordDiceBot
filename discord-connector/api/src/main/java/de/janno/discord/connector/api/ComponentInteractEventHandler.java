package de.janno.discord.connector.api;

import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Objects;

public interface ComponentInteractEventHandler {

    Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event);

    default boolean matchingComponentCustomId(String buttonCustomId) {
        if (!BottomCustomIdUtils.isValidCustomId(buttonCustomId)) {
            return false;
        }
        return Objects.equals(getCommandId(), BottomCustomIdUtils.getCommandNameFromCustomId(buttonCustomId));
    }

    @NonNull String getCommandId();
}
