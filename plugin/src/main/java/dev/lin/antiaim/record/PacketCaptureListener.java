package dev.lin.antiaim.record;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;

public final class PacketCaptureListener extends PacketListenerAbstract {
    private final CaptureService captures;

    public PacketCaptureListener(CaptureService captures) {
        super(PacketListenerPriority.MONITOR);
        this.captures = captures;
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
        CaptureService.ActiveCapture capture = captures.get(uuid);
        if (capture == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        try {
            if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                handleFlying(capture, new WrapperPlayClientPlayerFlying(event));
                return;
            }
            if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                handleAttack(capture, player, new WrapperPlayClientInteractEntity(event));
                return;
            }
            if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                capture.writeRaw(JsonLine.swing());
                capture.swingCount++;
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[LinAntiAim] write failed: " + e.getMessage());
        }
    }

    private void handleFlying(CaptureService.ActiveCapture capture, WrapperPlayClientPlayerFlying packet) throws IOException {
        boolean hasRot = packet.hasRotationChanged();
        boolean hasPos = packet.hasPositionChanged();
        if (!hasRot && !hasPos) {
            return;
        }
        var loc = packet.getLocation();
        float yaw = hasRot ? loc.getYaw() : capture.lastYaw;
        float pitch = hasRot ? loc.getPitch() : capture.lastPitch;
        double x = hasPos ? loc.getX() : Double.NaN;
        double y = hasPos ? loc.getY() : Double.NaN;
        double z = hasPos ? loc.getZ() : Double.NaN;

        capture.writeRaw(JsonLine.look(capture, x, y, z, yaw, pitch, packet.isOnGround(), hasPos, hasRot));
        if (hasRot) {
            capture.lastYaw = yaw;
            capture.lastPitch = pitch;
            capture.hasLook = true;
        }
        capture.lookCount++;
    }

    private void handleAttack(CaptureService.ActiveCapture capture, Player player,
                              WrapperPlayClientInteractEntity packet) throws IOException {
        if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }
        int id = packet.getEntityId();
        Entity target = null;
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getEntityId() == id) {
                target = entity;
                break;
            }
        }
        float yaw = capture.hasLook ? capture.lastYaw : player.getLocation().getYaw();
        float pitch = capture.hasLook ? capture.lastPitch : player.getLocation().getPitch();
        if (target == null) {
            capture.writeRaw("{\"type\":\"attack\",\"t\":" + System.currentTimeMillis()
                    + ",\"yaw\":" + yaw + ",\"pitch\":" + pitch
                    + ",\"targetId\":" + id + ",\"targetType\":\"UNKNOWN\",\"alignDeg\":-1}");
        } else {
            capture.writeRaw(JsonLine.attack(capture, player, target, yaw, pitch));
        }
        capture.attackCount++;
    }
}
