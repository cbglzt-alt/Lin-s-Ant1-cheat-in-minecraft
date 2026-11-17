package dev.lin.antiaim.check;

import dev.lin.antiaim.alert.AlertLevel;
import dev.lin.antiaim.alert.AlertService;
import dev.lin.antiaim.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Skeleton only — no medium+ alerts until legit A1 attack baseline exists.
 * See docs/detections/aim-provisional.md.
 */
public final class AimAttackAlignCheck {
    private final AlertService alerts;
    private final boolean enabled;
    private final boolean debugOnly;
    private final int window;

    public AimAttackAlignCheck(AlertService alerts, FileConfiguration config) {
        this.alerts = alerts;
        this.enabled = config.getBoolean("checks.align.enabled", true);
        this.debugOnly = config.getBoolean("checks.align.debug_only", true);
        this.window = Math.max(5, config.getInt("checks.align.window", 20));
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

        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        for (PlayerData.AttackSample s : data.alignWindow()) {
            sum += s.alignDeg();
            min = Math.min(min, s.alignDeg());
        }
        int n = data.alignWindow().size();
        double mean = sum / n;

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("alignDeg", round3(alignDeg));
        fields.put("mean", round3(mean));
        fields.put("min", round3(min));
        fields.put("n", n);
        fields.put("targetId", targetId);
        fields.put("provisional", "blocked-until-A1-attack");

        // Always debug-level while debug_only; never escalate without legit baseline.
        if (debugOnly) {
            alerts.emit(player, "AimAttackAlign", AlertLevel.DEBUG,
                    String.format(Locale.ROOT, "align=%.2f° mean=%.2f° n=%d", alignDeg, mean, n),
                    fields);
        }
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
