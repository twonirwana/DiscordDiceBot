package de.janno.discord.connector;

import de.janno.discord.connector.api.IDiscordConnector;
import de.janno.discord.connector.api.ISlashCommand;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.jda.JdaClient;

import java.util.List;

public class DiscordConnector implements IDiscordConnector {
    public static void createAndStart(String token, boolean disableCommandUpdate, List<ISlashCommand> commands, MessageDefinition welcomeMessageDefinition) throws Exception {
        new DiscordConnector().start(token, disableCommandUpdate, commands, welcomeMessageDefinition);
    }

    @Override
    public void start(String token, boolean disableCommandUpdate, List<ISlashCommand> commands, MessageDefinition welcomeMessageDefinition) throws Exception {
        new JdaClient().start(token, disableCommandUpdate, commands, welcomeMessageDefinition);
    }
}
