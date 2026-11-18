package dev.lin.antiaim.check;

import dev.lin.antiaim.alert.AlertLevel;
import dev.lin.antiaim.alert.AlertService;
import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.punish.PunishmentService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * AuraSwitch — must separate a legit sword sweep from aura.
 *
 * A normal sweep hits several mobs in one swing; their alignDeg is small because
 * they all stand in front of the player. A KillAura switches to targets far
 * outside the look direction, so the same-time cluster contains at least one
 * attack with a large alignDeg.
 *
 * Capture check (cluster 120ms, >=3 distinct, wild >=60°):
 *   A1/B1/B2/C2 -> 0 triggers; C1 -> 25 trigger clusters.
 */
public final class AuraSwitchCheck {
    private final AlertService alerts;
    private final PunishmentService punishment;
    private final boolean enabled;
    private final long clusterMs;
    private final int minDistinct;
    private final double wildAlignDeg;
    private final int streakToAlert;
    private final long alertCooldownMs;
    private final long decayMs;

    public AuraSwitchCheck(AlertService alerts, PunishmentService punishment, FileConfiguration config) {
        this.alerts = alerts;
        this.punishment = punishment;
        this.enabled = config.getBoolean("checks.aura_switch.enabled", true);
        this.clusterMs = config.getLong("checks.aura_switch.cluster_ms", 120L);
        this.minDistinct = Math.max(2, config.getInt("checks.aura_switch.min_distinct", 3));
        this.wildAlignDeg = config.getDouble("checks.aura_switch.wild_align_deg", 60.0);
        this.streakToAlert = Math.max(1, config.getInt("checks.aura_switch.streak_to_alert", 2));
        this.alertCooldownMs = config.getLong("checks.aura_switch.alert_cooldown_ms", 3000L);
        this.decayMs = config.getLong("checks.aura_switch.decay_ms", 8000L);
    }

    public boolean enabled() {
        return enabled;
    }

    public void onAttack(Player player, PlayerData data, long t, int targetId, double alignDeg) {
        if (!enabled) {
            return;
        }
        decay(data, t);

        Set<Integer> ids = new HashSet<>();
        double wildest = 0;
        for (PlayerData.AttackSample s : data.alignWindow()) {
            if (t - s.t() <= clusterMs) {
                ids.add(s.targetId());
                wildest = Math.max(wildest, s.alignDeg());
            }
        }
        int distinct = ids.size();
        boolean trigger = distinct >= minDistinct && wildest >= wildAlignDeg;

        if (trigger) {
            data.setAuraStreak(data.auraStreak() + 1);
            data.addAuraVl(1);
        } else {
            data.setAuraStreak(0);
        }

        if (data.auraStreak() >= streakToAlert
                && t - data.lastAuraAlertMs() >= alertCooldownMs) {
            data.setLastAuraAlertMs(t);
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("vl", data.auraVl());
            fields.put("streak", data.auraStreak());
            fields.put("distinct", distinct);
            fields.put("wildAlignDeg", round2(wildest));
            fields.put("clusterMs", clusterMs);
            fields.put("totalVl", data.totalVl());
            alerts.emit(player, "AuraSwitch", AlertLevel.MEDIUM,
                    String.format(Locale.ROOT,
                            "vl=%d streak=%d distinct=%d/%dms wild=%.1f°",
                            data.auraVl(), data.auraStreak(), distinct, clusterMs, wildest),
                    fields);
            punishment.addVl(player, data, 2, "AuraSwitch",
                    String.format(Locale.ROOT, "distinct=%d wild=%.1f°", distinct, wildest));
        }
    }

    private void decay(PlayerData data, long now) {
        if (data.lastAttackMs() > 0 && now - data.lastAttackMs() >= decayMs) {
            data.setAuraVl(0);
            data.setAuraStreak(0);
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
