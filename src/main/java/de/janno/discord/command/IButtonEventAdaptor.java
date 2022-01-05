package de.janno.discord.command;

import discord4j.core.object.component.LayoutComponent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IButtonEventAdaptor extends IDiscordAdapter {

    String getCustomId();

    Long getMessageId();

    Long getChannelId();

    boolean isPinned();

    Mono<Void> editMessage(String message);

    Mono<Long> createButtonMessage(String messageContent, List<LayoutComponent> buttonLayout);

    Mono<Void> deleteMessage(long messageId);

    List<String> getAllButtonIds();

    String getMessageContent();
}
