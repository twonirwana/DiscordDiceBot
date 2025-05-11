package de.janno.discord.connector;

import de.janno.discord.connector.api.*;
import de.janno.discord.connector.jda.JdaClient;
import lombok.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class DiscordConnectorImpl implements DiscordConnector {
    public static void createAndStart(
            @NonNull List<SlashCommand> slashCommands,
            @NonNull List<ComponentCommand> componentCommands,
            @NonNull WelcomeMessageCreator welcomeMessageCreator,
            @NonNull Set<Long> allGuildIdsInPersistence,
            @NonNull Consumer<ChildrenChannelCreationEvent> childrenChannelCreationEventConsumer) throws Exception {
        new DiscordConnectorImpl().start(slashCommands, componentCommands, welcomeMessageCreator, allGuildIdsInPersistence, childrenChannelCreationEventConsumer);
    }

    @Override
    public void start(@NonNull List<SlashCommand> slashCommands,
                      @NonNull List<ComponentCommand> componentCommands,
                      @NonNull WelcomeMessageCreator welcomeMessageCreator,
                      @NonNull Set<Long> allGuildIdsInPersistence,
                      @NonNull Consumer<ChildrenChannelCreationEvent> childrenChannelCreationEventConsumer) throws Exception {
        new JdaClient(slashCommands, componentCommands, welcomeMessageCreator, allGuildIdsInPersistence, childrenChannelCreationEventConsumer);
    }
}
