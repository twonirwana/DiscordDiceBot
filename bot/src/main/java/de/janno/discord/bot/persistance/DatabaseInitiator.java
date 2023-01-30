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
import java.util.stream.Stream;

@Slf4j
public class DatabaseInitiator {

    public DatabaseInitiator(@NonNull String url, @Nullable String user, @Nullable String password) {
        JdbcConnectionPool connectionPool = JdbcConnectionPool.create(url, user, password);

        List<Migration> allMigrations = readMigrations();
        log.info("found migrations: {}", allMigrations);
        List<String> alreadyAppliedMigrations = getAlreadyAppliedMigrations(connectionPool);
        log.info("db migration status: {}", alreadyAppliedMigrations);

    }

    private void applyMissingMigrations(List<Migration> allMigrations, List<String> alreadyAppliedMigrations, JdbcConnectionPool connectionPool) {
        allMigrations.stream()
                .filter(m -> !alreadyAppliedMigrations.contains(m.getName()))
                .sorted(Comparator.comparing(Migration::getOrder))
                .forEach(m -> {
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
                        throw new RuntimeException(e);
                    }
                });
    }

    private List<String> getAlreadyAppliedMigrations(JdbcConnectionPool connectionPool) {
        try (Connection connection = connectionPool.getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select MIGRATION_NAME from DB_VERSION");
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            if (resultSet.next()) {
                builder.add(resultSet.getString("MIGRATION_NAME"));
            }
            return builder.build();
        } catch (SQLException e) {
            return List.of();
        }
    }

    private List<Migration> readMigrations() {
        try (Stream<Path> stream = Files.list(Paths.get(Resources.getResource("db/migrations").toURI()))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(this::readMigration)
                    .toList();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Migration readMigration(Path filePath) {
        Preconditions.checkArgument(filePath.getFileName().toString().contains("_"));
        Preconditions.checkArgument(filePath.getFileName().endsWith("sql"));
        String content = null;
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] split = filePath.getFileName().toString().split("_");
        return new Migration(Integer.parseInt(split[0]), split[1], content);
    }


    @Value
    private static class Migration {
        int order;
        String name;
        String sql;
    }
}
