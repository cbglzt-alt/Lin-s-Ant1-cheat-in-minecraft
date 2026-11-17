package dev.lin.antiaim.check;

import dev.lin.antiaim.alert.AlertLevel;
import dev.lin.antiaim.alert.AlertService;
import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.util.AimMath;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provisional AimSnap — see docs/detections/aim-provisional.md.
 * Alert-only; does not cancel damage or kick.
 */
public final class AimSnapCheck {
    private final AlertService alerts;
    private final boolean enabled;
    private final int window;
    private final double bigYawDeg;
    private final double bigRatio;
    private final double p90Deg;
    private final int streakToAlert;
    private final long decayNoCombatMs;
    private final long alertCooldownMs;
    private final int evalEvery;

    public AimSnapCheck(AlertService alerts, FileConfiguration config) {
        this.alerts = alerts;
        this.enabled = config.getBoolean("checks.aimsnap.enabled", true);
        this.window = Math.max(10, config.getInt("checks.aimsnap.window", 40));
        this.bigYawDeg = config.getDouble("checks.aimsnap.big_yaw_deg", 20.0);
        this.bigRatio = config.getDouble("checks.aimsnap.big_ratio", 0.12);
        this.p90Deg = config.getDouble("checks.aimsnap.p90_deg", 25.0);
        this.streakToAlert = Math.max(1, config.getInt("checks.aimsnap.streak_to_alert", 3));
        this.decayNoCombatMs = config.getLong("checks.aimsnap.decay_no_combat_ms", 5000L);
        this.alertCooldownMs = config.getLong("checks.aimsnap.alert_cooldown_ms", 3000L);
        this.evalEvery = Math.max(1, config.getInt("checks.aimsnap.eval_every", 10));
    }

    public int windowSize() {
        return window;
    }

    public boolean enabled() {
        return enabled;
    }

    public void onRotSample(Player player, PlayerData data, long now) {
        if (!enabled) {
            return;
        }
        if (data.isExempt(now)) {
            return;
        }
        decay(data, now);
        if (data.rotWindow().size() < window) {
            return;
        }
        if (data.rotSinceWindowEval() < evalEvery) {
            return;
        }
        data.resetRotSinceWindowEval();

        double[] abs = new double[data.rotWindow().size()];
        int i = 0;
        int big = 0;
        for (PlayerData.LookSample s : data.rotWindow()) {
            abs[i++] = s.absDYaw();
            if (s.absDYaw() >= bigYawDeg) {
                big++;
            }
        }
        java.util.Arrays.sort(abs);
        double ratio = big / (double) abs.length;
        double p90 = AimMath.percentile(abs, 0.90);
        boolean trigger = ratio >= bigRatio || p90 >= p90Deg;

        if (trigger) {
            data.markCombat(now);
            data.setAimSnapStreak(data.aimSnapStreak() + 1);
            data.addAimSnapVl(1);
        } else {
            data.setAimSnapStreak(0);
        }

        if (data.aimSnapStreak() >= streakToAlert) {
            if (now - data.lastSnapAlertMs() >= alertCooldownMs) {
                data.setLastSnapAlertMs(now);
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("vl", data.aimSnapVl());
                fields.put("streak", data.aimSnapStreak());
                fields.put("bigRatio", round3(ratio));
                fields.put("p90", round3(p90));
                fields.put("window", abs.length);
                alerts.emit(player, "AimSnap", AlertLevel.MEDIUM,
                        String.format(java.util.Locale.ROOT,
                                "vl=%d streak=%d bigRatio=%.1f%% p90=%.1f°",
                                data.aimSnapVl(), data.aimSnapStreak(), ratio * 100.0, p90),
                        fields);
            }
        }
    }

    private void decay(PlayerData data, long now) {
        if (data.lastCombatMs() <= 0) {
            return;
        }
        if (now - data.lastCombatMs() >= decayNoCombatMs) {
            data.setAimSnapVl(0);
            data.setAimSnapStreak(0);
        }
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
