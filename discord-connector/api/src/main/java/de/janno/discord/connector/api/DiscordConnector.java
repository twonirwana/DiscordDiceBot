package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.MessageDefinition;

import java.util.List;

public interface DiscordConnector {

    void start(String token, boolean disableCommandUpdate, List<SlashCommand> commands, MessageDefinition welcomeMessageDefinition) throws Exception;
}