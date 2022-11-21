package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ButtonEventAdaptor extends DiscordAdapter {

    String getCustomId();

    long getMessageId();

    long getChannelId();

    boolean isPinned();

    String getInvokingGuildMemberName();

    Mono<Void> editMessage(@Nullable String message, @Nullable List<ComponentRowDefinition> componentRowDefinitions);

    /**
     * will be removed when almost all users have switched to the persisted button id
     */
    List<LabelAndCustomId> getAllButtonIds();

    /**
     * will be removed when almost all users have switched to the persisted button id
     */
    String getMessageContent();

    Requester getRequester();

    Optional<String> checkPermissions(Long answerTargetChannelId);

    Mono<Void> createResultMessageWithEventReference(EmbedOrMessageDefinition answer, Long targetChannelId);

    @NonNull Flux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds);

    @NonNull OffsetDateTime getMessageCreationTime();
    @NonNull OffsetDateTime getEventCreationTime();

    @Value
    class LabelAndCustomId {
        @NonNull
        String label;
        @NonNull
        String customId;
    }
}
