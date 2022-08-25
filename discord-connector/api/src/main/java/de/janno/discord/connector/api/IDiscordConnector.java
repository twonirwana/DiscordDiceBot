package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.MessageDefinition;

import java.util.List;

public interface IDiscordConnector {

    void start(String token, boolean disableCommandUpdate, List<ISlashCommand> commands, MessageDefinition welcomeMessageDefinition) throws Exception;
}