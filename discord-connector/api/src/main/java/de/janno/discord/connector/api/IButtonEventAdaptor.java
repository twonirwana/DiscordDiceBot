package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IButtonEventAdaptor extends IDiscordAdapter {

    String getCustomId();

    long getMessageId();

    long getChannelId();

    boolean isPinned();

    String getInvokingGuildMemberName();

    Mono<Void> acknowledge();

    Mono<Void> editMessage(String message);

    Mono<Long> createButtonMessage(MessageDefinition messageDefinition);

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
