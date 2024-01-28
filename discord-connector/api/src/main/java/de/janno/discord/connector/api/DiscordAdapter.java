package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import java.util.Collection;

public interface DiscordAdapter {
    Long getGuildId();

    @NonNull Mono<Void> reply(@NonNull String message, boolean ephemeral);

    @NonNull Mono<Void> deleteMessageById(long messageId);

    @NonNull Mono<Long> createMessageWithoutReference(@NonNull EmbedOrMessageDefinition messageDefinition);

    @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds);

}
