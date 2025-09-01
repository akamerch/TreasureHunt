package me.merch.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.merch.TreasureHunt;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final TreasureHunt plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(TreasureHunt plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("database.host") + ":" +
                    plugin.getConfig().getInt("database.port") + "/" +
                    plugin.getConfig().getString("database.name") + "?useSSL=false&autoReconnect=true");
            config.setUsername(plugin.getConfig().getString("database.username"));
            config.setPassword(plugin.getConfig().getString("database.password"));
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size"));

            dataSource = new HikariDataSource(config);

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS treasures (" +
                        "id VARCHAR(50) PRIMARY KEY," +
                        "world VARCHAR(100)," +
                        "x INT," +
                        "y INT," +
                        "z INT," +
                        "command TEXT" +
                        ");");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS treasure_claims (" +
                        "treasure_id VARCHAR(50)," +
                        "player_uuid VARCHAR(50)," +
                        "PRIMARY KEY(treasure_id, player_uuid)" +
                        ");");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to the database!");
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (dataSource != null) dataSource.close();
    }

    public Connection getConnection() throws SQLException {
        if (dataSource != null) return dataSource.getConnection();
        throw new SQLException("Database not connected!");
    }
}
