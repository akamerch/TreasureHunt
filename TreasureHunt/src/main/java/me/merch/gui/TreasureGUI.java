package me.merch.gui;

import me.merch.TreasureHunt;
import me.merch.storage.TreasureStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class TreasureGUI implements Listener {

    private final TreasureHunt plugin;

    public TreasureGUI(TreasureHunt plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&6Treasure List"));

        int size = plugin.getConfig().getInt("gui.size", 54);
        if (size % 9 != 0 || size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection itemsSec = plugin.getConfig().getConfigurationSection("gui.items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection sec = itemsSec.getConfigurationSection(key);
                if (sec == null) continue;

                String matName = sec.getString("material", "BARRIER");
                Material mat = Material.matchMaterial(matName);
                if (mat == null) mat = Material.BARRIER;

                ItemStack custom = new ItemStack(mat);
                ItemMeta meta = custom.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                            sec.getString("name", "&cCustom Item")));
                    List<String> lore = sec.getStringList("lore");
                    if (lore != null && !lore.isEmpty()) {
                        meta.setLore(ChatColor.translateAlternateColorCodes('&',
                                String.join("\n", lore)).lines().toList());
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    custom.setItemMeta(meta);
                }

                int slot = sec.getInt("slot", -1);
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, custom);
                }
            }
        }

        Collection<TreasureStorage.Treasure> treasures = plugin.getTreasureStorage().getTreasures().values();
        for (TreasureStorage.Treasure treasure : treasures) {
            ItemStack tItem = createTreasureItem(treasure.getId());
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    inv.setItem(i, tItem);
                    break;
                }
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createTreasureItem(String treasureId) {
        String matName = plugin.getConfig().getString("gui.treasure-item.material", "EMERALD");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.EMERALD;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String display = plugin.getConfig().getString("gui.treasure-item.name", "&aTreasure %id%");
            display = display.replace("%id%", treasureId);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', display));

            List<String> loreCfg = plugin.getConfig().getStringList("gui.treasure-item.lore");
            if (loreCfg != null && !loreCfg.isEmpty()) {
                var lore = loreCfg.stream()
                        .map(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%id%", treasureId)))
                        .toList();
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&6Treasure List"));

        if (!Objects.equals(event.getView().getTitle(), title)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        ItemMeta meta = current.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String clickedName = ChatColor.stripColor(meta.getDisplayName()).trim();

        String closeCfgRaw = plugin.getConfig().getString("gui.items.close.name", "&cClose Menu");
        String closeCfg = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', closeCfgRaw)).trim();
        if (closeCfg.equalsIgnoreCase(clickedName)) {
            player.closeInventory();
            return;
        }

        TreasureStorage storage = plugin.getTreasureStorage();
        for (String id : storage.getTreasures().keySet()) {
            String cfgDisplayRaw = plugin.getConfig().getString("gui.treasure-item.name", "&aTreasure %id%");
            String expected = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cfgDisplayRaw.replace("%id%", id))).trim();
            if (expected.equalsIgnoreCase(clickedName)) {
                TreasureStorage.Treasure treasure = storage.getTreasure(id);
                if (treasure == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.treasure-not-found", "&cTreasure not found!")));
                    return;
                }

                if (event.getClick() == ClickType.LEFT) {
                    player.teleport(treasure.getLocation().add(0.5, 1.0, 0.5));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.teleport-to-treasure", "&aTeleported to treasure %id%").replace("%id%", id)));
                } else if (event.getClick() == ClickType.RIGHT) {
                    storage.removeTreasure(id);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.treasure-deleted", "&cTreasure successfully deleted!")));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&6Treasure List"));
        if (!Objects.equals(e.getView().getTitle(), title)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
    }
}
