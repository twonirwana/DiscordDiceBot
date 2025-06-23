package de.janno.discord.connector.api;

import java.util.Set;

public interface DatabaseConnector {
    void markDataOfMissingGuildsToDelete(Set<Long> allGuildIdsAtStartup);

    void markDataOfLeavingGuildsToDelete(long leavingGuildId);

    void unmarkDataOfJoiningGuilds(long joiningGuildId);

    void copyChildChannel(ChildrenChannelCreationEvent childrenChannelCreationEvent);
}
