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
    private final long decayAfterIdleMs;
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
    private final boolean bypassOps;

    private boolean cancelDamageEnabled;
    private boolean kickEnabled;

    public PunishmentService(JavaPlugin plugin, AlertService alerts, VlStore store, FileConfiguration config) {
        this.plugin = plugin;
        this.alerts = alerts;
        this.store = store;
        this.log = plugin.getLogger();
        this.enabled = config.getBoolean("punish.enabled", true);
        this.decayPerSec = Math.max(0, config.getInt("punish.vl_decay_per_sec", 1));
        this.decayAfterIdleMs = config.getLong("punish.vl_decay_after_idle_ms", 6000L);
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
        this.bypassOps = config.getBoolean("punish.bypass_for_ops", false);
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
                + " kick=" + kickEnabled + "@" + kickAtVl
                + " decay=" + decayPerSec + "/s after " + (decayAfterIdleMs / 1000) + "s idle";
    }

    public String bypassPermission() {
        return bypassPermission;
    }

    /**
     * Bukkit gives operators every permission, so an OP tester silently bypassed
     * punishment. Bypass only counts when the permission is explicitly granted.
     */
    public boolean isBypassed(Player player) {
        if (!player.hasPermission(bypassPermission)) {
            return false;
        }
        if (player.isOp() && !bypassOps) {
            return player.isPermissionSet(bypassPermission);
        }
        return true;
    }

    /** Called on join: restore persisted VL, but never high enough to kick on sight. */
    public void restore(PlayerData data) {
        int stored = store.get(data.uuid());
        if (stored <= 0) {
            return;
        }
        int capped = Math.min(stored, Math.max(0, kickAtVl - 1));
        data.setTotalVl(capped);
        if (stored >= kickAtVl) {
            log.info("restored vl for " + data.name() + ": " + stored + " capped to " + capped
                    + " (no instant kick on join)");
        }
    }

    public void addVl(Player player, PlayerData data, int amount, String check, String reason) {
        if (!enabled || amount <= 0) {
            return;
        }
        decay(data);
        data.addTotalVl(amount);
        store.put(data.uuid(), data.totalVl());
        evaluate(player, data, check, reason);
    }

    /**
     * Applies the tier matching the current VL. Safe to call repeatedly
     * (from new violations and from the 1s heartbeat) so enabling a tier at
     * runtime takes effect immediately.
     */
    public void evaluate(Player player, PlayerData data, String check, String reason) {
        if (!enabled || data.kicked()) {
            return;
        }
        int vl = data.totalVl();
        if (vl <= 0) {
            return;
        }

        if (isBypassed(player)) {
            if (vl >= cancelAtVl) {
                data.noteBlockedByBypass("bypass");
            }
            return;
        }
        data.clearBlocked();
        if (vl >= kickAtVl) {
            if (kickEnabled) {
                kick(player, data, check, vl, reason);
            } else {
                data.noteBlockedByBypass("kick-disabled");
            }
            return;
        }
        if (vl >= kickAtVl - warnMarginVl) {
            if (kickEnabled) {
                warn(player, data, check, vl,
                        "final warning: reaching vl " + kickAtVl + " will disconnect you");
            }
        }
        if (vl >= cancelAtVl) {
            if (cancelDamageEnabled) {
                applyCancelDamage(player, data, check, vl, reason);
            } else {
                data.noteBlockedByBypass("cancel-disabled");
                warn(player, data, check, vl, "suspicious aim detected (vl " + vl + ")");
            }
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
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("action", "KICK");
        fields.put("vl", vl);
        fields.put("check", check);
        fields.put("reason", reason);
        alerts.emit(player, check, AlertLevel.HIGH,
                "vl=" + vl + " → kick (" + reason + ")", fields);

        if (isBypassed(player)) {
            data.noteBlockedByBypass("bypass");
            log.info("punish bypassed for " + player.getName() + " (has " + bypassPermission + ")");
            return;
        }        data.markKicked();
        store.put(data.uuid(), vl);
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

    /**
     * VL only decays after the player has been violation-free for a grace period.
     * Decaying every second unconditionally made scores impossible to accumulate:
     * AuraSwitch awards +2 per 3s (≈0.67/s) while old decay removed 1/s.
     * Called once per second from the heartbeat, so one step == one second.
     */
    public void decay(PlayerData data) {
        if (!enabled || decayPerSec <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - data.lastViolationMs() < decayAfterIdleMs) {
            return;
        }
        int before = data.totalVl();
        data.decayTotalVl(decayPerSec);
        if (data.totalVl() != before) {
            store.put(data.uuid(), data.totalVl());
        }
    }

    public void tickAll(Iterable<PlayerData> all) {
        for (PlayerData d : all) {
            decay(d);
        }
    }

    /** Heartbeat: decay + re-evaluate tiers so runtime toggles apply immediately. */
    public void tickOnline(java.util.List<Player> online) {
        for (Player player : online) {
            if (!player.isOnline()) {
                continue;
            }
            PlayerData data = dataLookup.apply(player);
            if (data == null) {
                continue;
            }
            decay(data);
            evaluate(player, data, "Punish", "vl=" + data.totalVl());
        }
    }

    private java.util.function.Function<Player, PlayerData> dataLookup = p -> null;

    public void bindDataLookup(java.util.function.Function<Player, PlayerData> lookup) {
        this.dataLookup = lookup;
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
