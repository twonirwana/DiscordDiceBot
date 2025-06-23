package de.janno.discord.connector.api;

import lombok.NonNull;

import java.util.List;

public interface DiscordConnector {
    void start(@NonNull List<SlashCommand> slashCommands,
               @NonNull List<ComponentCommand> componentCommands,
               @NonNull WelcomeMessageCreator welcomeMessageCreator,
               @NonNull DatabaseConnector databaseConnector) throws Exception;

}