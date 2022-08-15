package de.janno.discord.bot.persistance;

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class MessageDataDAOImpl implements MessageDataDAO {

    //todo syncroniced oder transaction?
    private final JdbcConnectionPool connectionPool;


    public MessageDataDAOImpl(String url, String user, String password) {
        connectionPool = JdbcConnectionPool.create(url, user, password);

        //todo metrics
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
                    """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MessageDataDTO transformResultSet(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return new MessageDataDTO(
                    resultSet.getObject("CONFIG_ID", UUID.class),
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
    public Optional<MessageDataDTO> getDataForMessage(long channelId, long messageId) {
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM MESSAGE_DATA MC WHERE MC.CHANNEL_ID = ? AND MC.MESSAGE_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, messageId);
                ResultSet resultSet = preparedStatement.executeQuery();
                MessageDataDTO messageDataDTO = transformResultSet(resultSet);
                log.debug("getDataForMessage: {}", messageDataDTO);

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
    public Set<Long> getAllMessageIdsForConfig(UUID configUUID) {
        log.debug("getAllMessageIdsForConfig: {}", configUUID);

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT DISTINCT MC.MESSAGE_ID FROM MESSAGE_DATA MC WHERE MC.CONFIG_ID = ?")) {
                preparedStatement.setObject(1, configUUID);
                ResultSet resultSet = preparedStatement.executeQuery();
                final ImmutableSet.Builder<Long> resultBuilder = ImmutableSet.builder();
                while (resultSet.next()) {
                    resultBuilder.add(resultSet.getLong("MESSAGE_ID"));
                }
                return resultBuilder.build();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void deleteDataForMessage(long channelId, long messageId) {
        log.debug("deleteDataForMessage: {}.{}", channelId, messageId);

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM MESSAGE_DATA WHERE CHANNEL_ID = ? AND MESSAGE_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, messageId);
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDataForChannel(long channelId) {
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM MESSAGE_DATA WHERE CHANNEL_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveMessageData(MessageDataDTO messageData) {
        log.debug("saveMessageData: {}", messageData);

        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO MESSAGE_DATA(CONFIG_ID, CHANNEL_ID, MESSAGE_ID, COMMAND_ID, CONFIG_CLASS_ID, CONFIG, STATE_CLASS_ID, STATE, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, messageData.getConfigUUID());
                preparedStatement.setObject(2, messageData.getChannelId());
                preparedStatement.setLong(3, messageData.getMessageId());
                preparedStatement.setString(4, messageData.getCommandId());
                preparedStatement.setString(5, messageData.getConfigClassId());
                preparedStatement.setString(6, messageData.getConfig());
                preparedStatement.setString(7, messageData.getStateDataClassId());
                preparedStatement.setString(8, messageData.getStateData());
                preparedStatement.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateCommandConfigOfMessage(long channelId, long messageId, @NonNull String stateDataClassId, @Nullable String stateData) {
        log.debug("updateCommandConfigOfMessage: {}-{}", stateDataClassId, stateData);
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
    }

}
