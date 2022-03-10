package de.janno.discord.connector.api;

import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IButtonEventAdaptor extends IDiscordAdapter {

    String getCustomId();

    Long getMessageId();

    Long getChannelId();

    boolean isPinned();

    String getInvokingGuildMemberName();

    Mono<Void> editMessage(String message);

    Mono<Long> createButtonMessage(String messageContent, List<ComponentRowDefinition> buttonLayout);

    Mono<Void> deleteMessage(long messageId);

    List<LabelAndCustomId> getAllButtonIds();

    String getMessageContent();

    Mono<Requester> getRequester();

    @Value
    class LabelAndCustomId {
        @NonNull
        String label;
        @NonNull
        String customId;
    }
}
