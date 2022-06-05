package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface IButtonEventAdaptor extends IDiscordAdapter {

    String getCustomId();

    long getMessageId();

    long getChannelId();

    boolean isPinned();

    String getInvokingGuildMemberName();

    Mono<Void> acknowledge();

    Mono<Void> editMessage(@Nullable String message,@Nullable List<ComponentRowDefinition> componentRowDefinitions);

    Mono<Long> createButtonMessage(MessageDefinition messageDefinition);

    Mono<Void> deleteMessage(long messageId);

    List<LabelAndCustomId> getAllButtonIds();

    String getMessageContent();

    Mono<Requester> getRequester();

    Optional<String> checkPermissions();

    @Value
    class LabelAndCustomId {
        @NonNull
        String label;
        @NonNull
        String customId;
    }
}
