package de.janno.discord.connector.api;

import java.util.List;

public interface IDiscordConnector {

    void start(String token, boolean disableCommandUpdate, List<ISlashCommand> commands);
}