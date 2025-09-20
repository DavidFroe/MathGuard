package com.example.mathguard;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class MathGuardPlugin extends JavaPlugin implements Listener {
    private ChallengeManager manager;
    private Set<Material> guardedBlocks;
    private Set<String> enabledWorlds;
    private Economy economy; // optional

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocal();
        manager = new ChallengeManager(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        setupEconomy();

        if (getCommand("mathguard") != null) {
            getCommand("mathguard").setExecutor((sender, cmd, label, args) -> {
                reloadConfig();
                reloadLocal();
                sender.sendMessage("§aMathGuard reloaded.");
                return true;
            });
        }
    }

    private void reloadLocal() {
        FileConfiguration c = getConfig();
        enabledWorlds = new HashSet<>(c.getStringList("enabled-worlds"));
        guardedBlocks = new HashSet<>();
        for (String s : c.getStringList("block-types")) {
            try { guardedBlocks.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
    }

    public String color(String path) { return getConfig().getString(path, ""); }

    public boolean hasEconomy() { return economy != null; }
    public Economy getEconomy() { return economy; }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return;
        var rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            getLogger().info("Vault Economy erkannt.");
        }
    }

    private boolean shouldAsk() {
        double r = Math.random();
        return r < getConfig().getDouble("rarity", 1.0);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        if (p.hasPermission("mathguard.bypass")) return;

        Block b = e.getClickedBlock();
        if (!enabledWorlds.contains(b.getWorld().getName())) return;
        if (!guardedBlocks.contains(b.getType())) return;

        switch (e.getAction()) {
            case RIGHT_CLICK_BLOCK -> {
                if (!shouldAsk()) return;
                e.setCancelled(true);

                Runnable openOriginal = () -> {
                    if (b.getType() == Material.CRAFTING_TABLE) {
                        p.openWorkbench(b.getLocation(), true);
                    } else if (b.getState() instanceof Container c) {
                        Inventory inv = c.getInventory();
                        p.openInventory(inv);
                    }
                };

                if (!manager.hasPending(p)) manager.startChallenge(p, openOriginal);
                else manager.reopen(p);
            }
            default -> {}
        }
    }

    @EventHandler public void onInvClick(InventoryClickEvent e) { manager.handleClick(e); }
    @EventHandler public void onInvDrag(InventoryDragEvent e) { manager.handleDrag(e); }
    @EventHandler public void onInvClose(InventoryCloseEvent e) { manager.handleClose(e); }
}
