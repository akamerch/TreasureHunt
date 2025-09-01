package me.merch.commands;

import me.merch.TreasureHunt;
import me.merch.gui.TreasureGUI;
import me.merch.storage.TreasureStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class TreasureCommand implements CommandExecutor {

    private final TreasureHunt plugin;
    private String creatingTreasureId = null;
    private String creatingCommand = null;

    public TreasureCommand(TreasureHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("treasure.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        TreasureStorage storage = plugin.getTreasureStorage();

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 3) {
                    sendUsage(player);
                    return true;
                }
                creatingTreasureId = args[1];
                creatingCommand = String.join(" ", args).replace(args[0] + " " + args[1] + " ", "");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.click-to-create")));
                return true;

            case "delete":
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                if (storage.getTreasures().containsKey(args[1])) {
                    storage.removeTreasure(args[1]);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.treasure-deleted")));
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.treasure-not-found")));
                }
                return true;

            case "list":
                if (storage.getTreasures().isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No treasures found.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Treasure List:");
                    storage.getTreasures().keySet().forEach(id ->
                            player.sendMessage(ChatColor.AQUA + "- " + id)
                    );
                }
                return true;

            case "completed":
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                String tid = args[1];
                Map<UUID, Integer> completed = storage.getCompletedWithCounts(tid);
                if (completed.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.no-players-found")));
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.players-found")
                                    .replace("%treasure%", tid)));
                    for (Map.Entry<UUID, Integer> entry : completed.entrySet()) {
                        player.sendMessage(ChatColor.AQUA + "- " +
                                Bukkit.getOfflinePlayer(entry.getKey()).getName() +
                                ChatColor.YELLOW + " x" + entry.getValue());
                    }
                }
                return true;

            case "gui":
                TreasureGUI gui = plugin.getTreasureGUI();
                gui.open(player);
                return true;

            case "help":
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.help-title")));
                for (String line : plugin.getConfig().getStringList("messages.help-commands")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
                return true;

            default:
                sendUsage(player);
                return true;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.usage")));
    }

    public String getCreatingTreasureId() {
        return creatingTreasureId;
    }

    public String getCreatingCommand() {
        return creatingCommand;
    }

    public void resetCreation() {
        creatingTreasureId = null;
        creatingCommand = null;
    }
}
