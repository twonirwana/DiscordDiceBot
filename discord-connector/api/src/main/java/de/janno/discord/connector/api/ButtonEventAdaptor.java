package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface ButtonEventAdaptor extends DiscordAdapter {

    String getCustomId();

    long getMessageId();

    long getChannelId();

    boolean isPinned();

    String getInvokingGuildMemberName();

    Mono<Void> editMessage(@Nullable String message, @Nullable List<ComponentRowDefinition> componentRowDefinitions);

    Mono<Long> createButtonMessage(MessageDefinition messageDefinition);

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

    Mono<Void> createResultMessageWithEventReference(EmbedDefinition answer, Long targetChannelId);

    @Value
    class LabelAndCustomId {
        @NonNull
        String label;
        @NonNull
        String customId;
    }
}
