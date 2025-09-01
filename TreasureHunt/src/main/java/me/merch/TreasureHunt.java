package me.merch;

import me.merch.commands.TreasureCommand;
import me.merch.commands.TreasureTabCompleter;
import me.merch.gui.TreasureGUI;
import me.merch.listeners.BlockClickListener;
import me.merch.storage.DatabaseManager;
import me.merch.storage.TreasureStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TreasureHunt extends JavaPlugin {

    private static TreasureHunt instance;
    private DatabaseManager databaseManager;
    private TreasureStorage treasureStorage;
    private TreasureCommand treasureCommand;
    private TreasureGUI treasureGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Database
        if (getConfig().getBoolean("database.enabled")) {
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();
        }

        // Storage
        treasureStorage = new TreasureStorage(this);

        // GUI
        treasureGUI = new TreasureGUI(this);

        // Commands
        treasureCommand = new TreasureCommand(this);
        getCommand("treasure").setExecutor(treasureCommand);
        getCommand("treasure").setTabCompleter(new TreasureTabCompleter());

        // Listener
        Bukkit.getPluginManager().registerEvents(new BlockClickListener(this, treasureCommand), this);

        getLogger().info("TreasureHunt v1.0 enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("TreasureHunt v1.0 disabled!");
    }

    public static TreasureHunt getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TreasureStorage getTreasureStorage() {
        return treasureStorage;
    }

    public TreasureCommand getTreasureCommand() {
        return treasureCommand;
    }

    public TreasureGUI getTreasureGUI() {
        return treasureGUI;
    }
}
