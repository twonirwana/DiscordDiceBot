package de.janno.discord.command;

import de.janno.discord.cache.ButtonMessageCache;
import discord4j.core.object.component.LayoutComponent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IButtonEventAdaptor extends IDiscordAdapter {

    String getCustomId();

    Long getMessageId();

    Long getChannelId();

    boolean isPinned();

    Mono<Void> editMessage(String message);

    Mono<Long> createButtonMessage(String messageContent,
                                   ButtonMessageCache buttonMessageCache,
                                   List<LayoutComponent> buttonLayout,
                                   int configHash);

    Mono<Void> deleteMessage(Mono<Long> messageId,
                             ButtonMessageCache buttonMessageCache,
                             int configHash);

    List<String> getAllButtonIds();

    String getMessageContent();
}
