package de.janno.discord.connector.api;

import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface DiscordAdapter {
    Long getGuildId();

    Mono<Void> reply(@NonNull String message);

    /**
     * returns the id of the delete message and will not delete pinned messages
     */
    Mono<Long> deleteMessage(long messageId, boolean deletePinned);


}
