package de.janno.discord.bot.persistance;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class PersistenceManagerImpl implements PersistenceManager {

    private final static long USER_ID_NULL_PLACEHOLDER = -1L;
    private final DatabaseConnector databaseConnector;

    public PersistenceManagerImpl(@NonNull String url, @Nullable String user, @Nullable String password) {
        databaseConnector = new DatabaseConnector(url, user, password);
        DatabaseInitiator.initialize(databaseConnector);

        queryGauge("db.channel.count", "select count (distinct CHANNEL_ID) from MESSAGE_DATA;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.channel.config.count", "select count (distinct CHANNEL_ID) from CHANNEL_CONFIG;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.guild.count", "select count (distinct GUILD_ID) from MESSAGE_DATA;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.guild-null.count", "select count (distinct CHANNEL_ID) from MESSAGE_DATA where GUILD_ID is null;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.messageDataWithConfig.count", "SELECT COUNT(*) FROM (SELECT DISTINCT CHANNEL_ID, MESSAGE_ID FROM MESSAGE_DATA WHERE CONFIG_CLASS_ID IS NOT NULL);", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.guild-30d.active", "select count (distinct GUILD_ID) from MESSAGE_DATA where (CURRENT_TIMESTAMP - CREATION_DATE) <= interval '43200' MINUTE;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.guild-7d.active", "select count (distinct GUILD_ID) from MESSAGE_DATA where (CURRENT_TIMESTAMP - CREATION_DATE) <= interval '10080' MINUTE;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.guild-1d.active", "select count (distinct GUILD_ID) from MESSAGE_DATA where (CURRENT_TIMESTAMP - CREATION_DATE) <= interval '1440' MINUTE;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.messageData-30d.active", "select count (MESSAGE_ID) from MESSAGE_DATA where (CURRENT_TIMESTAMP - CREATION_DATE) <= interval '43200' MINUTE;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.messageData-7d.active", "select count (MESSAGE_ID) from MESSAGE_DATA where (CURRENT_TIMESTAMP - CREATION_DATE) <= interval '10080' MINUTE;", databaseConnector.getDataSource(), Set.of());
        queryGauge("db.messageData-1d.active", "select count (MESSAGE_ID) from MESSAGE_DATA where (CURRENT_TIMESTAMP - CREATION_DATE) <= interval '1440' MINUTE;", databaseConnector.getDataSource(), Set.of());

        long interval = io.avaje.config.Config.getLong("db.deleteMarkMessageDataIntervalInMilliSec", 10_000);
        if (interval > 0) {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::deleteOldMessageDataThatAreMarked,
                    io.avaje.config.Config.getLong("db.deleteMarkMessageDataStartDelayMilliSec", 0),
                    interval,
                    TimeUnit.MILLISECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("start db shutdown");
            databaseConnector.dispose();
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                connection.createStatement().execute("SHUTDOWN");
                log.info("db shutdown");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));


    }

    @VisibleForTesting
    void deleteOldMessageDataThatAreMarked() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final Duration delay = Duration.ofMillis(io.avaje.config.Config.getLong("db.delayMessageDataDeletionMs", 10_000));
        LocalDateTime deleteAllBefore = LocalDateTime.now().minus(delay);

        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM MESSAGE_DATA WHERE MARKED_DELETED IS NOT NULL AND MARKED_DELETED < ?")) {
                preparedStatement.setObject(1, deleteAllBefore);
                long deleted = preparedStatement.executeUpdate();
                if (deleted > 0) {
                    log.trace("deleted message_data: {}", deleted);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteOldMessageDataThatAreMarked", stopwatch.elapsed());
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

        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, CHANNEL_ID, GUILD_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, CONFIG_NAME, CREATION_USER_ID FROM MESSAGE_CONFIG MC WHERE MC.CONFIG_ID = ?")) {
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

    @Override
    public void deleteAllMessageConfigForChannel(long channelId, @Nullable String name) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            final String deleteConfig;
            if (Strings.isNullOrEmpty(name)) {
                deleteConfig = "DELETE FROM MESSAGE_CONFIG WHERE CHANNEL_ID = ?";
            } else {
                deleteConfig = "DELETE FROM MESSAGE_CONFIG WHERE CHANNEL_ID = ? and CONFIG_NAME = ?";
            }
            try (PreparedStatement preparedStatement = con.prepareStatement(deleteConfig)) {
                preparedStatement.setLong(1, channelId);
                if (!Strings.isNullOrEmpty(name)) {
                    preparedStatement.setString(2, name);
                }
                preparedStatement.execute();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteAllMessageConfigForChannel", stopwatch.elapsed());
    }

    private MessageConfigDTO transformResultSet2MessageConfigDTO(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return new MessageConfigDTO(
                    resultSet.getObject("CONFIG_ID", UUID.class),
                    resultSet.getLong("GUILD_ID"),
                    resultSet.getLong("CHANNEL_ID"),
                    resultSet.getString("COMMAND_ID"),
                    resultSet.getString("CONFIG_CLASS_ID"),
                    resultSet.getString("CONFIG"),
                    resultSet.getString("CONFIG_NAME"),
                    resultSet.getObject("CREATION_USER_ID", Long.class)
            );
        }
        return null;
    }

    @Override
    public void saveMessageConfig(@NonNull MessageConfigDTO messageConfigDTO) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO MESSAGE_CONFIG(CONFIG_ID, GUILD_ID, CHANNEL_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, CONFIG_NAME, CREATION_USER_ID, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, messageConfigDTO.getConfigUUID());
                preparedStatement.setObject(2, messageConfigDTO.getGuildId());
                preparedStatement.setObject(3, messageConfigDTO.getChannelId());
                preparedStatement.setString(4, messageConfigDTO.getCommandId());
                preparedStatement.setString(5, messageConfigDTO.getConfigClassId());
                preparedStatement.setString(6, messageConfigDTO.getConfig());
                preparedStatement.setString(7, messageConfigDTO.getName());
                preparedStatement.setObject(8, messageConfigDTO.getCreationUserId());
                preparedStatement.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
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

        try (Connection con = databaseConnector.getConnection()) {
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
    public @NonNull Set<Long> getAllActiveMessageIdsForConfig(@NonNull UUID configUUID) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT DISTINCT MD.MESSAGE_ID FROM MESSAGE_DATA MD WHERE MD.CONFIG_ID = ? AND MD.MARKED_DELETED is null")) {
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

    @Override
    public void markAsDeleted(long channelId, long messageId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("UPDATE MESSAGE_DATA SET MARKED_DELETED = ?  WHERE CHANNEL_ID = ? AND MESSAGE_ID = ?")) {
                preparedStatement.setObject(1, LocalDateTime.now());
                preparedStatement.setLong(2, channelId);
                preparedStatement.setLong(3, messageId);
                preparedStatement.execute();
                BotMetrics.databaseTimer("markAsDeleted", stopwatch.elapsed());
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
        try (Connection con = databaseConnector.getConnection()) {
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
    public @NonNull Set<Long> deleteMessageDataForChannel(long channelId, String name) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final ImmutableSet<Long> messageIds;
        try (Connection con = databaseConnector.getConnection()) {
            con.setAutoCommit(false);
            final String messageIdSql;
            if (Strings.isNullOrEmpty(name)) {
                messageIdSql = "SELECT DISTINCT MD.MESSAGE_ID FROM MESSAGE_DATA MD WHERE MD.CHANNEL_ID = ?";
            } else {
                messageIdSql = "SELECT DISTINCT MD.MESSAGE_ID FROM MESSAGE_DATA MD JOIN MESSAGE_CONFIG MC ON MC.CONFIG_ID = MD.CONFIG_ID WHERE MD.CHANNEL_ID = ? and MC.CONFIG_NAME = ?";
            }
            try (PreparedStatement preparedStatement = con.prepareStatement(messageIdSql)) {
                preparedStatement.setObject(1, channelId);
                if (!Strings.isNullOrEmpty(name)) {
                    preparedStatement.setString(2, name);
                }
                ResultSet resultSet = preparedStatement.executeQuery();
                final ImmutableSet.Builder<Long> messageIdBuilder = ImmutableSet.builder();
                while (resultSet.next()) {
                    messageIdBuilder.add(resultSet.getLong("MESSAGE_ID"));
                }
                messageIds = messageIdBuilder.build();
            }
            final String markDeletedSql;

            if (Strings.isNullOrEmpty(name)) {
                markDeletedSql = "UPDATE MESSAGE_DATA SET MARKED_DELETED = ? WHERE CHANNEL_ID = ?";
            } else {
                markDeletedSql = "UPDATE MESSAGE_DATA SET MARKED_DELETED = ?  where message_id in (SELECT DISTINCT MD.MESSAGE_ID FROM MESSAGE_DATA MD JOIN MESSAGE_CONFIG MC ON MC.CONFIG_ID = MD.CONFIG_ID WHERE MD.CHANNEL_ID = ? and MC.CONFIG_NAME = ?)";
            }

            try (PreparedStatement preparedStatement = con.prepareStatement(markDeletedSql)) {
                preparedStatement.setObject(1, LocalDateTime.now());
                preparedStatement.setLong(2, channelId);
                if (!Strings.isNullOrEmpty(name)) {
                    preparedStatement.setString(3, name);
                }
                preparedStatement.execute();
            }
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteDataForChannel", stopwatch.elapsed());
        return messageIds;
    }

    @Override
    public void saveMessageData(@NonNull MessageDataDTO messageData) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
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
        try (Connection con = databaseConnector.getConnection()) {
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
    public @NonNull Optional<ChannelConfigDTO> getChannelConfig(long channelId, @NonNull String configClassId) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, CHANNEL_ID, GUILD_ID, COMMAND_ID, USER_ID, CONFIG_CLASS_ID, CONFIG, CONFIG_NAME FROM CHANNEL_CONFIG CC WHERE CC.CHANNEL_ID = ? AND CC.CONFIG_CLASS_ID = ? AND CC.USER_ID = " + USER_ID_NULL_PLACEHOLDER)) {
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
    public @NonNull Optional<ChannelConfigDTO> getUserChannelConfig(long channelId, long userId, @NonNull String configClassId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Preconditions.checkArgument(!Objects.equals(userId, USER_ID_NULL_PLACEHOLDER), "The userId is not to be allowed to be %d".formatted(USER_ID_NULL_PLACEHOLDER));

        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT CONFIG_ID, CHANNEL_ID, GUILD_ID, USER_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, CONFIG_NAME FROM CHANNEL_CONFIG CC WHERE CC.USER_ID = ? AND CC.CHANNEL_ID = ? AND CC.CONFIG_CLASS_ID = ?")) {
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
                    resultSet.getString("CONFIG"),
                    resultSet.getString("CONFIG_NAME")
            );
        }
        return null;
    }

    @Override
    public void saveChannelConfig(@NonNull ChannelConfigDTO channelConfigDTO) {
        Preconditions.checkArgument(!Objects.equals(channelConfigDTO.getUserId(), USER_ID_NULL_PLACEHOLDER), "The userId is not to be allowed to be %d".formatted(USER_ID_NULL_PLACEHOLDER));
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            final long userId = channelConfigDTO.getUserId() == null ? USER_ID_NULL_PLACEHOLDER : channelConfigDTO.getUserId();
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO CHANNEL_CONFIG(CONFIG_ID, GUILD_ID, CHANNEL_ID, USER_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, CONFIG_NAME, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, channelConfigDTO.getConfigUUID());
                preparedStatement.setObject(2, channelConfigDTO.getGuildId());
                preparedStatement.setObject(3, channelConfigDTO.getChannelId());
                preparedStatement.setObject(4, userId);
                preparedStatement.setString(5, channelConfigDTO.getCommandId());
                preparedStatement.setString(6, channelConfigDTO.getConfigClassId());
                preparedStatement.setString(7, channelConfigDTO.getConfig());
                preparedStatement.setString(8, channelConfigDTO.getName());
                preparedStatement.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
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
        try (Connection con = databaseConnector.getConnection()) {

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
        try (Connection con = databaseConnector.getConnection()) {

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

    @Override
    public void deleteAllChannelConfig(long channelId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {

            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM CHANNEL_CONFIG WHERE CHANNEL_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteAllChannelConfig", stopwatch.elapsed());
    }


    @Override
    public void deleteMessageConfig(UUID configUUID) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {

            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM MESSAGE_CONFIG WHERE CONFIG_ID = ?")) {
                preparedStatement.setObject(1, configUUID);
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BotMetrics.databaseTimer("deleteMessageConfig", stopwatch.elapsed());
    }

    @Override
    public Optional<MessageConfigDTO> getNewestMessageDataInChannel(long channelId, LocalDateTime since, Set<String> commandIds) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            final String sql = """
                    SELECT MC.CONFIG_ID, MC.CHANNEL_ID, MC.GUILD_ID, MC.COMMAND_ID, MC.CONFIG_CLASS_ID, MC.CONFIG, MC.CONFIG_NAME, MC.CREATION_USER_ID
                    FROM MESSAGE_CONFIG MC
                             join MESSAGE_DATA MD on mc.CONFIG_ID = md.CONFIG_ID
                    WHERE MD.CHANNEL_ID = ?
                    AND MD.CREATION_DATE < ?
                    AND MD.COMMAND_ID in ("""
                    + commandIds.stream().map("'%s'"::formatted).collect(Collectors.joining(","))
                    + """
                    ) AND MD.MARKED_DELETED is null
                    order by MD.CREATION_DATE desc
                    """;
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setTimestamp(2, Timestamp.valueOf(since));
                preparedStatement.setMaxRows(1);
                ResultSet resultSet = preparedStatement.executeQuery();
                MessageConfigDTO messageConfigDTO = transformResultSet2MessageConfigDTO(resultSet);

                BotMetrics.databaseTimer("getLastMessageDataInChannel", stopwatch.elapsed());

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
    public List<SavedNamedConfigId> getLastUsedNamedCommandsOfUserAndGuild(long userId, Long guildId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            final String sql;
            if (guildId != null) {
                sql = """
                        SELECT MC.CONFIG_ID, MC.COMMAND_ID, MC.CONFIG_NAME
                        FROM MESSAGE_CONFIG MC
                                 JOIN (SELECT MC2.COMMAND_ID, MC2.CONFIG_NAME, MAX(MC2.CREATION_DATE) AS LatestDate
                                       FROM MESSAGE_CONFIG MC2
                                       where (MC2.CREATION_USER_ID = ?
                                           OR MC2.GUILD_ID = ?)
                                         and MC2.CONFIG_NAME is not null
                                       GROUP BY MC2.COMMAND_ID, MC2.CONFIG_NAME) Latest
                                      ON MC.COMMAND_ID = Latest.COMMAND_ID
                                          and mc.CONFIG_NAME = Latest.CONFIG_NAME
                                          AND MC.CREATION_DATE = Latest.LatestDate
                        where (MC.CREATION_USER_ID = ?
                            OR MC.GUILD_ID = ?)
                          and MC.CONFIG_NAME is not null
                        order by MC.CONFIG_NAME;
                        """;
            } else {
                sql = """
                        SELECT MC.CONFIG_ID, MC.COMMAND_ID, MC.CONFIG_NAME
                        FROM MESSAGE_CONFIG MC
                                 JOIN (SELECT MC2.COMMAND_ID, MC2.CONFIG_NAME, MAX(MC2.CREATION_DATE) AS LatestDate
                                       FROM MESSAGE_CONFIG MC2
                                       where MC2.CREATION_USER_ID = ?
                                         and MC2.CONFIG_NAME is not null
                                       GROUP BY MC2.COMMAND_ID, MC2.CONFIG_NAME) Latest
                                      ON MC.COMMAND_ID = Latest.COMMAND_ID
                                          and mc.CONFIG_NAME = Latest.CONFIG_NAME
                                          AND MC.CREATION_DATE = Latest.LatestDate
                        where MC.CREATION_USER_ID = ?
                          and MC.CONFIG_NAME is not null
                        order by MC.CONFIG_NAME;
                        """;
            }
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
                if (guildId != null) {
                    preparedStatement.setLong(1, userId);
                    preparedStatement.setLong(2, guildId);
                    preparedStatement.setLong(3, userId);
                    preparedStatement.setLong(4, guildId);
                } else {
                    preparedStatement.setLong(1, userId);
                    preparedStatement.setLong(2, userId);
                }

                ResultSet resultSet = preparedStatement.executeQuery();
                List<SavedNamedConfigId> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new SavedNamedConfigId(
                            resultSet.getObject("CONFIG_ID", UUID.class),
                            resultSet.getString("COMMAND_ID"),
                            resultSet.getString("CONFIG_NAME")
                    ));
                }
                BotMetrics.databaseTimer("getNamedCommandsOfUserAndGuild", stopwatch.elapsed());
                return result;

            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<String> getNamedCommandsChannel(long channelId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Connection con = databaseConnector.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("""
                    SELECT DISTINCT MC.CONFIG_NAME
                    FROM MESSAGE_CONFIG MC
                    where MC.CHANNEL_ID = ?
                      and MC.CONFIG_NAME is not null
                    order by MC.CONFIG_NAME;
                    """)) {

                preparedStatement.setLong(1, channelId);

                ResultSet resultSet = preparedStatement.executeQuery();
                List<String> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(resultSet.getString("CONFIG_NAME"));
                }
                BotMetrics.databaseTimer("getNamedCommandsChannel", stopwatch.elapsed());
                return result;

            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
