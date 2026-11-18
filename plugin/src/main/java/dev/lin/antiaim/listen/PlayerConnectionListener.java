package dev.lin.antiaim.listen;

import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.punish.PunishmentService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerConnectionListener implements Listener {
    private final PlayerDataService players;
    private final PunishmentService punishment;
    private final long teleportExemptMs;

    public PlayerConnectionListener(JavaPlugin plugin, PlayerDataService players, PunishmentService punishment) {
        this.players = players;
        this.punishment = punishment;
        this.teleportExemptMs = plugin.getConfig().getLong("checks.aimsnap.teleport_exempt_ms", 2000L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = players.get(event.getPlayer());
        punishment.restore(data);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerData data = players.get(event.getPlayer().getUniqueId());
        if (data != null) {
            punishment.persist(data);
        }
        players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }
        PlayerData data = players.get(event.getPlayer());
        data.exemptUntil(System.currentTimeMillis() + teleportExemptMs);
        data.setAimSnapStreak(0);
    }
}
