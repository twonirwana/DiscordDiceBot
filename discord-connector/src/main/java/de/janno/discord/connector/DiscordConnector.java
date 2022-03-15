package de.janno.discord.connector;

import de.janno.discord.connector.api.IDiscordConnector;
import de.janno.discord.connector.api.ISlashCommand;
import de.janno.discord.connector.javacord.JavaCordClient;

import java.util.List;

public class DiscordConnector implements IDiscordConnector {
    public static void createAndStart(String token, boolean disableCommandUpdate, List<ISlashCommand> commands) {
        new DiscordConnector().start(token, disableCommandUpdate, commands);
    }

    @Override
    public void start(String token, boolean disableCommandUpdate, List<ISlashCommand> commands) {
        new JavaCordClient().start(token, disableCommandUpdate, commands);
    }
}
