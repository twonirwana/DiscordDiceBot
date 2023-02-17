package de.janno.discord.bot.persistance;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DatabaseInitiator {

    public static void initialize(@NonNull String url, @Nullable String user, @Nullable String password) {
        JdbcConnectionPool connectionPool = JdbcConnectionPool.create(url, user, password);

        List<Migration> allMigrations = readMigrations();
        log.info("found migrations: {}", allMigrations.stream().map(Migration::getName).collect(Collectors.joining(", ")));
        List<String> alreadyAppliedMigrations = getAlreadyAppliedMigrations(connectionPool);
        log.info("db migration status: {}", alreadyAppliedMigrations);
        applyMissingMigrations(allMigrations, alreadyAppliedMigrations, connectionPool);
    }

    private static void applyMissingMigrations(List<Migration> allMigrations, List<String> alreadyAppliedMigrations, JdbcConnectionPool connectionPool) {
        Preconditions.checkArgument(allMigrations.stream().map(Migration::getOrder).distinct().count() == allMigrations.size(), "Duplicate migration order");
        Preconditions.checkArgument(allMigrations.stream().map(Migration::getName).distinct().count() == allMigrations.size(), "Duplicate migration name");

        allMigrations.stream()
                .filter(m -> !alreadyAppliedMigrations.contains(m.getName()))
                .sorted(Comparator.comparing(Migration::getOrder))
                .forEach(m -> {
                    log.info("Start executing {}", m.getName());
                    try (Connection connection = connectionPool.getConnection()) {
                        Statement statement = connection.createStatement();
                        statement.execute(m.getSql());
                        try (PreparedStatement preparedStatement =
                                     connection.prepareStatement("INSERT INTO DB_VERSION(MIGRATION_NAME, CREATION_DATE) VALUES (?, ?)")) {
                            preparedStatement.setString(1, m.getName());
                            preparedStatement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                            preparedStatement.execute();
                        }
                    } catch (SQLException e) {
                        log.error("Error in {}", m.getName());
                        throw new RuntimeException(e);
                    }
                    log.info("Finish executing {}", m.getName());
                });
    }

    private static List<String> getAlreadyAppliedMigrations(JdbcConnectionPool connectionPool) {
        try (Connection connection = connectionPool.getConnection()) {
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
        try (Stream<Path> stream = Files.list(Paths.get(Resources.getResource("db/migrations").toURI()))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(DatabaseInitiator::readMigration)
                    .toList();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Migration readMigration(Path filePath) {
        Preconditions.checkArgument(filePath.getFileName().toString().contains("_"), "Wrong file format: {}", filePath);
        Preconditions.checkArgument(filePath.getFileName().toString().endsWith("sql"), "Wrong file format: {}", filePath);
        String content;
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fileName = filePath.getFileName().toString();
        int firstUnderscoreIndex = fileName.indexOf("_");
        String order = fileName.substring(0, firstUnderscoreIndex);
        String name = fileName.substring(firstUnderscoreIndex + 1, fileName.length() - 4);
        return new Migration(order, name, content);
    }


    @Value
    private static class Migration {
        String order;
        String name;
        String sql;
    }
}
