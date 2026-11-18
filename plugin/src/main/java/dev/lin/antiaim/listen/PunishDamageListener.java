package dev.lin.antiaim.listen;

import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.punish.PunishmentService;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * High tier: block outgoing damage while the player's cancel window is active.
 */
public final class PunishDamageListener implements Listener {
    private final PlayerDataService players;
    private final PunishmentService punishment;

    public PunishDamageListener(PlayerDataService players, PunishmentService punishment) {
        this.players = players;
        this.punishment = punishment;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!punishment.damageCancelEnabled()) {
            return;
        }
        Player attacker = resolve(event);
        if (attacker == null) {
            return;
        }
        PlayerData data = players.get(attacker);
        if (data == null || !data.damageCancelled(System.currentTimeMillis())) {
            return;
        }
        event.setCancelled(true);
    }

    private static Player resolve(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            return p;
        }
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
