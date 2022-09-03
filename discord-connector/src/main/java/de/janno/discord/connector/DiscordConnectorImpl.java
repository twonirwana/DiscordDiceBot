package de.janno.discord.connector;

import de.janno.discord.connector.api.DiscordConnector;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.jda.JdaClient;

import java.util.List;

public class DiscordConnectorImpl implements DiscordConnector {
    public static void createAndStart(String token, boolean disableCommandUpdate, List<SlashCommand> commands, MessageDefinition welcomeMessageDefinition) throws Exception {
        new DiscordConnectorImpl().start(token, disableCommandUpdate, commands, welcomeMessageDefinition);
    }

    @Override
    public void start(String token, boolean disableCommandUpdate, List<SlashCommand> commands, MessageDefinition welcomeMessageDefinition) throws Exception {
        new JdaClient().start(token, disableCommandUpdate, commands, welcomeMessageDefinition);
    }
}
