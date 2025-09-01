package me.merch.storage;

import me.merch.TreasureHunt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;

public class TreasureStorage {

    private final TreasureHunt plugin;
    private final Map<String, Treasure> treasures = new HashMap<>();

    public TreasureStorage(TreasureHunt plugin) {
        this.plugin = plugin;
        loadTreasures();
    }

    public void loadTreasures() {
        treasures.clear();
        if (plugin.getConfig().getBoolean("database.enabled")) {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM treasures")) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    String command = rs.getString("command");
                    treasures.put(id, new Treasure(id, new Location(Bukkit.getWorld(world), x, y, z), command));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            FileConfiguration config = plugin.getConfig();
            if (config.contains("treasures")) {
                for (String id : config.getConfigurationSection("treasures").getKeys(false)) {
                    String path = "treasures." + id;
                    String world = config.getString(path + ".world");
                    int x = config.getInt(path + ".x");
                    int y = config.getInt(path + ".y");
                    int z = config.getInt(path + ".z");
                    String command = config.getString(path + ".command");
                    treasures.put(id, new Treasure(id, new Location(Bukkit.getWorld(world), x, y, z), command));
                }
            }
        }
    }

    public Map<String, Treasure> getTreasures() {
        return treasures;
    }

    public Treasure getTreasure(String id) {
        return treasures.get(id);
    }

    public void addTreasure(Treasure treasure) {
        treasures.put(treasure.getId(), treasure);
        if (plugin.getConfig().getBoolean("database.enabled")) {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO treasures (id, world, x, y, z, command) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, treasure.getId());
                ps.setString(2, treasure.getLocation().getWorld().getName());
                ps.setInt(3, treasure.getLocation().getBlockX());
                ps.setInt(4, treasure.getLocation().getBlockY());
                ps.setInt(5, treasure.getLocation().getBlockZ());
                ps.setString(6, treasure.getCommand());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            plugin.getConfig().set("treasures." + treasure.getId() + ".world", treasure.getLocation().getWorld().getName());
            plugin.getConfig().set("treasures." + treasure.getId() + ".x", treasure.getLocation().getBlockX());
            plugin.getConfig().set("treasures." + treasure.getId() + ".y", treasure.getLocation().getBlockY());
            plugin.getConfig().set("treasures." + treasure.getId() + ".z", treasure.getLocation().getBlockZ());
            plugin.getConfig().set("treasures." + treasure.getId() + ".command", treasure.getCommand());
            plugin.saveConfig();
        }
    }

    public void removeTreasure(String id) {
        treasures.remove(id);
        if (plugin.getConfig().getBoolean("database.enabled")) {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM treasures WHERE id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            plugin.getConfig().set("treasures." + id, null);
            plugin.saveConfig();
        }
    }

    public Map<UUID, Integer> getCompletedWithCounts(String treasureId) {
        Map<UUID, Integer> result = new HashMap<>();
        if (plugin.getConfig().getBoolean("database.enabled")) {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT player_uuid, COUNT(*) AS count FROM treasure_claims WHERE treasure_id=? GROUP BY player_uuid")) {
                ps.setString(1, treasureId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    result.put(UUID.fromString(rs.getString("player_uuid")), rs.getInt("count"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (plugin.getConfig().contains("treasure_claims." + treasureId)) {
                for (String key : plugin.getConfig().getConfigurationSection("treasure_claims." + treasureId).getKeys(false)) {
                    UUID uuid = UUID.fromString(key);
                    int count = plugin.getConfig().getInt("treasure_claims." + treasureId + "." + key, 1);
                    result.put(uuid, count);
                }
            }
        }
        return result;
    }

    public void claimTreasure(String treasureId, UUID playerUUID) {
        if (plugin.getConfig().getBoolean("database.enabled")) {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO treasure_claims (treasure_id, player_uuid) VALUES (?, ?)")) {
                ps.setString(1, treasureId);
                ps.setString(2, playerUUID.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            String path = "treasure_claims." + treasureId + "." + playerUUID.toString();
            int current = plugin.getConfig().getInt(path, 0);
            plugin.getConfig().set(path, current + 1);
            plugin.saveConfig();
        }
    }

    public static class Treasure {
        private final String id;
        private final Location location;
        private final String command;

        public Treasure(String id, Location location, String command) {
            this.id = id;
            this.location = location;
            this.command = command;
        }

        public String getId() {
            return id;
        }

        public Location getLocation() {
            return location;
        }

        public String getCommand() {
            return command;
        }
    }
}
