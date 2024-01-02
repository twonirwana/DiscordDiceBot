package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public interface DiscordConnector {
    void start(@NonNull String token,
               boolean disableCommandUpdate,
               @NonNull List<SlashCommand> slashCommands,
               @NonNull List<ComponentInteractEventHandler> componentInteractEventHandlers,
               @NonNull Function<WelcomeRequest, EmbedOrMessageDefinition> welcomeMessageDefinition,
               @NonNull Set<Long> allServerIdsInPersistence,
               String newsGuildId,
               String newsChannelId) throws Exception;

    record WelcomeRequest(long guildId, long channelId, Locale guildLocale) {
    }

}