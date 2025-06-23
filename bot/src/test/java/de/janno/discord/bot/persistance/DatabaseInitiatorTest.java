package de.janno.discord.bot.persistance;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseInitiatorTest {

    @AfterAll
    static void cleanup() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get("").toAbsolutePath())) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(DatabaseInitiator.BACKUP_FILE_PREFIX))
                    .toList();
            for (Path file : files) {
                Files.delete(file);
            }
        }
    }

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
                "messageConfig_deleteIndex",
                "config_delete");
    }

    @Test
    void applyWithBackupFile() throws IOException {
        String resourceName = "backup.zip";

        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        File backupFile = new File(resourceName);
        backupFile.deleteOnExit();
        FileUtils.copyURLToFile(resourceUrl, backupFile);

        DatabaseConnector databaseConnector = new DatabaseConnector("jdbc:h2:mem:" + UUID.randomUUID(), null, null);

        DatabaseInitiator.initialize(databaseConnector);

        List<String> res = DatabaseInitiator.getAlreadyAppliedMigrations(databaseConnector);

        assertThat(res).containsExactly(
                "base",
                "base", //twice because once from the base migration and once from the version.sql
                "configTable",
                "channelConfigUser",
                "aliasCommandIdCleanUp",
                "channelConfigFix",
                "configGuildNull",
                "configName",
                "messageData_delete",
                "messageConfig_deleteIndex",
                "config_delete");
    }

}