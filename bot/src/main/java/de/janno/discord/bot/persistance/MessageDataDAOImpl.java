package de.janno.discord.bot.persistance;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class MessageDataDAOImpl implements MessageDataDAO {

    private final JdbcConnectionPool connectionPool;

    public MessageDataDAOImpl(@NonNull String url, @Nullable String user, @Nullable String password) {
        connectionPool = JdbcConnectionPool.create(url, user, password);
        new DatabaseTableMetrics(connectionPool, "h2", "MESSAGE_DATA", ImmutableSet.of()).bindTo(globalRegistry);

        queryGauge("db.channel.count", "select count (distinct CHANNEL_ID) from MESSAGE_DATA;", connectionPool, Set.of());
        queryGauge("db.guild.count", "select count (distinct GUILD_ID) from MESSAGE_DATA;", connectionPool, Set.of());
        queryGauge("db.guild-null.count", "select count (distinct CHANNEL_ID) from MESSAGE_DATA where GUILD_ID is null;", connectionPool, Set.of());
        queryGauge("db.guild-30d.active", "select count (distinct GUILD_ID) from MESSAGE_DATA where DATEDIFF(DAY,CURRENT_DATE, CREATION_DATE) <= 30;", connectionPool, Set.of());

        try (Connection connection = connectionPool.getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS MESSAGE_DATA(
                        CONFIG_ID       UUID            NOT NULL,
                        CHANNEL_ID      BIGINT          NOT NULL,
                        MESSAGE_ID      BIGINT          NOT NULL,
                        COMMAND_ID      VARCHAR         NOT NULL,
                        CONFIG_CLASS_ID VARCHAR         NOT NULL,
                        CONFIG          VARCHAR         NOT NULL,
                        STATE_CLASS_ID  VARCHAR         NOT NULL,
                        STATE           VARCHAR         NULL,
                        CREATION_DATE   TIMESTAMP       NOT NULL,
                        PRIMARY KEY (CONFIG_ID, CHANNEL_ID, MESSAGE_ID)
                    );

                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_ID ON MESSAGE_DATA (CONFIG_ID);
                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CHANNEL ON MESSAGE_DATA (CHANNEL_ID);
                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CHANNEL_MESSAGE ON MESSAGE_DATA (CHANNEL_ID, MESSAGE_ID);
                                        
                    ALTER TABLE MESSAGE_DATA ADD COLUMN IF NOT EXISTS GUILD_ID BIGINT;
                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_GUILD ON MESSAGE_DATA (GUILD_ID);
                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_GUILD_CHANNEl ON MESSAGE_DATA (GUILD_ID, CHANNEL_ID);
                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CREATION_DATE_GUILD_CHANNEl ON MESSAGE_DATA (CREATION_DATE, GUILD_ID);
                    """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("start db shutdown");
            connectionPool.dispose();
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                connection.createStatement().execute("SHUTDOWN");
                log.info("db shutdown");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private MessageDataDTO transformResultSet(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return new MessageDataDTO(
                    resultSet.getObject("CONFIG_ID", UUID.class),
                    resultSet.getLong("GUILD_ID"),
                    resultSet.getLong("CHANNEL_ID"),
                    resultSet.getLong("MESSAGE_ID"),
                    resultSet.getString("COMMAND_ID"),
                    resultSet.getString("CONFIG_CLASS_ID"),
                    resultSet.getString("CONFIG"),
                    resultSet.getString("STATE_CLASS_ID"),
                    resultSet.getString("STATE")
            );
        }
        return null;
    }

    @Override
    public @NonNull Optional<MessageDataDTO> getDataForMessage(long channelId, long messageId) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM MESSAGE_DATA MC WHERE MC.CHANNEL_ID = ? AND MC.MESSAGE_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, messageId);
                ResultSet resultSet = preparedStatement.executeQuery();
                MessageDataDTO messageDataDTO = transformResultSet(resultSet);

                BotMetrics.databaseTimer("getDataForMessage", stopwatch.elapsed());

                if (messageDataDTO == null) {
                    return Optional.empty();
                }
                return Optional.of(messageDataDTO);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public @NonNull Set<Long> getAllMessageIdsForConfig(@NonNull UUID configUUID) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT DISTINCT MC.MESSAGE_ID FROM MESSAGE_DATA MC WHERE MC.CONFIG_ID = ?")) {
                preparedStatement.setObject(1, configUUID);
                ResultSet resultSet = preparedStatement.executeQuery();
                final ImmutableSet.Builder<Long> resultBuilder = ImmutableSet.builder();
                while (resultSet.next()) {
                    resultBuilder.add(resultSet.getLong("MESSAGE_ID"));
                }
                BotMetrics.databaseTimer("getAllMessageIdsForConfig", stopwatch.elapsed());

                return resultBuilder.build();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void queryGauge(String name, String query, DataSource dataSource, Set<Tag> tags) {
        ToDoubleFunction<DataSource> totalRows = ds -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            } catch (SQLException ignored) {
                return 0;
            }
        };

        Gauge.builder(name, dataSource, totalRows).tags(tags)
                .register(globalRegistry);
    }

    @Override
    public void deleteDataForMessage(long channelId, long messageId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM MESSAGE_DATA WHERE CHANNEL_ID = ? AND MESSAGE_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, messageId);
                preparedStatement.execute();
                BotMetrics.databaseTimer("deleteDataForMessage", stopwatch.elapsed());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NonNull Set<Long> deleteDataForChannel(long channelId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final ImmutableSet<Long> ids;
        try (Connection con = connectionPool.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT DISTINCT MC.MESSAGE_ID FROM MESSAGE_DATA MC WHERE MC.CHANNEL_ID = ?")) {
                preparedStatement.setObject(1, channelId);
                ResultSet resultSet = preparedStatement.executeQuery();
                final ImmutableSet.Builder<Long> resultBuilder = ImmutableSet.builder();
                while (resultSet.next()) {
                    resultBuilder.add(resultSet.getLong("MESSAGE_ID"));
                }
                ids = resultBuilder.build();
            }

            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM MESSAGE_DATA WHERE CHANNEL_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.execute();
            }
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteDataForChannel", stopwatch.elapsed());
        return ids;
    }

    @Override
    public void saveMessageData(@NonNull MessageDataDTO messageData) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO MESSAGE_DATA(CONFIG_ID, GUILD_ID, CHANNEL_ID, MESSAGE_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, STATE_CLASS_ID, STATE, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, messageData.getConfigUUID());
                preparedStatement.setObject(2, messageData.getGuildId());
                preparedStatement.setObject(3, messageData.getChannelId());
                preparedStatement.setLong(4, messageData.getMessageId());
                preparedStatement.setString(5, messageData.getCommandId());
                preparedStatement.setString(6, messageData.getConfigClassId());
                preparedStatement.setString(7, messageData.getConfig());
                preparedStatement.setString(8, messageData.getStateDataClassId());
                preparedStatement.setString(9, messageData.getStateData());
                preparedStatement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("saveMessageData", stopwatch.elapsed());
    }

    @Override
    public void updateCommandConfigOfMessage(long channelId, long messageId, @NonNull String stateDataClassId, @Nullable String stateData) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("UPDATE MESSAGE_DATA SET STATE_CLASS_ID = ?, STATE = ? WHERE CHANNEL_ID = ? AND MESSAGE_ID = ?")) {
                preparedStatement.setString(1, stateDataClassId);
                preparedStatement.setString(2, stateData);
                preparedStatement.setLong(3, channelId);
                preparedStatement.setLong(4, messageId);
                preparedStatement.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("saveMessageData", stopwatch.elapsed());
    }

}
