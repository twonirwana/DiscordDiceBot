package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public interface DiscordConnector {
    void start(String token,
               boolean disableCommandUpdate,
               List<SlashCommand> commands,
               Function<WelcomeRequest, EmbedOrMessageDefinition> welcomeMessageDefinition,
               Set<Long> allServerIdsInPersistence,
               String newsGuildId,
               String newsChannelId) throws Exception;

    record WelcomeRequest(long guildId, long channelId, Locale guildLocale) {
    }

}