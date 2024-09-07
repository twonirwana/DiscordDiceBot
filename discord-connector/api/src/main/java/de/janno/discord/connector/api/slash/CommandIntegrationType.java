package de.janno.discord.connector.api.slash;

import java.util.Set;

public enum CommandIntegrationType {
    GUILD_INSTALL,
    USER_INSTALL;

    public static Set<CommandIntegrationType> ALL = Set.of(CommandIntegrationType.values());
}
