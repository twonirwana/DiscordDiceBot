package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface DiscordAdapter {
    Long getGuildId();

    @NonNull Mono<Void> reply(@NonNull String message, boolean ephemeral);

    @NonNull Flux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds);

    @NonNull Mono<Void> deleteMessageById(long messageId);

    @NonNull Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition);

}
