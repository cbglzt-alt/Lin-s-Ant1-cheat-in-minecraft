package dev.lin.antiaim;

import com.github.retrooper.packetevents.PacketEvents;
import dev.lin.antiaim.alert.AlertService;
import dev.lin.antiaim.check.AimAttackAlignCheck;
import dev.lin.antiaim.check.AimSnapCheck;
import dev.lin.antiaim.check.AuraSwitchCheck;
import dev.lin.antiaim.command.AcCommand;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.listen.CombatBukkitListener;
import dev.lin.antiaim.listen.CombatPacketListener;
import dev.lin.antiaim.listen.PlayerConnectionListener;
import dev.lin.antiaim.listen.PunishDamageListener;
import dev.lin.antiaim.punish.PunishmentService;
import dev.lin.antiaim.record.AttackProbe;
import dev.lin.antiaim.record.CaptureService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Depends on the standalone PacketEvents plugin (see plugin.yml {@code depend}).
 * Do not call SpigotPacketEventsBuilder / init / terminate here.
 */
public final class LinAntiAimPlugin extends JavaPlugin {
    private CaptureService captureService;
    private PlayerDataService playerDataService;
    private PunishmentService punishmentService;
    private BukkitTask vlDecayTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.captureService = new CaptureService(this);
        this.playerDataService = new PlayerDataService();

        AlertService alerts = new AlertService(this, captureService);
        this.punishmentService = new PunishmentService(this, alerts, getConfig());
        AimSnapCheck aimSnap = new AimSnapCheck(alerts, punishmentService, getConfig());
        AimAttackAlignCheck align = new AimAttackAlignCheck(alerts, punishmentService, getConfig());
        AuraSwitchCheck auraSwitch = new AuraSwitchCheck(alerts, punishmentService, getConfig());
        boolean recorderEnabled = getConfig().getBoolean("recorder.enabled", true);
        AttackProbe attacks = new AttackProbe(captureService, align, auraSwitch, recorderEnabled, getLogger());

        PacketEvents.getAPI().getEventManager().registerListener(
                new CombatPacketListener(captureService, playerDataService, aimSnap, attacks, recorderEnabled));
        getServer().getPluginManager().registerEvents(
                new CombatBukkitListener(playerDataService, attacks), this);
        getServer().getPluginManager().registerEvents(
                new PunishDamageListener(playerDataService, punishmentService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, playerDataService), this);

        vlDecayTask = getServer().getScheduler().runTaskTimer(this,
                () -> punishmentService.tickAll(playerDataService.all()), 20L, 20L);

        var ac = getCommand("ac");
        if (ac != null) {
            AcCommand executor = new AcCommand(captureService, playerDataService, punishmentService);
            ac.setExecutor(executor);
            ac.setTabCompleter(executor);
        }

        getLogger().info("LinAntiAim ready: recorder=" + recorderEnabled
                + " AimSnap=" + aimSnap.enabled()
                + " Align=" + align.enabled()
                + " AuraSwitch=" + auraSwitch.enabled()
                + " punish.enabled=" + punishmentService.enabled()
                + " cancelDamage=" + punishmentService.damageCancelEnabled()
                + " kick=" + punishmentService.kickEnabled());
        getLogger().info("Tiers: alert vl>0 | cancel@" + punishmentService.cancelAtVl()
                + " | kick@" + punishmentService.kickAtVl());
    }

    @Override
    public void onDisable() {
        if (vlDecayTask != null) {
            vlDecayTask.cancel();
        }
        if (captureService != null) {
            captureService.stopAll("plugin-disable");
        }
    }

    public CaptureService getCaptureService() {
        return captureService;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public PunishmentService getPunishmentService() {
        return punishmentService;
    }
}
