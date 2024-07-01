package de.janno.discord.bot.persistance;

import com.google.common.collect.ImmutableSet;
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics;
import lombok.NonNull;
import org.h2.jdbcx.JdbcConnectionPool;
import javax.annotation.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

public class DatabaseConnector {

    private final JdbcConnectionPool connectionPool;

    public DatabaseConnector(@NonNull String url, @Nullable String user, @Nullable String password) {
        connectionPool = JdbcConnectionPool.create(url, user, password);
        new DatabaseTableMetrics(connectionPool, "h2", "MESSAGE_DATA", ImmutableSet.of()).bindTo(globalRegistry);
        new DatabaseTableMetrics(connectionPool, "h2", "MESSAGE_CONFIG", ImmutableSet.of()).bindTo(globalRegistry);
        new DatabaseTableMetrics(connectionPool, "h2", "CHANNEL_CONFIG", ImmutableSet.of()).bindTo(globalRegistry);

    }

    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    public void dispose() {
        connectionPool.dispose();
    }

    public DataSource getDataSource(){
        return connectionPool;
    }
}
