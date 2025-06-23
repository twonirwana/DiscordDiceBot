package de.janno.discord.connector;

import de.janno.discord.connector.api.*;
import de.janno.discord.connector.jda.JdaClient;
import lombok.NonNull;

import java.util.List;

public class DiscordConnectorImpl implements DiscordConnector {
    public static void createAndStart(
            @NonNull List<SlashCommand> slashCommands,
            @NonNull List<ComponentCommand> componentCommands,
            @NonNull WelcomeMessageCreator welcomeMessageCreator,
            @NonNull DatabaseConnector databaseConnector) throws Exception {
        new DiscordConnectorImpl().start(slashCommands, componentCommands, welcomeMessageCreator, databaseConnector);
    }

    @Override
    public void start(@NonNull List<SlashCommand> slashCommands,
                      @NonNull List<ComponentCommand> componentCommands,
                      @NonNull WelcomeMessageCreator welcomeMessageCreator,
                      @NonNull DatabaseConnector databaseConnector) throws Exception {
        new JdaClient(slashCommands, componentCommands, welcomeMessageCreator, databaseConnector);
    }
}
