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
import dev.lin.antiaim.punish.VlStore;
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
    private BukkitTask heartbeatTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.captureService = new CaptureService(this);
        this.playerDataService = new PlayerDataService();

        AlertService alerts = new AlertService(this, captureService);
        VlStore vlStore = new VlStore(this);
        this.punishmentService = new PunishmentService(this, alerts, vlStore, getConfig());
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
                new PlayerConnectionListener(this, playerDataService, punishmentService), this);

        // decay once per second; persist every 30s so a crash loses little
        heartbeatTask = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            private int ticks;

            @Override
            public void run() {
                punishmentService.tickAll(playerDataService.all());
                if (++ticks % 30 == 0) {
                    punishmentService.persistAll(playerDataService.all());
                }
            }
        }, 20L, 20L);

        var ac = getCommand("ac");
        if (ac != null) {
            AcCommand executor = new AcCommand(captureService, playerDataService, punishmentService);
            ac.setExecutor(executor);
            ac.setTabCompleter(executor);
        }

        getLogger().info("LinAntiAim ready: recorder=" + recorderEnabled
                + " AimSnap=" + aimSnap.enabled()
                + " Align=" + align.enabled()
                + " AuraSwitch=" + auraSwitch.enabled());
        getLogger().info(punishmentService.statusLine()
                + " | tiers: cancel@" + punishmentService.cancelAtVl()
                + " kick@" + punishmentService.kickAtVl());
    }

    @Override
    public void onDisable() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        if (punishmentService != null && playerDataService != null) {
            punishmentService.persistAll(playerDataService.all());
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
