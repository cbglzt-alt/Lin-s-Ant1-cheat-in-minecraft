package dev.lin.antiaim.record;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Locale;

final class JsonLine {
    private JsonLine() {
    }

    static String sessionStart(CaptureService.ActiveCapture c, Player player) {
        return obj(
                kv("type", quote("session")),
                kv("t", Long.toString(System.currentTimeMillis())),
                kv("label", quote(c.label)),
                kv("player", quote(player.getName())),
                kv("uuid", quote(player.getUniqueId().toString())),
                kv("world", quote(player.getWorld().getName())),
                kv("protocol", quote("c2s-look-attack-swing"))
        );
    }

    static String sessionEnd(CaptureService.ActiveCapture c, String reason) {
        return obj(
                kv("type", quote("session_end")),
                kv("t", Long.toString(System.currentTimeMillis())),
                kv("label", quote(c.label)),
                kv("reason", quote(reason)),
                kv("looks", Integer.toString(c.lookCount)),
                kv("attacks", Integer.toString(c.attackCount)),
                kv("swings", Integer.toString(c.swingCount)),
                kv("durationMs", Long.toString(System.currentTimeMillis() - c.startedAtMs))
        );
    }

    static String look(CaptureService.ActiveCapture c, double x, double y, double z,
                       float yaw, float pitch, boolean onGround, boolean hasPos, boolean hasRot) {
        StringBuilder sb = new StringBuilder(160);
        sb.append('{');
        append(sb, "type", quote("look"), true);
        append(sb, "t", Long.toString(System.currentTimeMillis()), false);
        append(sb, "yaw", fmt(yaw), false);
        append(sb, "pitch", fmt(pitch), false);
        if (hasPos) {
            append(sb, "x", fmt(x), false);
            append(sb, "y", fmt(y), false);
            append(sb, "z", fmt(z), false);
        }
        append(sb, "onGround", Boolean.toString(onGround), false);
        append(sb, "hasPos", Boolean.toString(hasPos), false);
        append(sb, "hasRot", Boolean.toString(hasRot), false);
        if (c.hasLook) {
            append(sb, "dYaw", fmt(deltaYaw(c.lastYaw, yaw)), false);
            append(sb, "dPitch", fmt(pitch - c.lastPitch), false);
        }
        sb.append('}');
        return sb.toString();
    }

    static String attack(CaptureService.ActiveCapture c, Player attacker, Entity target,
                         float yaw, float pitch) {
        double align = alignDeg(attacker, target, yaw, pitch);
        StringBuilder sb = new StringBuilder(220);
        sb.append('{');
        append(sb, "type", quote("attack"), true);
        append(sb, "t", Long.toString(System.currentTimeMillis()), false);
        append(sb, "yaw", fmt(yaw), false);
        append(sb, "pitch", fmt(pitch), false);
        append(sb, "targetId", Integer.toString(target.getEntityId()), false);
        append(sb, "targetType", quote(target.getType().name()), false);
        append(sb, "tx", fmt(target.getLocation().getX()), false);
        append(sb, "ty", fmt(target.getLocation().getY()), false);
        append(sb, "tz", fmt(target.getLocation().getZ()), false);
        append(sb, "alignDeg", fmt(align), false);
        sb.append('}');
        return sb.toString();
    }

    static String swing() {
        return obj(
                kv("type", quote("swing")),
                kv("t", Long.toString(System.currentTimeMillis()))
        );
    }

    /** Angle between look vector and eye→body-center vector (degrees). */
    static double alignDeg(Player attacker, Entity target, float yaw, float pitch) {
        Vector look = direction(yaw, pitch);
        double bodyY = target.getLocation().getY() + target.getHeight() / 2.0;
        Vector to = new Vector(
                target.getLocation().getX() - attacker.getEyeLocation().getX(),
                bodyY - attacker.getEyeLocation().getY(),
                target.getLocation().getZ() - attacker.getEyeLocation().getZ()
        );
        if (to.lengthSquared() < 1.0e-8 || look.lengthSquared() < 1.0e-8) {
            return 180.0;
        }
        to.normalize();
        look.normalize();
        double dot = Math.max(-1.0, Math.min(1.0, look.dot(to)));
        return Math.toDegrees(Math.acos(dot));
    }

    private static Vector direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vector(x, y, z);
    }

    private static float deltaYaw(float from, float to) {
        float d = to - from;
        while (d > 180f) d -= 360f;
        while (d < -180f) d += 360f;
        return d;
    }

    private static String obj(String... parts) {
        return "{" + String.join(",", parts) + "}";
    }

    private static String kv(String k, String v) {
        return quote(k) + ":" + v;
    }

    private static void append(StringBuilder sb, String k, String v, boolean first) {
        if (!first) sb.append(',');
        sb.append(quote(k)).append(':').append(v);
    }

    private static String quote(String s) {
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.5f", v);
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.5f", v);
    }
}
