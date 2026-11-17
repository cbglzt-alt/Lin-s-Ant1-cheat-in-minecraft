package dev.lin.antiaim.data;

import java.util.ArrayDeque;
import java.util.UUID;

public final class PlayerData {
    public record LookSample(long t, float absDYaw, float absDPitch) {
    }

    public record AttackSample(long t, int targetId, double alignDeg) {
    }

    private final UUID uuid;
    private final String name;

    private float lastYaw;
    private float lastPitch;
    private boolean hasLook;

    private final ArrayDeque<LookSample> rotWindow = new ArrayDeque<>();
    private final ArrayDeque<AttackSample> alignWindow = new ArrayDeque<>();

    private int aimSnapVl;
    private int aimSnapStreak;
    private int rotSinceWindowEval;
    private long lastCombatMs;
    private long lastSnapAlertMs;
    private long exemptUntilMs;
    private long lastAttackDedupMs;
    private int lastAttackDedupTarget = Integer.MIN_VALUE;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public float lastYaw() {
        return lastYaw;
    }

    public float lastPitch() {
        return lastPitch;
    }

    public boolean hasLook() {
        return hasLook;
    }

    public void setLook(float yaw, float pitch) {
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        this.hasLook = true;
    }

    public ArrayDeque<LookSample> rotWindow() {
        return rotWindow;
    }

    public ArrayDeque<AttackSample> alignWindow() {
        return alignWindow;
    }

    public void pushRot(long t, float absDYaw, float absDPitch, int maxWindow) {
        rotWindow.addLast(new LookSample(t, absDYaw, absDPitch));
        while (rotWindow.size() > maxWindow) {
            rotWindow.removeFirst();
        }
        rotSinceWindowEval++;
    }

    public int rotSinceWindowEval() {
        return rotSinceWindowEval;
    }

    public void resetRotSinceWindowEval() {
        rotSinceWindowEval = 0;
    }

    public void pushAlign(long t, int targetId, double alignDeg, int maxWindow) {
        alignWindow.addLast(new AttackSample(t, targetId, alignDeg));
        while (alignWindow.size() > maxWindow) {
            alignWindow.removeFirst();
        }
        lastCombatMs = t;
    }

    public boolean shouldDedupAttack(long t, int targetId, long windowMs) {
        if (targetId == lastAttackDedupTarget && t - lastAttackDedupMs <= windowMs) {
            return true;
        }
        lastAttackDedupMs = t;
        lastAttackDedupTarget = targetId;
        return false;
    }

    public int aimSnapVl() {
        return aimSnapVl;
    }

    public void addAimSnapVl(int delta) {
        aimSnapVl = Math.max(0, aimSnapVl + delta);
    }

    public void setAimSnapVl(int vl) {
        aimSnapVl = Math.max(0, vl);
    }

    public int aimSnapStreak() {
        return aimSnapStreak;
    }

    public void setAimSnapStreak(int streak) {
        aimSnapStreak = Math.max(0, streak);
    }

    public long lastCombatMs() {
        return lastCombatMs;
    }

    public void markCombat(long t) {
        lastCombatMs = t;
    }

    public long lastSnapAlertMs() {
        return lastSnapAlertMs;
    }

    public void setLastSnapAlertMs(long t) {
        lastSnapAlertMs = t;
    }

    public void exemptUntil(long ms) {
        exemptUntilMs = ms;
    }

    public boolean isExempt(long now) {
        return now < exemptUntilMs;
    }
}
