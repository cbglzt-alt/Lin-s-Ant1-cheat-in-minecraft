package dev.lin.antiaim.record;

import dev.lin.antiaim.check.AimAttackAlignCheck;
import dev.lin.antiaim.check.AuraSwitchCheck;
import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.util.AimMath;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Shared attack intake for packet / damage / swing-ray sources.
 */
public final class AttackProbe {
    private final CaptureService captures;
    private final AimAttackAlignCheck align;
    private final AuraSwitchCheck auraSwitch;
    private final boolean recorderEnabled;
    private final Logger log;

    public AttackProbe(CaptureService captures, AimAttackAlignCheck align, AuraSwitchCheck auraSwitch,
                       boolean recorderEnabled, Logger log) {
        this.captures = captures;
        this.align = align;
        this.auraSwitch = auraSwitch;
        this.recorderEnabled = recorderEnabled;
        this.log = log;
    }

    public void onAttack(Player attacker, PlayerData data, Entity target, int targetId,
                         boolean damaged, String source, long dedupMs) {
        long now = System.currentTimeMillis();
        if (data.shouldDedupAttack(now, targetId, dedupMs)) {
            return;
        }

        float yaw = data.hasLook() ? data.lastYaw() : attacker.getLocation().getYaw();
        float pitch = data.hasLook() ? data.lastPitch() : attacker.getLocation().getPitch();
        double alignDeg = target != null ? AimMath.alignDeg(attacker, target, yaw, pitch) : -1.0;

        data.markAttack(now);
        if (alignDeg >= 0) {
            if (align.enabled()) {
                align.onAttack(attacker, data, now, targetId, alignDeg);
            } else {
                data.pushAlign(now, targetId, alignDeg, Math.max(10, align.windowSize()));
            }
            if (auraSwitch.enabled()) {
                auraSwitch.onAttack(attacker, data, now, targetId, alignDeg);
            }
        }

        if (!recorderEnabled) {
            return;
        }
        CaptureService.ActiveCapture capture = captures.get(attacker);
        if (capture == null) {
            return;
        }
        try {
            if (target == null) {
                capture.writeAttackUnknown(now, yaw, pitch, targetId, damaged, source);
            } else {
                capture.writeAttack(attacker, target, yaw, pitch, damaged, source);
            }
            log.info("attack recorded player=" + attacker.getName()
                    + " source=" + source
                    + " damaged=" + damaged
                    + " target=" + (target != null ? target.getType().name() : ("id:" + targetId))
                    + " align=" + (alignDeg >= 0 ? String.format(java.util.Locale.ROOT, "%.2f", alignDeg) : "n/a"));
        } catch (IOException e) {
            log.warning("attack write failed: " + e.getMessage());
        }
    }

    /** Ray-trace from eye along look; used when swing fires but InteractEntity is missing. */
    public static Entity rayTarget(Player player, double range) {
        Vector dir = player.getEyeLocation().getDirection();
        RayTraceResult hit = player.getWorld().rayTrace(
                player.getEyeLocation(),
                dir,
                range,
                FluidCollisionMode.NEVER,
                true,
                0.0,
                entity -> entity != player && entity instanceof LivingEntity && entity.isValid()
        );
        if (hit == null) {
            return null;
        }
        return hit.getHitEntity();
    }
}
