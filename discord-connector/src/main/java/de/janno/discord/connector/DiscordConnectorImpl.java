package de.janno.discord.connector;

import de.janno.discord.connector.api.ComponentInteractEventHandler;
import de.janno.discord.connector.api.DiscordConnector;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.jda.JdaClient;
import lombok.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class DiscordConnectorImpl implements DiscordConnector {
    public static void createAndStart(
            @NonNull List<SlashCommand> slashCommands,
            @NonNull List<ComponentInteractEventHandler> componentInteractEventHandlers,
            @NonNull Function<WelcomeRequest, EmbedOrMessageDefinition> welcomeMessageDefinition,
            @NonNull Set<Long> allGuildIdsInPersistence
    ) throws Exception {
        new DiscordConnectorImpl().start(slashCommands, componentInteractEventHandlers, welcomeMessageDefinition, allGuildIdsInPersistence);
    }

    @Override
    public void start(@NonNull List<SlashCommand> slashCommands,
                      @NonNull List<ComponentInteractEventHandler> componentInteractEventHandlers,
                      @NonNull Function<WelcomeRequest, EmbedOrMessageDefinition> welcomeMessageDefinition,
                      @NonNull Set<Long> allGuildIdsInPersistence
    ) throws Exception {
        new JdaClient(slashCommands, componentInteractEventHandlers, welcomeMessageDefinition, allGuildIdsInPersistence);
    }
}
