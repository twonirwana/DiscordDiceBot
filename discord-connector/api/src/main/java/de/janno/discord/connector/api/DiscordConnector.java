package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;

import java.util.List;
import java.util.Set;

public interface DiscordConnector {
    void start(String token,
               boolean disableCommandUpdate,
               List<SlashCommand> commands,
               EmbedOrMessageDefinition welcomeMessageDefinition,
               Set<Long> allServerIdsInPersistence) throws Exception;
}