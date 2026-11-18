package dev.lin.antiaim.punish;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Persists accumulated VL so relogging / restarting does not reset the score.
 */
public final class VlStore {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public VlStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "vl.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Cannot create data folder for vl.yml");
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public int get(UUID uuid) {
        return yaml.getInt("vl." + uuid, 0);
    }

    public void put(UUID uuid, int vl) {
        if (vl <= 0) {
            yaml.set("vl." + uuid, null);
        } else {
            yaml.set("vl." + uuid, vl);
        }
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("vl.yml save failed: " + e.getMessage());
        }
    }
}
