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
    private long lastAttackMs;
    private long lastSnapAlertMs;
    private long lastAlignAlertMs;
    private long lastAuraAlertMs;
    private int alignVl;
    private int alignStreak;
    private int auraVl;
    private int auraStreak;
    private long exemptUntilMs;
    private long lastAttackDedupMs;
    private int lastAttackDedupTarget = Integer.MIN_VALUE;

    private int totalVl;
    private long lastVlMs;
    private long damageCancelUntilMs;
    private long lastPunishActionMs;
    private boolean kicked;

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
        lastAttackMs = t;
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

    public long lastAttackMs() {
        return lastAttackMs;
    }

    public void markAttack(long t) {
        lastAttackMs = t;
        lastCombatMs = t;
    }

    public long lastSnapAlertMs() {
        return lastSnapAlertMs;
    }

    public void setLastSnapAlertMs(long t) {
        lastSnapAlertMs = t;
    }

    public long lastAlignAlertMs() {
        return lastAlignAlertMs;
    }

    public void setLastAlignAlertMs(long t) {
        lastAlignAlertMs = t;
    }

    public long lastAuraAlertMs() {
        return lastAuraAlertMs;
    }

    public void setLastAuraAlertMs(long t) {
        lastAuraAlertMs = t;
    }

    public int alignVl() {
        return alignVl;
    }

    public void addAlignVl(int delta) {
        alignVl = Math.max(0, alignVl + delta);
    }

    public void setAlignVl(int vl) {
        alignVl = Math.max(0, vl);
    }

    public int alignStreak() {
        return alignStreak;
    }

    public void setAlignStreak(int streak) {
        alignStreak = Math.max(0, streak);
    }

    public int auraVl() {
        return auraVl;
    }

    public void addAuraVl(int delta) {
        auraVl = Math.max(0, auraVl + delta);
    }

    public void setAuraVl(int vl) {
        auraVl = Math.max(0, vl);
    }

    public int auraStreak() {
        return auraStreak;
    }

    public void setAuraStreak(int streak) {
        auraStreak = Math.max(0, streak);
    }

    public void exemptUntil(long ms) {
        exemptUntilMs = ms;
    }

    public boolean isExempt(long now) {
        return now < exemptUntilMs;
    }

    public int totalVl() {
        return totalVl;
    }

    public void setTotalVl(int vl) {
        totalVl = Math.max(0, vl);
        lastVlMs = System.currentTimeMillis();
    }

    public void addTotalVl(int delta) {
        totalVl = Math.max(0, totalVl + delta);
        lastVlMs = System.currentTimeMillis();
    }

    public long lastVlMs() {
        return lastVlMs;
    }

    public long damageCancelUntilMs() {
        return damageCancelUntilMs;
    }

    public void cancelDamageUntil(long ms) {
        damageCancelUntilMs = ms;
    }

    public boolean damageCancelled(long now) {
        return now < damageCancelUntilMs;
    }

    public long lastPunishActionMs() {
        return lastPunishActionMs;
    }

    public void markPunishAction(long t) {
        lastPunishActionMs = t;
    }

    public boolean kicked() {
        return kicked;
    }

    public void markKicked() {
        kicked = true;
    }
}
