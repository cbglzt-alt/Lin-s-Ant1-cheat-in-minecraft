package dev.lin.antiaim.listen;

import dev.lin.antiaim.check.AimAttackAlignCheck;
import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.record.CaptureService;
import dev.lin.antiaim.util.AimMath;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.io.IOException;

/**
 * Damage-based attack fallback (PacketEvents InteractEntity may be missing on some hosts).
 */
public final class CombatBukkitListener implements Listener {
    private final CaptureService captures;
    private final PlayerDataService players;
    private final AimAttackAlignCheck align;
    private final boolean recorderEnabled;

    public CombatBukkitListener(CaptureService captures, PlayerDataService players,
                                AimAttackAlignCheck align, boolean recorderEnabled) {
        this.captures = captures;
        this.players = players;
        this.align = align;
        this.recorderEnabled = recorderEnabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        Entity target = event.getEntity();
        PlayerData data = players.get(attacker);
        long now = System.currentTimeMillis();
        int id = target.getEntityId();
        if (data.shouldDedupAttack(now, id, 50L)) {
            return;
        }

        float yaw = data.hasLook() ? data.lastYaw() : attacker.getLocation().getYaw();
        float pitch = data.hasLook() ? data.lastPitch() : attacker.getLocation().getPitch();
        double alignDeg = AimMath.alignDeg(attacker, target, yaw, pitch);

        if (align.enabled() && alignDeg >= 0) {
            align.onAttack(attacker, data, now, id, alignDeg);
        } else {
            data.markCombat(now);
        }

        if (!recorderEnabled) {
            return;
        }
        CaptureService.ActiveCapture capture = captures.get(attacker);
        if (capture == null) {
            return;
        }
        try {
            capture.writeAttack(attacker, target, yaw, pitch);
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
