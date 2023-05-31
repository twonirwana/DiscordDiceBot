package de.janno.discord.bot.persistance;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class PersistenceManagerImpl implements PersistenceManager {

    private final static long USER_ID_NULL_PLACEHOLDER = -1L;
    private final JdbcConnectionPool connectionPool;

    public PersistenceManagerImpl(@NonNull String url, @Nullable String user, @Nullable String password) {
        DatabaseInitiator.initialize(url, user, password);

        connectionPool = JdbcConnectionPool.create(url, user, password);

        new DatabaseTableMetrics(connectionPool, "h2", "MESSAGE_DATA", ImmutableSet.of()).bindTo(globalRegistry);
        new DatabaseTableMetrics(connectionPool, "h2", "MESSAGE_CONFIG", ImmutableSet.of()).bindTo(globalRegistry);
        new DatabaseTableMetrics(connectionPool, "h2", "CHANNEL_CONFIG", ImmutableSet.of()).bindTo(globalRegistry);

        queryGauge("db.channel.count", "select count (distinct CHANNEL_ID) from MESSAGE_DATA;", connectionPool, Set.of());
        queryGauge("db.channel.config.count", "select count (distinct CHANNEL_ID) from CHANNEL_CONFIG;", connectionPool, Set.of());
        queryGauge("db.guild.count", "select count (distinct GUILD_ID) from MESSAGE_DATA;", connectionPool, Set.of());
        queryGauge("db.guild-null.count", "select count (distinct CHANNEL_ID) from MESSAGE_DATA where GUILD_ID is null;", connectionPool, Set.of());
        queryGauge("db.messageDataWithConfig.count", "SELECT COUNT(*) FROM (SELECT DISTINCT CHANNEL_ID, MESSAGE_ID FROM MESSAGE_DATA WHERE CONFIG_CLASS_ID IS NOT NULL);", connectionPool, Set.of());
        queryGauge("db.guild-30d.active", "select count (distinct GUILD_ID) from MESSAGE_DATA where (CURRENT_TIMESTAMP - CREATION_DATE) <= interval '43200' MINUTE;", connectionPool, Set.of());

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

    private MessageDataDTO transformResultSet2MessageDataDTO(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return new MessageDataDTO(
                    resultSet.getObject("CONFIG_ID", UUID.class),
                    resultSet.getLong("GUILD_ID"),
                    resultSet.getLong("CHANNEL_ID"),
                    resultSet.getLong("MESSAGE_ID"),
                    resultSet.getString("COMMAND_ID"),
                    resultSet.getString("STATE_CLASS_ID"),
                    resultSet.getString("STATE")
            );
        }
        return null;
    }

    @Override
    public @NonNull Optional<MessageConfigDTO> getMessageConfig(@NonNull UUID configUUID) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, CHANNEL_ID, GUILD_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG FROM MESSAGE_CONFIG MC WHERE MC.CONFIG_ID = ?")) {
                preparedStatement.setObject(1, configUUID);
                ResultSet resultSet = preparedStatement.executeQuery();
                MessageConfigDTO messageConfigDTO = transformResultSet2MessageConfigDTO(resultSet);

                BotMetrics.databaseTimer("getConfig", stopwatch.elapsed());

                if (messageConfigDTO == null) {
                    return Optional.empty();
                }
                return Optional.of(messageConfigDTO);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MessageConfigDTO transformResultSet2MessageConfigDTO(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return new MessageConfigDTO(
                    resultSet.getObject("CONFIG_ID", UUID.class),
                    resultSet.getLong("GUILD_ID"),
                    resultSet.getLong("CHANNEL_ID"),
                    resultSet.getString("COMMAND_ID"),
                    resultSet.getString("CONFIG_CLASS_ID"),
                    resultSet.getString("CONFIG")
            );
        }
        return null;
    }

    @Override
    public @NonNull Optional<MessageConfigDTO> getConfigFromMessage(long channelId, long messageId) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, GUILD_ID, CHANNEL_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG FROM MESSAGE_DATA MC WHERE MC.CHANNEL_ID = ? AND MC.MESSAGE_ID = ? AND MC.CONFIG_CLASS_ID IS NOT NULL")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, messageId);
                ResultSet resultSet = preparedStatement.executeQuery();
                MessageConfigDTO messageConfigDTO = transformResultSet2MessageConfigDTO(resultSet);

                BotMetrics.databaseTimer("getConfigFromMessage", stopwatch.elapsed());

                if (messageConfigDTO == null) {
                    return Optional.empty();
                }
                return Optional.of(messageConfigDTO);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void saveMessageConfig(@NonNull MessageConfigDTO messageConfigDTO) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO MESSAGE_CONFIG(CONFIG_ID, GUILD_ID, CHANNEL_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, messageConfigDTO.getConfigUUID());
                preparedStatement.setObject(2, messageConfigDTO.getGuildId());
                preparedStatement.setObject(3, messageConfigDTO.getChannelId());
                preparedStatement.setString(4, messageConfigDTO.getCommandId());
                preparedStatement.setString(5, messageConfigDTO.getConfigClassId());
                preparedStatement.setString(6, messageConfigDTO.getConfig());
                preparedStatement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("saveConfig", stopwatch.elapsed());
    }

    @Override
    public @NonNull Optional<MessageDataDTO> getMessageData(long channelId, long messageId) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, CHANNEL_ID, MESSAGE_ID, COMMAND_ID, STATE_CLASS_ID, STATE, GUILD_ID FROM MESSAGE_DATA MC WHERE MC.CHANNEL_ID = ? AND MC.MESSAGE_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, messageId);
                ResultSet resultSet = preparedStatement.executeQuery();
                MessageDataDTO messageDataDTO = transformResultSet2MessageDataDTO(resultSet);

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

    private void queryGauge(String name, String query, DataSource dataSource, Set<Tag> tags) {
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
    public void deleteStateForMessage(long channelId, long messageId) {
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
    public @NonNull Set<Long> deleteMessageDataForChannel(long channelId) {
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
                         con.prepareStatement("INSERT INTO MESSAGE_DATA(CONFIG_ID, GUILD_ID, CHANNEL_ID, MESSAGE_ID, COMMAND_ID, STATE_CLASS_ID, STATE, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, messageData.getConfigUUID());
                preparedStatement.setObject(2, messageData.getGuildId());
                preparedStatement.setObject(3, messageData.getChannelId());
                preparedStatement.setLong(4, messageData.getMessageId());
                preparedStatement.setString(5, messageData.getCommandId());
                preparedStatement.setString(6, messageData.getStateDataClassId());
                preparedStatement.setString(7, messageData.getStateData());
                preparedStatement.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("saveMessageData", stopwatch.elapsed());
    }

    @Override
    public Set<Long> getAllGuildIds() {
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT DISTINCT GUILD_ID FROM MESSAGE_DATA MC WHERE MC.GUILD_ID IS NOT NULL")) {
                ResultSet resultSet = preparedStatement.executeQuery();
                final ImmutableSet.Builder<Long> resultBuilder = ImmutableSet.builder();
                while (resultSet.next()) {
                    resultBuilder.add(resultSet.getLong("GUILD_ID"));
                }
                return resultBuilder.build();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NonNull Optional<ChannelConfigDTO> getChannelConfig(long channelId, @NotNull String configClassId) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, CHANNEL_ID, GUILD_ID, COMMAND_ID, USER_ID, CONFIG_CLASS_ID, CONFIG FROM CHANNEL_CONFIG CC WHERE CC.CHANNEL_ID = ? AND CC.CONFIG_CLASS_ID = ? AND CC.USER_ID = " + USER_ID_NULL_PLACEHOLDER)) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setString(2, configClassId);
                ResultSet resultSet = preparedStatement.executeQuery();
                ChannelConfigDTO channelConfigDTO = transformResultSet2ChannelConfigDTO(resultSet);

                BotMetrics.databaseTimer("getChannelConfig", stopwatch.elapsed());

                if (channelConfigDTO == null) {
                    return Optional.empty();
                }
                return Optional.of(channelConfigDTO);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NonNull Optional<ChannelConfigDTO> getUserChannelConfig(long channelId, long userId, @NotNull String configClassId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Preconditions.checkArgument(!Objects.equals(userId, USER_ID_NULL_PLACEHOLDER), "The userId is not to be allowed to be %d".formatted(USER_ID_NULL_PLACEHOLDER));

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, CHANNEL_ID, GUILD_ID, USER_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG FROM CHANNEL_CONFIG CC WHERE CC.USER_ID = ? AND CC.CHANNEL_ID = ? AND CC.CONFIG_CLASS_ID = ?")) {
                preparedStatement.setLong(1, userId);
                preparedStatement.setLong(2, channelId);
                preparedStatement.setString(3, configClassId);
                ResultSet resultSet = preparedStatement.executeQuery();
                ChannelConfigDTO channelConfigDTO = transformResultSet2ChannelConfigDTO(resultSet);

                BotMetrics.databaseTimer("getUserChannelConfig", stopwatch.elapsed());

                if (channelConfigDTO == null) {
                    return Optional.empty();
                }
                return Optional.of(channelConfigDTO);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ChannelConfigDTO transformResultSet2ChannelConfigDTO(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            Long userIdInDB = resultSet.getObject("USER_ID", Long.class);
            return new ChannelConfigDTO(
                    resultSet.getObject("CONFIG_ID", UUID.class),
                    resultSet.getLong("GUILD_ID"),
                    resultSet.getLong("CHANNEL_ID"),
                    userIdInDB == USER_ID_NULL_PLACEHOLDER ? null : userIdInDB,
                    resultSet.getString("COMMAND_ID"),
                    resultSet.getString("CONFIG_CLASS_ID"),
                    resultSet.getString("CONFIG")
            );
        }
        return null;
    }

    @Override
    public void saveChannelConfig(@NonNull ChannelConfigDTO channelConfigDTO) {
        Preconditions.checkArgument(!Objects.equals(channelConfigDTO.getUserId(), USER_ID_NULL_PLACEHOLDER), "The userId is not to be allowed to be %d".formatted(USER_ID_NULL_PLACEHOLDER));
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {
            final long userId = channelConfigDTO.getUserId() == null ? USER_ID_NULL_PLACEHOLDER : channelConfigDTO.getUserId();
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO CHANNEL_CONFIG(CONFIG_ID, GUILD_ID, CHANNEL_ID, USER_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, channelConfigDTO.getConfigUUID());
                preparedStatement.setObject(2, channelConfigDTO.getGuildId());
                preparedStatement.setObject(3, channelConfigDTO.getChannelId());
                preparedStatement.setObject(4, userId);
                preparedStatement.setString(5, channelConfigDTO.getCommandId());
                preparedStatement.setString(6, channelConfigDTO.getConfigClassId());
                preparedStatement.setString(7, channelConfigDTO.getConfig());
                preparedStatement.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("saveChannelConfig", stopwatch.elapsed());
    }

    @Override
    public void deleteChannelConfig(long channelId, String configClassId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {

            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM CHANNEL_CONFIG WHERE CHANNEL_ID = ? AND CONFIG_CLASS_ID = ? AND USER_ID = " + USER_ID_NULL_PLACEHOLDER)) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setString(2, configClassId);
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteChannelConfig", stopwatch.elapsed());
    }

    @Override
    public void deleteUserChannelConfig(long channelId, long userId, String configClassId) {
        Preconditions.checkArgument(!Objects.equals(userId, USER_ID_NULL_PLACEHOLDER), "The userId is not to be allowed to be %d".formatted(USER_ID_NULL_PLACEHOLDER));
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = connectionPool.getConnection()) {

            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM CHANNEL_CONFIG WHERE CHANNEL_ID = ? AND USER_ID = ? AND CONFIG_CLASS_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, userId);
                preparedStatement.setString(3, configClassId);
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteUserChannelConfig", stopwatch.elapsed());
    }

}
