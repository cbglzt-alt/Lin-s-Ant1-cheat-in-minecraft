package dev.lin.antiaim;

import com.github.retrooper.packetevents.PacketEvents;
import dev.lin.antiaim.alert.AlertService;
import dev.lin.antiaim.check.AimAttackAlignCheck;
import dev.lin.antiaim.check.AimSnapCheck;
import dev.lin.antiaim.command.AcCommand;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.listen.CombatBukkitListener;
import dev.lin.antiaim.listen.CombatPacketListener;
import dev.lin.antiaim.listen.PlayerConnectionListener;
import dev.lin.antiaim.record.CaptureService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Depends on the standalone PacketEvents plugin (see plugin.yml {@code depend}).
 * Do not call SpigotPacketEventsBuilder / init / terminate here.
 */
public final class LinAntiAimPlugin extends JavaPlugin {
    private CaptureService captureService;
    private PlayerDataService playerDataService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.captureService = new CaptureService(this);
        this.playerDataService = new PlayerDataService();

        AlertService alerts = new AlertService(this, captureService);
        AimSnapCheck aimSnap = new AimSnapCheck(alerts, getConfig());
        AimAttackAlignCheck align = new AimAttackAlignCheck(alerts, getConfig());
        boolean recorderEnabled = getConfig().getBoolean("recorder.enabled", true);

        PacketEvents.getAPI().getEventManager().registerListener(
                new CombatPacketListener(captureService, playerDataService, aimSnap, align, recorderEnabled));
        getServer().getPluginManager().registerEvents(
                new CombatBukkitListener(captureService, playerDataService, align, recorderEnabled), this);
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, playerDataService), this);

        var ac = getCommand("ac");
        if (ac != null) {
            AcCommand executor = new AcCommand(captureService);
            ac.setExecutor(executor);
            ac.setTabCompleter(executor);
        }

        getLogger().info("LinAntiAim ready: recorder=" + recorderEnabled
                + " AimSnap=" + aimSnap.enabled()
                + " Align=" + align.enabled() + "(debug_only)");
        getLogger().info("Use /ac record start <label> — alerts default punish=off");
    }

    @Override
    public void onDisable() {
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
}
