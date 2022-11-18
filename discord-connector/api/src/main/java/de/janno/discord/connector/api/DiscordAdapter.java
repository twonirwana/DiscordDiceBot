package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface DiscordAdapter {
    Long getGuildId();

    @NonNull Mono<Void> reply(@NonNull String message, boolean ephemeral);

    @NonNull Mono<Void> deleteMessageById(long messageId);

    @NonNull Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition);

}
