package de.janno.discord.connector.api;

import lombok.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface DiscordConnector {
    void start(@NonNull List<SlashCommand> slashCommands,
               @NonNull List<ComponentCommand> componentCommands,
               @NonNull WelcomeMessageCreator welcomeMessageCreator,
               @NonNull Set<Long> allServerIdsInPersistence,
               @NonNull Consumer<ChildrenChannelCreationEvent> childrenChannelCreationEventConsumer) throws Exception;

}