package dev.lin.antiaim.check;

import dev.lin.antiaim.alert.AlertLevel;
import dev.lin.antiaim.alert.AlertService;
import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.punish.PunishmentService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AimAttackAlign — A1 legit mean10≈11.9° vs B1 Always mean10≈4.2°.
 * Alert when recent attack-align window is persistently too tight.
 */
public final class AimAttackAlignCheck {
    private final AlertService alerts;
    private final PunishmentService punishment;
    private final boolean enabled;
    private final boolean debugOnly;
    private final int window;
    private final int minSamples;
    private final double meanMaxDeg;
    private final double tightDeg;
    private final int tightMinCount;
    private final int streakToAlert;
    private final long alertCooldownMs;
    private final long decayMs;

    public AimAttackAlignCheck(AlertService alerts, PunishmentService punishment, FileConfiguration config) {
        this.alerts = alerts;
        this.punishment = punishment;
        this.enabled = config.getBoolean("checks.align.enabled", true);
        this.debugOnly = config.getBoolean("checks.align.debug_only", false);
        this.window = Math.max(5, config.getInt("checks.align.window", 10));
        this.minSamples = Math.max(5, config.getInt("checks.align.min_samples", 10));
        this.meanMaxDeg = config.getDouble("checks.align.mean_max_deg", 8.0);
        this.tightDeg = config.getDouble("checks.align.tight_deg", 8.0);
        this.tightMinCount = Math.max(1, config.getInt("checks.align.tight_min_count", 6));
        this.streakToAlert = Math.max(1, config.getInt("checks.align.streak_to_alert", 2));
        this.alertCooldownMs = config.getLong("checks.align.alert_cooldown_ms", 3000L);
        this.decayMs = config.getLong("checks.align.decay_ms", 8000L);
    }

    public int windowSize() {
        return window;
    }

    public boolean enabled() {
        return enabled;
    }

    public void onAttack(Player player, PlayerData data, long t, int targetId, double alignDeg) {
        if (!enabled) {
            return;
        }
        data.pushAlign(t, targetId, alignDeg, window);
        decay(data, t);

        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        int tight = 0;
        for (PlayerData.AttackSample s : data.alignWindow()) {
            sum += s.alignDeg();
            min = Math.min(min, s.alignDeg());
            if (s.alignDeg() < tightDeg) {
                tight++;
            }
        }
        int n = data.alignWindow().size();
        double mean = sum / n;

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("alignDeg", round3(alignDeg));
        fields.put("mean", round3(mean));
        fields.put("min", round3(min));
        fields.put("tight", tight);
        fields.put("n", n);
        fields.put("targetId", targetId);

        if (debugOnly) {
            alerts.emit(player, "AimAttackAlign", AlertLevel.DEBUG,
                    String.format(Locale.ROOT, "align=%.2f° mean=%.2f° tight=%d/%d", alignDeg, mean, tight, n),
                    fields);
            return;
        }

        if (n < minSamples) {
            return;
        }

        boolean trigger = mean <= meanMaxDeg && tight >= tightMinCount;
        if (trigger) {
            data.setAlignStreak(data.alignStreak() + 1);
            data.addAlignVl(1);
        } else {
            data.setAlignStreak(0);
        }

        if (data.alignStreak() >= streakToAlert
                && t - data.lastAlignAlertMs() >= alertCooldownMs) {
            data.setLastAlignAlertMs(t);
            fields.put("vl", data.alignVl());
            fields.put("streak", data.alignStreak());
            fields.put("totalVl", data.totalVl());
            alerts.emit(player, "AimAttackAlign", AlertLevel.MEDIUM,
                    String.format(Locale.ROOT,
                            "vl=%d streak=%d mean=%.2f° tight=%d/%d",
                            data.alignVl(), data.alignStreak(), mean, tight, n),
                    fields);
            punishment.addVl(player, data, 2, "AimAttackAlign",
                    String.format(Locale.ROOT, "mean=%.2f° tight=%d/%d", mean, tight, n));
        }
    }

    private void decay(PlayerData data, long now) {
        if (data.lastAttackMs() > 0 && now - data.lastAttackMs() >= decayMs) {
            data.setAlignVl(0);
            data.setAlignStreak(0);
        }
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
