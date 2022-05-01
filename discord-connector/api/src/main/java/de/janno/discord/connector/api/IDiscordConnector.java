package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.MessageDefinition;

import javax.security.auth.login.LoginException;
import java.util.List;

public interface IDiscordConnector {

    void start(String token, boolean disableCommandUpdate, List<ISlashCommand> commands, MessageDefinition welcomeMessageDefinition) throws Exception;
}