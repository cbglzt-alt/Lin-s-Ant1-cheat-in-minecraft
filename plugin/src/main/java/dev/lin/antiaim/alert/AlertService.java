package dev.lin.antiaim.alert;

import dev.lin.antiaim.record.CaptureService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class AlertService {
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final JavaPlugin plugin;
    private final CaptureService captures;
    private final Path alertsDir;
    private final boolean jsonlEnabled;
    private final boolean broadcast;

    public AlertService(JavaPlugin plugin, CaptureService captures) {
        this.plugin = plugin;
        this.captures = captures;
        this.alertsDir = plugin.getDataFolder().toPath().resolve("alerts");
        this.jsonlEnabled = plugin.getConfig().getBoolean("alerts.jsonl", true);
        this.broadcast = plugin.getConfig().getBoolean("alerts.broadcast", true);
        if (jsonlEnabled) {
            try {
                Files.createDirectories(alertsDir);
            } catch (IOException e) {
                plugin.getLogger().warning("Cannot create alerts dir: " + e.getMessage());
            }
        }
    }

    public void emit(Player player, String check, AlertLevel level, String summary, Map<String, Object> fields) {
        String captureLabel = null;
        String captureFile = null;
        CaptureService.ActiveCapture cap = captures.get(player);
        if (cap != null) {
            captureLabel = cap.label();
            captureFile = cap.fileName();
        }

        StringBuilder line = new StringBuilder(160);
        line.append("[LinAntiAim] ").append(level).append(' ')
                .append(check).append(' ').append(player.getName())
                .append(" — ").append(summary);
        if (captureLabel != null) {
            line.append(" capture=").append(captureLabel);
        }
        if (level == AlertLevel.DEBUG) {
            plugin.getLogger().log(Level.FINE, line.toString());
            if (plugin.getConfig().getBoolean("alerts.log_debug", false)) {
                plugin.getLogger().info(line.toString());
            }
        } else {
            plugin.getLogger().info(line.toString());
        }

        if (broadcast && level.ordinal() >= AlertLevel.MEDIUM.ordinal()) {
            String chat = "§c[LinAntiAim] §e" + check + " §7" + player.getName() + " §f" + summary
                    + (captureLabel != null ? " §8[" + captureLabel + "]" : "");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("linantiaim.alerts") || online.hasPermission("linantiaim.admin")) {
                    online.sendMessage(chat);
                }
            }
        }

        if (jsonlEnabled) {
            writeJsonl(player, check, level, summary, fields, captureLabel, captureFile);
        }
    }

    private void writeJsonl(Player player, String check, AlertLevel level, String summary,
                            Map<String, Object> fields, String captureLabel, String captureFile) {
        Path file = alertsDir.resolve("alerts-" + DAY.format(Instant.now()) + ".jsonl");
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        append(sb, "t", Long.toString(System.currentTimeMillis()), true);
        append(sb, "check", quote(check), false);
        append(sb, "level", quote(level.name()), false);
        append(sb, "player", quote(player.getName()), false);
        append(sb, "uuid", quote(player.getUniqueId().toString()), false);
        append(sb, "summary", quote(summary), false);
        if (captureLabel != null) {
            append(sb, "captureLabel", quote(captureLabel), false);
        }
        if (captureFile != null) {
            append(sb, "captureFile", quote(captureFile), false);
        }
        if (fields != null) {
            for (Map.Entry<String, Object> e : fields.entrySet()) {
                append(sb, e.getKey(), formatValue(e.getValue()), false);
            }
        }
        sb.append('}');
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(sb.toString());
            w.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("alert jsonl write failed: " + e.getMessage());
        }
    }

    private static void append(StringBuilder sb, String k, String v, boolean first) {
        if (!first) {
            sb.append(',');
        }
        sb.append(quote(k)).append(':').append(v);
    }

    private static String formatValue(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        return quote(String.valueOf(v));
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
