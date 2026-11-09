package dev.lin.antiaim;

import com.github.retrooper.packetevents.PacketEvents;
import dev.lin.antiaim.command.AcCommand;
import dev.lin.antiaim.record.BukkitAttackListener;
import dev.lin.antiaim.record.CaptureService;
import dev.lin.antiaim.record.PacketCaptureListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Depends on the standalone PacketEvents plugin (see plugin.yml {@code depend}).
 * Do not call SpigotPacketEventsBuilder / init / terminate here.
 */
public final class LinAntiAimPlugin extends JavaPlugin {
    private CaptureService captureService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.captureService = new CaptureService(this);
        PacketEvents.getAPI().getEventManager().registerListener(new PacketCaptureListener(captureService));
        getServer().getPluginManager().registerEvents(new BukkitAttackListener(captureService), this);

        var ac = getCommand("ac");
        if (ac != null) {
            AcCommand executor = new AcCommand(captureService);
            ac.setExecutor(executor);
            ac.setTabCompleter(executor);
        }

        getLogger().info("LinAntiAim recorder ready. Use /ac record start <label>");
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
}
