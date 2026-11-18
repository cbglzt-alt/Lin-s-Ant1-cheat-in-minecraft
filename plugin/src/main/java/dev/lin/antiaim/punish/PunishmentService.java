package dev.lin.antiaim.punish;

import dev.lin.antiaim.alert.AlertLevel;
import dev.lin.antiaim.alert.AlertService;
import dev.lin.antiaim.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Escalating punishment from accumulated VL:
 * medium = alert, high = cancel damage window, critical = kick.
 */
public final class PunishmentService {
    private final JavaPlugin plugin;
    private final AlertService alerts;
    private final Logger log;

    private final boolean enabled;
    private final int decayPerSec;
    private final int cancelAtVl;
    private final long cancelDurationMs;
    private final int kickAtVl;
    private final boolean cancelDamageEnabled;
    private final boolean kickEnabled;
    private final long actionCooldownMs;
    private final String kickMessage;
    private final String bypassPermission;

    public PunishmentService(JavaPlugin plugin, AlertService alerts, FileConfiguration config) {
        this.plugin = plugin;
        this.alerts = alerts;
        this.log = plugin.getLogger();
        this.enabled = config.getBoolean("punish.enabled", true);
        this.decayPerSec = Math.max(0, config.getInt("punish.vl_decay_per_sec", 1));
        this.cancelAtVl = config.getInt("punish.cancel_damage_at_vl", 12);
        this.cancelDurationMs = config.getLong("punish.cancel_damage_seconds", 5L) * 1000L;
        this.kickAtVl = config.getInt("punish.kick_at_vl", 24);
        this.cancelDamageEnabled = config.getBoolean("punish.cancel_damage", false);
        this.kickEnabled = config.getBoolean("punish.kick", false);
        this.actionCooldownMs = config.getLong("punish.action_cooldown_ms", 4000L);
        this.kickMessage = config.getString("punish.kick_message",
                "Unfair advantage detected ({check}, vl={vl}).");
        this.bypassPermission = config.getString("punish.bypass_permission", "linantiaim.bypass");
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean damageCancelEnabled() {
        return enabled && cancelDamageEnabled;
    }

    public boolean kickEnabled() {
        return enabled && kickEnabled;
    }

    public int cancelAtVl() {
        return cancelAtVl;
    }

    public int kickAtVl() {
        return kickAtVl;
    }

    public void addVl(Player player, PlayerData data, int amount, String check, String reason) {
        if (!enabled || amount <= 0) {
            return;
        }
        if (data.kicked()) {
            return;
        }
        decay(data);
        data.addTotalVl(amount);
        int vl = data.totalVl();

        if (vl >= kickAtVl && kickEnabled) {
            kick(player, data, check, vl, reason);
            return;
        }
        if (vl >= cancelAtVl && cancelDamageEnabled) {
            long now = System.currentTimeMillis();
            data.cancelDamageUntil(now + cancelDurationMs);
            if (now - data.lastPunishActionMs() >= actionCooldownMs) {
                data.markPunishAction(now);
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("action", "CANCEL_DAMAGE");
                fields.put("vl", vl);
                fields.put("check", check);
                fields.put("durationMs", cancelDurationMs);
                alerts.emit(player, check, AlertLevel.HIGH,
                        "vl=" + vl + " → cancel damage " + (cancelDurationMs / 1000) + "s (" + reason + ")",
                        fields);
                player.sendMessage(ChatColor.RED + "[LinAntiAim] " + ChatColor.GRAY
                        + "Attack blocked for " + (cancelDurationMs / 1000) + "s.");
            }
        }
    }

    public void kick(Player player, PlayerData data, String check, int vl, String reason) {
        if (data.kicked()) {
            return;
        }
        data.markKicked();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("action", "KICK");
        fields.put("vl", vl);
        fields.put("check", check);
        fields.put("reason", reason);
        alerts.emit(player, check, AlertLevel.HIGH,
                "vl=" + vl + " → kick (" + reason + ")", fields);

        if (player.hasPermission(bypassPermission)) {
            log.info("punish bypassed for " + player.getName() + " (has " + bypassPermission + ")");
            return;
        }
        String msg = kickMessage.replace("{check}", check)
                .replace("{vl}", Integer.toString(vl));
        String colored = ChatColor.translateAlternateColorCodes('&', msg);
        // must run on main thread
        Runnable kick = () -> player.kickPlayer(colored);
        if (Bukkit.isPrimaryThread()) {
            kick.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, kick);
        }
    }

    /** Lazy decay; call on VL reads or from the 1s heartbeat. */
    public void decay(PlayerData data) {
        if (!enabled || decayPerSec <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = data.lastVlMs();
        if (last <= 0 || now - last < 1000L) {
            return;
        }
        long secs = (now - last) / 1000L;
        data.setTotalVl(Math.max(0, data.totalVl() - (int) (secs * decayPerSec)));
    }

    public void tickAll(Iterable<PlayerData> all) {
        for (PlayerData d : all) {
            decay(d);
        }
    }

    public void reset(PlayerData data) {
        data.setTotalVl(0);
        data.cancelDamageUntil(0);
        data.markPunishAction(0);
    }

    public AlertLevel levelFor(PlayerData data) {
        int vl = data.totalVl();
        if (vl >= kickAtVl) {
            return AlertLevel.HIGH;
        }
        if (vl >= cancelAtVl) {
            return AlertLevel.HIGH;
        }
        if (vl > 0) {
            return AlertLevel.MEDIUM;
        }
        return AlertLevel.LOW;
    }
}
