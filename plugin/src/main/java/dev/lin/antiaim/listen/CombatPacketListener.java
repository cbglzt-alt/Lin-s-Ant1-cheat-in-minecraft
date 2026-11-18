package dev.lin.antiaim.listen;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.lin.antiaim.check.AimSnapCheck;
import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.record.AttackProbe;
import dev.lin.antiaim.record.CaptureService;
import dev.lin.antiaim.util.AimMath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;

/**
 * Shared look/attack/swing intake for recorder + checks.
 */
public final class CombatPacketListener extends PacketListenerAbstract {
    private final CaptureService captures;
    private final PlayerDataService players;
    private final AimSnapCheck aimSnap;
    private final AttackProbe attacks;
    private final boolean recorderEnabled;

    public CombatPacketListener(CaptureService captures, PlayerDataService players,
                                AimSnapCheck aimSnap, AttackProbe attacks,
                                boolean recorderEnabled) {
        super(PacketListenerPriority.NORMAL);
        this.captures = captures;
        this.players = players;
        this.aimSnap = aimSnap;
        this.attacks = attacks;
        this.recorderEnabled = recorderEnabled;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        if (user == null) {
            return;
        }
        UUID uuid = user.getUUID();
        if (uuid == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        CaptureService.ActiveCapture capture = recorderEnabled ? captures.get(uuid) : null;
        PlayerData data = players.get(player);

        try {
            if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                handleFlying(player, data, capture, new WrapperPlayClientPlayerFlying(event));
                return;
            }
            if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                handleInteract(player, data, new WrapperPlayClientInteractEntity(event));
                return;
            }
            if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                handleSwing(player, data, capture);
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[LinAntiAim] write failed: " + e.getMessage());
        }
    }

    private void handleFlying(Player player, PlayerData data, CaptureService.ActiveCapture capture,
                              WrapperPlayClientPlayerFlying packet) throws IOException {
        boolean hasRot = packet.hasRotationChanged();
        boolean hasPos = packet.hasPositionChanged();
        if (!hasRot && !hasPos) {
            return;
        }
        var loc = packet.getLocation();
        float yaw = hasRot ? loc.getYaw() : data.lastYaw();
        float pitch = hasRot ? loc.getPitch() : data.lastPitch();
        double x = hasPos ? loc.getX() : Double.NaN;
        double y = hasPos ? loc.getY() : Double.NaN;
        double z = hasPos ? loc.getZ() : Double.NaN;
        long now = System.currentTimeMillis();

        if (capture != null) {
            if (data.hasLook()) {
                capture.syncLookFrom(data.lastYaw(), data.lastPitch(), true);
            }
            capture.writeLook(x, y, z, yaw, pitch, packet.isOnGround(), hasPos, hasRot);
        }

        if (!hasRot) {
            return;
        }

        float dYaw = data.hasLook() ? AimMath.deltaYaw(data.lastYaw(), yaw) : 0f;
        float dPitch = data.hasLook() ? (pitch - data.lastPitch()) : 0f;
        data.setLook(yaw, pitch);
        if (aimSnap.enabled()) {
            data.pushRot(now, Math.abs(dYaw), Math.abs(dPitch), aimSnap.windowSize());
            aimSnap.onRotSample(player, data, now);
        }
    }

    private void handleInteract(Player player, PlayerData data, WrapperPlayClientInteractEntity packet) {
        if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }
        int id = packet.getEntityId();
        Entity target = findEntity(player, id);
        attacks.onAttack(player, data, target, id, false, "packet", 80L);
    }

    private void handleSwing(Player player, PlayerData data, CaptureService.ActiveCapture capture)
            throws IOException {
        long now = System.currentTimeMillis();
        data.markCombat(now);
        if (capture != null) {
            capture.writeSwing();
        }
        // Fallback when InteractEntity never arrives on this host: swing + look ray.
        Entity target = AttackProbe.rayTarget(player, 4.5);
        if (target != null) {
            attacks.onAttack(player, data, target, target.getEntityId(), false, "swing_ray", 100L);
        }
    }

    private static Entity findEntity(Player player, int id) {
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getEntityId() == id) {
                return entity;
            }
        }
        return null;
    }
}
