package dev.lin.antiaim.record;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CaptureService {
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final JavaPlugin plugin;
    private final Path recordsDir;
    private final Map<UUID, ActiveCapture> active = new ConcurrentHashMap<>();

    public CaptureService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recordsDir = plugin.getDataFolder().toPath().resolve("records");
        try {
            Files.createDirectories(recordsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create records dir", e);
        }
    }

    public Path getRecordsDir() {
        return recordsDir;
    }

    public boolean isRecording(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public ActiveCapture get(Player player) {
        return active.get(player.getUniqueId());
    }

    public ActiveCapture get(UUID playerId) {
        return active.get(playerId);
    }

    public synchronized String start(Player player, String label) throws IOException {
        if (active.containsKey(player.getUniqueId())) {
            throw new IllegalStateException("Already recording. /ac record stop first.");
        }
        String safeLabel = sanitize(label);
        String fileName = FILE_TS.format(Instant.now()) + "_" + sanitize(player.getName()) + "_" + safeLabel + ".jsonl";
        Path file = recordsDir.resolve(fileName);
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        ActiveCapture capture = new ActiveCapture(player.getUniqueId(), player.getName(), safeLabel, file, writer);
        active.put(player.getUniqueId(), capture);
        capture.writeRaw(JsonLine.sessionStart(capture, player));
        plugin.getLogger().info("Capture start " + player.getName() + " -> " + file.getFileName());
        return file.getFileName().toString();
    }

    public synchronized String stop(Player player, String reason) throws IOException {
        ActiveCapture capture = active.remove(player.getUniqueId());
        if (capture == null) {
            return null;
        }
        capture.writeRaw(JsonLine.sessionEnd(capture, reason));
        capture.close();
        plugin.getLogger().info("Capture stop " + player.getName() + " looks=" + capture.lookCount
                + " attacks=" + capture.attackCount + " file=" + capture.file.getFileName());
        return capture.file.getFileName().toString();
    }

    public void stopAll(String reason) {
        for (UUID id : active.keySet().toArray(UUID[]::new)) {
            ActiveCapture c = active.get(id);
            if (c == null) continue;
            try {
                Player p = plugin.getServer().getPlayer(id);
                if (p != null) {
                    stop(p, reason);
                } else {
                    active.remove(id);
                    c.writeRaw(JsonLine.sessionEnd(c, reason));
                    c.close();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed stopping capture " + id + ": " + e.getMessage());
            }
        }
    }

    public String statusLine(Player player) {
        ActiveCapture c = active.get(player.getUniqueId());
        if (c == null) {
            return "not recording";
        }
        return "recording label=" + c.label + " looks=" + c.lookCount + " attacks=" + c.attackCount
                + " swings=" + c.swingCount + " file=" + c.file.getFileName();
    }

    private static String sanitize(String raw) {
        String s = raw.trim().replaceAll("[^a-zA-Z0-9._-]+", "-");
        if (s.isEmpty()) {
            return "unnamed";
        }
        return s.substring(0, Math.min(s.length(), 64));
    }

    public static final class ActiveCapture {
        final UUID playerId;
        final String playerName;
        final String label;
        final Path file;
        final BufferedWriter writer;
        final long startedAtMs = System.currentTimeMillis();
        volatile float lastYaw;
        volatile float lastPitch;
        volatile boolean hasLook;
        int lookCount;
        int attackCount;
        int swingCount;

        ActiveCapture(UUID playerId, String playerName, String label, Path file, BufferedWriter writer) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.label = label;
            this.file = file;
            this.writer = writer;
        }

        public String label() {
            return label;
        }

        public String fileName() {
            return file.getFileName().toString();
        }

        public void syncLookFrom(float yaw, float pitch, boolean hasLook) {
            this.lastYaw = yaw;
            this.lastPitch = pitch;
            this.hasLook = hasLook;
        }

        public synchronized void writeLook(double x, double y, double z, float yaw, float pitch,
                                           boolean onGround, boolean hasPos, boolean hasRot) throws IOException {
            writeRaw(JsonLine.look(this, x, y, z, yaw, pitch, onGround, hasPos, hasRot));
            lookCount++;
            if (hasRot) {
                lastYaw = yaw;
                lastPitch = pitch;
                hasLook = true;
            }
        }

        public synchronized void writeAttack(Player attacker, org.bukkit.entity.Entity target,
                                             float yaw, float pitch, boolean damaged, String source)
                throws IOException {
            lastYaw = yaw;
            lastPitch = pitch;
            hasLook = true;
            writeRaw(JsonLine.attack(this, attacker, target, yaw, pitch, damaged, source));
            attackCount++;
        }

        public synchronized void writeAttackUnknown(long t, float yaw, float pitch, int targetId,
                                                    boolean damaged, String source) throws IOException {
            writeRaw("{\"type\":\"attack\",\"t\":" + t
                    + ",\"yaw\":" + yaw + ",\"pitch\":" + pitch
                    + ",\"targetId\":" + targetId
                    + ",\"targetType\":\"UNKNOWN\",\"alignDeg\":-1"
                    + ",\"damaged\":" + damaged
                    + ",\"source\":\"" + source + "\"}");
            attackCount++;
        }

        public synchronized void writeSwing() throws IOException {
            writeRaw(JsonLine.swing());
            swingCount++;
        }

        public synchronized void writeRaw(String line) throws IOException {
            writer.write(line);
            writer.newLine();
            writer.flush();
        }

        synchronized void close() throws IOException {
            writer.close();
        }
    }
}
