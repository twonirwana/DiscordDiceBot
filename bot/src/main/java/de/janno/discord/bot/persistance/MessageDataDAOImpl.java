package de.janno.discord.bot.persistance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.command.StateData;
import lombok.NonNull;
import lombok.Value;
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
                        CHANNEL_ID      BIGINT          NOT NULL,
                        MESSAGE_ID      BIGINT          NOT NULL,
                        COMMAND_ID      VARCHAR         NOT NULL,
                        CONFIG_ID       VARCHAR         NOT NULL,
                        CONFIG          VARCHAR         NOT NULL,
                        STATE_ID        VARCHAR         NOT NULL,
                        STATE           VARCHAR         NULL,
                        CREATION_DATE   TIMESTAMP       NOT NULL,
                        PRIMARY KEY (CHANNEL_ID, MESSAGE_ID)
                    );

                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CHANNEL ON MESSAGE_DATA (CHANNEL_ID);
                    CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CHANNEL_MESSAGE ON MESSAGE_DATA (CHANNEL_ID, MESSAGE_ID);
                    """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MessageDataRowDTO transformResultSet(ResultSet resultSet) throws SQLException {

        if (resultSet.next()) {
            return new MessageDataRowDTO(
                    resultSet.getLong("CHANNEL_ID"),
                    resultSet.getLong("MESSAGE_ID"),
                    resultSet.getString("COMMAND_ID"),
                    resultSet.getString("CONFIG_ID"),
                    resultSet.getString("CONFIG"),
                    resultSet.getString("STATE_ID"),
                    resultSet.getString("STATE")
            );
        }
        return null;
    }

    @Override
    public Optional<MessageData> getDataForMessage(long channelId, long messageId) {
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM MESSAGE_DATA MC WHERE MC.CHANNEL_ID = ? AND MC.MESSAGE_ID = ?")) {
                preparedStatement.setLong(1, channelId);
                preparedStatement.setLong(2, messageId);
                ResultSet resultSet = preparedStatement.executeQuery();
                MessageDataRowDTO rowDTOS = transformResultSet(resultSet);
                if (rowDTOS == null) {
                    return Optional.empty();
                }
                return Optional.of(deserialize(rowDTOS));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MessageData deserialize(MessageDataRowDTO rowDTOS) throws JsonProcessingException {
        Config config = Mapper.deserializeConfig(rowDTOS.getConfig(), rowDTOS.getConfigId());
        StateData state = Mapper.deserializeState(rowDTOS.getState(), rowDTOS.getStateId());
        return new MessageData(rowDTOS.getChannelId(), rowDTOS.getMessageId(), rowDTOS.getCommandId(), config, state);
    }


    @Override
    public Set<Long> getAllMessageIdsForConfig(UUID configUUID) {
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
    public void saveMessageData(MessageData messageData) {
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO MESSAGE_DATA(CHANNEL_ID, MESSAGE_ID, COMMAND_ID, CONFIG_ID, CONFIG, STATE_ID, STATE, CREATION_DATE) VALUES( ?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, messageData.getChannelId());
                preparedStatement.setLong(2, messageData.getMessageId());
                preparedStatement.setString(3, messageData.getCommandId());
                preparedStatement.setString(4, Mapper.getConfigId(messageData.getConfig()));
                preparedStatement.setString(5, Mapper.serializedConfig(messageData.getConfig()));
                preparedStatement.setString(6, Mapper.getStateDataId(messageData.getState()));
                preparedStatement.setString(7, Mapper.serializedStateData(messageData.getState()));
                preparedStatement.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateCommandConfigOfMessage(long channelId, long messageId, String stateId, State state) {
        //todo
    }

    @Value
    private static class MessageDataRowDTO {
        long channelId;
        long messageId;

        @NonNull
        String commandId;

        @NonNull
        String configId;

        @NonNull
        String config;

        @NonNull
        String stateId;

        @Nullable
        String state;
    }
}
