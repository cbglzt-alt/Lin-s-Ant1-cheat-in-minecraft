package dev.lin.antiaim.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class AimMath {
    private AimMath() {
    }

    public static float deltaYaw(float from, float to) {
        float d = to - from;
        while (d > 180f) {
            d -= 360f;
        }
        while (d < -180f) {
            d += 360f;
        }
        return d;
    }

    /** Angle between look vector and eye→body-center (degrees). */
    public static double alignDeg(Player attacker, Entity target, float yaw, float pitch) {
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

    public static Vector direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vector(x, y, z);
    }

    public static double percentile(double[] sorted, double p) {
        if (sorted.length == 0) {
            return Double.NaN;
        }
        if (sorted.length == 1) {
            return sorted[0];
        }
        double k = (sorted.length - 1) * p;
        int f = (int) Math.floor(k);
        int c = (int) Math.ceil(k);
        if (f == c) {
            return sorted[(int) k];
        }
        return sorted[f] * (c - k) + sorted[c] * (k - f);
    }
}
