package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface ButtonEventAdaptor extends DiscordAdapter {

    String getCustomId();

    long getMessageId();

    long getChannelId();

    long getUserId();

    boolean isPinned();

    String getInvokingGuildMemberName();

    /**
     * @param message can be null for no changes
     * @param componentRowDefinitions can be null for no changes
     */
    @NonNull Mono<Void> editMessage(String message, List<ComponentRowDefinition> componentRowDefinitions);

    @NonNull Requester getRequester();

    @NonNull Optional<String> checkPermissions(Long answerTargetChannelId, @NonNull Locale userLocale);

    @NonNull
    OffsetDateTime getMessageCreationTime();

    /**
     * acknowledge the event and delete original event
     */
    @NonNull Mono<Void> acknowledgeAndDeleteOriginal();

    @NonNull Mono<Void> acknowledge();

    /**
     * Gives the message definition of the message where the button was clicked
     */
    @NonNull EmbedOrMessageDefinition getMessageDefinitionOfEventMessageWithoutButtons();
}
