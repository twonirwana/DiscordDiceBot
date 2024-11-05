package de.janno.discord.connector;

import de.janno.discord.connector.api.ComponentCommand;
import de.janno.discord.connector.api.DiscordConnector;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.WelcomeMessageCreator;
import de.janno.discord.connector.jda.JdaClient;
import lombok.NonNull;

import java.util.List;
import java.util.Set;

public class DiscordConnectorImpl implements DiscordConnector {
    public static void createAndStart(
            @NonNull List<SlashCommand> slashCommands,
            @NonNull List<ComponentCommand> componentCommands,
            @NonNull WelcomeMessageCreator welcomeMessageCreator,
            @NonNull Set<Long> allGuildIdsInPersistence) throws Exception {
        new DiscordConnectorImpl().start(slashCommands, componentCommands, welcomeMessageCreator, allGuildIdsInPersistence);
    }

    @Override
    public void start(@NonNull List<SlashCommand> slashCommands,
                      @NonNull List<ComponentCommand> componentCommands,
                      @NonNull WelcomeMessageCreator welcomeMessageCreator,
                      @NonNull Set<Long> allGuildIdsInPersistence) throws Exception {
        new JdaClient(slashCommands, componentCommands, welcomeMessageCreator, allGuildIdsInPersistence);
    }
}
