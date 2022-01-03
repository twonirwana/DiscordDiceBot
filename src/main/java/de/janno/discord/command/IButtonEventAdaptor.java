package de.janno.discord.command;

import de.janno.discord.cache.ActiveButtonsCache;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.LayoutComponent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IButtonEventAdaptor extends IDiscordAdapter {
    String getCustomId();

    Snowflake getMessageId();

    Snowflake getChannelId();

    boolean isPinned();

    Mono<Void> editMessage(String message);

    Mono<Void> moveButtonMessage(boolean triggeringMessageIsPinned,
                                 String editMessageText,
                                 String buttonMessageText,
                                 ActiveButtonsCache activeButtonsCache,
                                 List<LayoutComponent> buttonLayout,
                                 List<String> config);

    List<String> getAllButtonIds();

    String getMessageContent();
}
