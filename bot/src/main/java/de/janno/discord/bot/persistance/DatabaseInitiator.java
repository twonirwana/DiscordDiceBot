package de.janno.discord.bot.persistance;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.*;

@Slf4j
public class DatabaseInitiator {

    public static final String BACKUP_FILE_PREFIX = "applied_backup_";
    private final static List<String> MIGRATION_FILES = ImmutableList.<String>builder()
            .add("1_base.sql")
            .add("2_configTable.sql")
            .add("3_channelConfigUser.sql")
            .add("4_aliasCommandIdCleanUp.sql")
            .add("5_channelConfigFix.sql")
            .add("6_configGuildNull.sql")
            .add("7_configName.sql")
            .add("8_messageData_delete.sql")
            .add("9_messageConfig_deleteIndex.sql")
            .add("10_config_delete.sql")
            .build();
    private final static String BACKUP_FILE_NAME = "backup.zip";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral('-')
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral('-')
            .appendValue(SECOND_OF_MINUTE, 2)
            .toFormatter();

    public static void initialize(@NonNull DatabaseConnector databaseConnector) {
        applyBackupFile(databaseConnector);
        List<Migration> allMigrations = readMigrations();
        log.info("found migrations: {}", allMigrations.stream().map(Migration::getName).collect(Collectors.joining(", ")));
        List<String> alreadyAppliedMigrations = getAlreadyAppliedMigrations(databaseConnector);
        log.info("db migration status: {}", alreadyAppliedMigrations);
        applyMissingMigrations(allMigrations, alreadyAppliedMigrations, databaseConnector);
    }

    private static void applyBackupFile(DatabaseConnector databaseConnector) {
        final Path path = Path.of(BACKUP_FILE_NAME);
        if (Files.exists(path)) {
            log.info("Start importing backup");
            try (ZipFile zipFile = new ZipFile(BACKUP_FILE_NAME)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String backup = IOUtils.toString(zipFile.getInputStream(entry), StandardCharsets.UTF_8);
                    log.info("Finished loading backup script: " + entry.getName());
                    try (Connection connection = databaseConnector.getConnection()) {
                        Statement statement = connection.createStatement();
                        statement.execute(backup);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                String backupMoveName = BACKUP_FILE_PREFIX + LocalDateTime.now().format(DATE_TIME_FORMATTER) + ".zip";
                Files.move(path, Path.of(backupMoveName));
                log.info("Finished importing backup and moved to {}", backupMoveName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void applyMissingMigrations(List<Migration> allMigrations,
                                               List<String> alreadyAppliedMigrations,
                                               DatabaseConnector databaseConnector) {
        Preconditions.checkArgument(allMigrations.stream().map(Migration::getOrder).distinct().count() == allMigrations.size(), "Duplicate migration order");
        Preconditions.checkArgument(allMigrations.stream().map(Migration::getName).distinct().count() == allMigrations.size(), "Duplicate migration name");

        allMigrations.stream()
                .filter(m -> !alreadyAppliedMigrations.contains(m.getName()))
                .sorted(Comparator.comparing(Migration::getOrder))
                .forEach(m -> {
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    log.info("Start executing {}", m.getName());
                    try (Connection connection = databaseConnector.getConnection()) {
                        Statement statement = connection.createStatement();
                        statement.execute(m.getSql());
                        try (PreparedStatement preparedStatement =
                                     connection.prepareStatement("""
                                             INSERT INTO DB_VERSION(MIGRATION_NAME, CREATION_DATE) SELECT ?, ? WHERE NOT EXISTS (
                                                 SELECT 1
                                                 FROM DB_VERSION
                                                 WHERE MIGRATION_NAME = ?
                                             );
                                             """)) {
                            preparedStatement.setString(1, m.getName());
                            preparedStatement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                            preparedStatement.setString(3, m.getName());
                            preparedStatement.execute();
                        }
                    } catch (SQLException e) {
                        log.error("Error in {}", m.getName());
                        throw new RuntimeException(e);
                    }
                    log.info("Finish executing {} in {}ms", m.getName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
                });
    }

    @VisibleForTesting
    static List<String> getAlreadyAppliedMigrations(DatabaseConnector databaseConnector) {
        try (Connection connection = databaseConnector.getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select MIGRATION_NAME from DB_VERSION");
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            while (resultSet.next()) {
                builder.add(resultSet.getString("MIGRATION_NAME"));
            }
            return builder.build();
        } catch (SQLException e) {
            return List.of();
        }
    }

    private static List<Migration> readMigrations() {
        return MIGRATION_FILES.stream()
                .map(DatabaseInitiator::readMigration)
                .toList();
    }

    private static Migration readMigration(final String fileName) {
        Preconditions.checkArgument(fileName.contains("_"), "Wrong file format: {}", fileName);
        Preconditions.checkArgument(fileName.endsWith("sql"), "Wrong file format: {}", fileName);
        String content;
        try {
            content = IOUtils.toString(Resources.getResource("db/migrations/" + fileName).openStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int firstUnderscoreIndex = fileName.indexOf("_");
        long order = Long.parseLong(fileName.substring(0, firstUnderscoreIndex));
        String name = fileName.substring(firstUnderscoreIndex + 1, fileName.length() - 4);
        return new Migration(order, name, content);
    }


    @Value
    private static class Migration {
        long order;
        String name;
        String sql;
    }
}
