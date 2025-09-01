package me.merch.listeners;

import me.merch.TreasureHunt;
import me.merch.commands.TreasureCommand;
import me.merch.storage.TreasureStorage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockClickListener implements Listener {

    private final TreasureHunt plugin;
    private final TreasureCommand treasureCommand;
    private final HashMap<Location, BukkitRunnable> particleTasks = new HashMap<>();

    public BlockClickListener(TreasureHunt plugin, TreasureCommand treasureCommand) {
        this.plugin = plugin;
        this.treasureCommand = treasureCommand;
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        TreasureStorage storage = plugin.getTreasureStorage();

        if (treasureCommand.getCreatingTreasureId() != null && treasureCommand.getCreatingCommand() != null) {

            if (action == Action.LEFT_CLICK_BLOCK && player.getGameMode() == GameMode.CREATIVE) {
                event.setCancelled(true);
            }

            TreasureStorage.Treasure treasure = new TreasureStorage.Treasure(
                    treasureCommand.getCreatingTreasureId(),
                    block.getLocation(),
                    treasureCommand.getCreatingCommand()
            );
            storage.addTreasure(treasure);
            treasureCommand.resetCreation();

            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.treasure-created")));

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (block.getType().isAir()) {
                        cancel();
                        return;
                    }
                    try {
                        block.getWorld().spawnParticle(Particle.valueOf("HAPPY_VILLAGER"),
                                block.getLocation().add(0.5, 1, 0.5), 20, 0.5, 0.5, 0.5, 0);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            };
            task.runTaskTimer(plugin, 0L, 20L);
            particleTasks.put(block.getLocation(), task);

            return;
        }

        for (TreasureStorage.Treasure treasure : storage.getTreasures().values()) {
            if (!treasure.getLocation().equals(block.getLocation())) continue;

            Map<UUID, Integer> claimedMap = storage.getCompletedWithCounts(treasure.getId());
            if (claimedMap.containsKey(player.getUniqueId())) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.treasure-already-claimed")));
                return;
            }

            storage.claimTreasure(treasure.getId(), player.getUniqueId());

            String cmdText = treasure.getCommand().replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdText);

            storage.removeTreasure(treasure.getId());

            if (particleTasks.containsKey(block.getLocation())) {
                particleTasks.get(block.getLocation()).cancel();
                particleTasks.remove(block.getLocation());
            }

            block.setType(org.bukkit.Material.AIR);

            Firework fw = block.getWorld().spawn(block.getLocation().add(0.5, 1, 0.5), Firework.class);
            FireworkMeta fwm = fw.getFireworkMeta();
            fwm.addEffect(org.bukkit.FireworkEffect.builder()
                    .withColor(org.bukkit.Color.GREEN)
                    .withFade(org.bukkit.Color.WHITE)
                    .with(org.bukkit.FireworkEffect.Type.BALL)
                    .build());
            fw.setFireworkMeta(fwm);
            try {
                block.getWorld().spawnParticle(Particle.valueOf("HAPPY_VILLAGER"),
                        block.getLocation().add(0.5, 1, 0.5), 40, 0.5, 0.5, 0.5, 0);
            } catch (IllegalArgumentException ignored) {
            }

            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.treasure-claimed")));
            return;
        }
    }
}
