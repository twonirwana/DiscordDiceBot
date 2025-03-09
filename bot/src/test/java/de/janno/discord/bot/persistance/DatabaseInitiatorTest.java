package de.janno.discord.bot.persistance;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseInitiatorTest {

    @Test
    void applyAllMigrations() {
        DatabaseConnector databaseConnector = new DatabaseConnector("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        DatabaseInitiator.initialize(databaseConnector);

        List<String> res = DatabaseInitiator.getAlreadyAppliedMigrations(databaseConnector);

        assertThat(res).containsExactly(
                "base",
                "configTable",
                "channelConfigUser",
                "aliasCommandIdCleanUp",
                "channelConfigFix",
                "configGuildNull",
                "configName",
                "messageData_delete",
                "messageConfig_deleteIndex");
    }
}