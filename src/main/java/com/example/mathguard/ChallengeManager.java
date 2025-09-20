package com.example.mathguard;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ChallengeManager {
    private final MathGuardPlugin plugin;
    private final Random random = new Random();

    static class Pending {
        final MathChallenge challenge;
        final Runnable openOriginal;   // öffnet nach Lösung die echte Kiste / Workbench
        Integer tens = null;           // null = noch keine Zehner-Eingabe

        Pending(MathChallenge ch, Runnable openOriginal) {
            this.challenge = ch;
            this.openOriginal = openOriginal;
        }
    }

    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Set<UUID> guiView = new HashSet<>();

    public ChallengeManager(MathGuardPlugin plugin) { this.plugin = plugin; }

    public boolean hasPending(Player p) { return pending.containsKey(p.getUniqueId()); }

    public void startChallenge(Player p, Runnable openOriginal) {
        MathChallenge ch = MathChallenge.random(random, plugin.getConfig());
        pending.put(p.getUniqueId(), new Pending(ch, openOriginal));
        openGuiTens(p); // Start mit Zehner
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
    }

    /* ========= GUI Phasen ========= */

    private void openGuiTens(Player p) {
        Pending pd = pending.get(p.getUniqueId());
        if (pd == null) return;
        pd.tens = null;
        String title = plugin.color("title-colors.question") + "Aufgabe: " + pd.challenge.display()
                + "   " + plugin.color("title-colors.input") + "Eingabe: __";
        Inventory inv = Bukkit.createInventory(p, 27, title);
        fillDigits(inv, "Zehner");
        inv.setItem(22, named(Material.BARRIER, "§cZurücksetzen"));
        inv.setItem(4, named(Material.BOOK, "§aLöse: " + pd.challenge.display()));
        guiView.add(p.getUniqueId());
        p.openInventory(inv);
    }

    private void openGuiOnes(Player p) {
        Pending pd = pending.get(p.getUniqueId());
        if (pd == null) return;
        if (pd.tens == null) { openGuiTens(p); return; }
        String title = plugin.color("title-colors.question") + "Aufgabe: " + pd.challenge.display()
                + "   " + plugin.color("title-colors.input") + "Eingabe: " + pd.tens + " _";
        Inventory inv = Bukkit.createInventory(p, 27, title);
        fillDigits(inv, "Einer");
        inv.setItem(22, named(Material.BARRIER, "§cZurücksetzen"));
        inv.setItem(4, named(Material.BOOK, "§aZehner: " + pd.tens + " – wähle Einer"));
        guiView.add(p.getUniqueId());
        p.openInventory(inv);
    }

    private void fillDigits(Inventory inv, String phaseLabel) {
        for (int d = 0; d <= 9; d++) {
            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName("§b" + d);
            m.setLore(List.of("§7Klicke, um " + phaseLabel + " zu setzen"));
            it.setItemMeta(m);
            inv.setItem(9 + d, it);
        }
    }

    private ItemStack named(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }

    /* ========= Events ========= */

    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!guiView.contains(p.getUniqueId())) return;

        // Nichts verschieben/nehmen – auch im unteren Inventar
        e.setCancelled(true);

        ItemStack cur = e.getCurrentItem();
        if (cur == null || !cur.hasItemMeta()) return;

        Pending pd = pending.get(p.getUniqueId());
        if (pd == null) return;

        Material mat = cur.getType();
        String name = cur.getItemMeta().getDisplayName();

        if (mat == Material.BARRIER) {
            // Eingabe zurücksetzen, Aufgabe bleibt – hartes Reopen nötig
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> openGuiTens(p));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
            return;
        }

        if (mat == Material.PAPER && name != null && name.startsWith("§b")) {
            int digit = Integer.parseInt(name.substring(2));
            int answer = pd.challenge.getResult();

            // Einstellig: direkt prüfen
            if (answer < 10 && pd.tens == null) {
                if (digit == answer) solve(p, pd);
                else {
                    failOnce(p);
                    p.closeInventory();
                    Bukkit.getScheduler().runTask(plugin, () -> openGuiTens(p));
                }
                return;
            }

            // Zehner-Phase -> zu Einer-Phase wechseln
            if (pd.tens == null) {
                pd.tens = digit;
                p.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> openGuiOnes(p));
                return;
            }

            // Einer-Phase -> prüfen
            int guess = pd.tens * 10 + digit;
            if (guess == answer) {
                solve(p, pd);
            } else {
                failOnce(p);
                p.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> openGuiTens(p));
            }
        }
    }

    public void handleDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!guiView.contains(p.getUniqueId())) return;
        e.setCancelled(true); // Drag komplett blocken
    }

    public void handleClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        // Beim Schließen: Aufgabe bleibt pending, nur GUI-Flag löschen
        guiView.remove(p.getUniqueId());
        // pd.tens NICHT zurücksetzen – sonst ginge Phase 2 verloren
    }

    /* ========= Helpers ========= */

    public void reopen(Player p) {
        Pending pd = pending.get(p.getUniqueId());
        if (pd == null) return;
        if (pd.tens == null) openGuiTens(p);
        else openGuiOnes(p);
    }

    private void solve(Player p, Pending pd) {
        guiView.remove(p.getUniqueId());
        pending.remove(p.getUniqueId());
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        giveRewards(p);
        Bukkit.getScheduler().runTask(plugin, pd.openOriginal);
    }

    private void failOnce(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.6f);
        maybeSpawnPenaltyMob(p);
    }

    private void giveRewards(Player p) {
        var c = plugin.getConfig();

        if (c.getBoolean("rewards.exp.enabled", true)) {
            int xp = c.getInt("rewards.exp.amount", 3);
            p.giveExp(xp);
        }

        if (c.getBoolean("rewards.item.enabled", false)) {
            String type = c.getString("rewards.item.type", "COOKED_BEEF");
            int amt = c.getInt("rewards.item.amount", 1);
            try {
                p.getWorld().dropItemNaturally(p.getLocation(),
                        new ItemStack(Material.valueOf(type), Math.max(1, amt)));
            } catch (IllegalArgumentException ignored) {}
        }

        if (plugin.hasEconomy() && c.getBoolean("rewards.vault.enabled", false)) {
            double money = c.getDouble("rewards.vault.amount", 1.0);
            plugin.getEconomy().depositPlayer(p, money);
        }
    }

    private void maybeSpawnPenaltyMob(Player p) {
        var sec = plugin.getConfig().getConfigurationSection("penalties.mob_on_wrong");
        if (sec == null || !sec.getBoolean("enabled", false)) return;
        if (Math.random() > sec.getDouble("chance", 1.0)) return;

        String type = sec.getString("type", "SILVERFISH");
        int cnt = sec.getInt("count", 1);
        try {
            var t = org.bukkit.entity.EntityType.valueOf(type);
            for (int i = 0; i < cnt; i++) p.getWorld().spawnEntity(p.getLocation(), t);
        } catch (IllegalArgumentException ignored) {}
    }
}
