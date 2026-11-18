package dev.lin.antiaim.listen;

import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.record.AttackProbe;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Bukkit fallbacks: damage (incl. cancelled) + interact-entity.
 */
public final class CombatBukkitListener implements Listener {
    private final PlayerDataService players;
    private final AttackProbe attacks;

    public CombatBukkitListener(PlayerDataService players, AttackProbe attacks) {
        this.players = players;
        this.attacks = attacks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        Entity target = event.getEntity();
        PlayerData data = players.get(attacker);
        boolean damaged = !event.isCancelled() && event.getFinalDamage() > 0.0;
        attacks.onAttack(attacker, data, target, target.getEntityId(), damaged, "damage", 80L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // Right-click path; left-click attacks usually go through damage/packet/swing_ray.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player attacker = event.getPlayer();
        Entity target = event.getRightClicked();
        PlayerData data = players.get(attacker);
        attacks.onAttack(attacker, data, target, target.getEntityId(), false, "bukkit_interact", 80L);
    }

    private static Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
