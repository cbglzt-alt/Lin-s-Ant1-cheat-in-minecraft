package dev.lin.antiaim.record;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.io.IOException;

/**
 * Fallback attack recorder. PacketEvents InteractEntity was not observed on the
 * Phase 1 Spigot 1.21 capture host (attacks=0 while swings>0). Damage events still fire.
 */
public final class BukkitAttackListener implements Listener {
    private final CaptureService captures;

    public BukkitAttackListener(CaptureService captures) {
        this.captures = captures;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        CaptureService.ActiveCapture capture = captures.get(attacker);
        if (capture == null) {
            return;
        }
        Entity target = event.getEntity();
        float yaw = capture.hasLook ? capture.lastYaw : attacker.getLocation().getYaw();
        float pitch = capture.hasLook ? capture.lastPitch : attacker.getLocation().getPitch();
        try {
            capture.writeRaw(JsonLine.attack(capture, attacker, target, yaw, pitch));
            capture.attackCount++;
        } catch (IOException e) {
            attacker.getServer().getLogger().warning("[LinAntiAim] attack write failed: " + e.getMessage());
        }
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
