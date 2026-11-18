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
 *   low     -> silent/debug
 *   medium  -> alert (chat + jsonl) + warning to the player
 *   high    -> cancel outgoing damage, duration grows with VL
 *   critical-> kick (after a pre-kick warning)
 *
 * cancel_damage and kick are off by default and can be toggled at runtime.
 */
public final class PunishmentService {
    private final JavaPlugin plugin;
    private final AlertService alerts;
    private final VlStore store;
    private final Logger log;

    private final boolean enabled;
    private final int decayPerSec;
    private final int cancelAtVl;
    private final long cancelBaseMs;
    private final long cancelStepMs;
    private final int cancelStepVl;
    private final long cancelMaxMs;
    private final int kickAtVl;
    private final int warnMarginVl;
    private final long actionCooldownMs;
    private final String kickMessage;
    private final String bypassPermission;

    private boolean cancelDamageEnabled;
    private boolean kickEnabled;

    public PunishmentService(JavaPlugin plugin, AlertService alerts, VlStore store, FileConfiguration config) {
        this.plugin = plugin;
        this.alerts = alerts;
        this.store = store;
        this.log = plugin.getLogger();
        this.enabled = config.getBoolean("punish.enabled", true);
        this.decayPerSec = Math.max(0, config.getInt("punish.vl_decay_per_sec", 1));
        this.cancelAtVl = config.getInt("punish.cancel_damage_at_vl", 12);
        this.cancelBaseMs = config.getLong("punish.cancel_damage_seconds", 5L) * 1000L;
        this.cancelStepMs = config.getLong("punish.cancel_damage_step_seconds", 2L) * 1000L;
        this.cancelStepVl = Math.max(1, config.getInt("punish.cancel_damage_step_vl", 6));
        this.cancelMaxMs = config.getLong("punish.cancel_damage_max_seconds", 20L) * 1000L;
        this.kickAtVl = config.getInt("punish.kick_at_vl", 30);
        this.warnMarginVl = Math.max(0, config.getInt("punish.warn_before_kick_vl", 4));
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

    public void setCancelDamage(boolean on) {
        this.cancelDamageEnabled = on;
    }

    public void setKick(boolean on) {
        this.kickEnabled = on;
    }

    public int cancelAtVl() {
        return cancelAtVl;
    }

    public int kickAtVl() {
        return kickAtVl;
    }

    public String statusLine() {
        return "punish enabled=" + enabled
                + " cancelDamage=" + cancelDamageEnabled + "@" + cancelAtVl
                + " kick=" + kickEnabled + "@" + kickAtVl;
    }

    /** Called on join: restore persisted VL. */
    public void restore(PlayerData data) {
        int stored = store.get(data.uuid());
        if (stored > 0) {
            data.setTotalVl(stored);
        }
    }

    public void addVl(Player player, PlayerData data, int amount, String check, String reason) {
        if (!enabled || amount <= 0 || data.kicked()) {
            return;
        }
        decay(data);
        data.addTotalVl(amount);
        int vl = data.totalVl();
        store.put(data.uuid(), vl);

        if (player.hasPermission(bypassPermission)) {
            return;
        }

        if (vl >= kickAtVl && kickEnabled) {
            kick(player, data, check, vl, reason);
            return;
        }
        if (vl >= kickAtVl - warnMarginVl && kickEnabled) {
            warn(player, data, check, vl, "final warning: reaching vl " + kickAtVl + " will disconnect you");
        }
        if (vl >= cancelAtVl && cancelDamageEnabled) {
            applyCancelDamage(player, data, check, vl, reason);
        } else if (vl >= cancelAtVl) {
            warn(player, data, check, vl, "suspicious aim detected (vl " + vl + ")");
        }
    }

    private void applyCancelDamage(Player player, PlayerData data, String check, int vl, String reason) {
        long now = System.currentTimeMillis();
        long duration = cancelDurationFor(vl);
        data.cancelDamageUntil(Math.max(data.damageCancelUntilMs(), now + duration));
        if (now - data.lastPunishActionMs() < actionCooldownMs) {
            return;
        }
        data.markPunishAction(now);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("action", "CANCEL_DAMAGE");
        fields.put("vl", vl);
        fields.put("check", check);
        fields.put("durationMs", duration);
        alerts.emit(player, check, AlertLevel.HIGH,
                "vl=" + vl + " → cancel damage " + (duration / 1000) + "s (" + reason + ")", fields);
        player.sendMessage(ChatColor.RED + "[LinAntiAim] " + ChatColor.GRAY
                + "Outgoing damage blocked for " + (duration / 1000) + "s.");
    }

    /** Duration grows with VL: base, then +step per cancelStepVl, capped. */
    public long cancelDurationFor(int vl) {
        int extraSteps = Math.max(0, (vl - cancelAtVl) / cancelStepVl);
        return Math.min(cancelMaxMs, cancelBaseMs + extraSteps * cancelStepMs);
    }

    private void warn(Player player, PlayerData data, String check, int vl, String text) {
        long now = System.currentTimeMillis();
        if (now - data.lastWarnMs() < actionCooldownMs) {
            return;
        }
        data.markWarn(now);
        player.sendMessage(ChatColor.RED + "[LinAntiAim] " + ChatColor.YELLOW + text);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("action", "WARN");
        fields.put("vl", vl);
        fields.put("check", check);
        alerts.emit(player, check, AlertLevel.MEDIUM, "vl=" + vl + " warn: " + text, fields);
    }

    public void kick(Player player, PlayerData data, String check, int vl, String reason) {
        if (data.kicked()) {
            return;
        }
        data.markKicked();
        store.put(data.uuid(), vl);
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
        Runnable kick = () -> player.kickPlayer(colored);
        if (Bukkit.isPrimaryThread()) {
            kick.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, kick);
        }
    }

    /** Lazy decay; also persists so a restart does not resurrect old VL. */
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
        int before = data.totalVl();
        data.setTotalVl(Math.max(0, before - (int) (secs * decayPerSec)));
        if (data.totalVl() != before) {
            store.put(data.uuid(), data.totalVl());
        }
    }

    public void tickAll(Iterable<PlayerData> all) {
        for (PlayerData d : all) {
            decay(d);
        }
    }

    public void persistAll(Iterable<PlayerData> all) {
        for (PlayerData d : all) {
            store.put(d.uuid(), d.totalVl());
        }
        store.save();
    }

    public void persist(PlayerData data) {
        store.put(data.uuid(), data.totalVl());
        store.save();
    }

    public void reset(PlayerData data) {
        data.setTotalVl(0);
        data.cancelDamageUntil(0);
        data.markPunishAction(0);
        data.markWarn(0);
        store.put(data.uuid(), 0);
        store.save();
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
