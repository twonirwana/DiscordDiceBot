package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.MessageDefinition;

import java.util.List;
import java.util.Set;

public interface DiscordConnector {
    void start(String token,
               boolean disableCommandUpdate,
               List<SlashCommand> commands,
               MessageDefinition welcomeMessageDefinition,
               Set<Long> allServerIdsInPersistence) throws Exception;
}