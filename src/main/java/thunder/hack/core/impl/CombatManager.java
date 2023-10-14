package thunder.hack.core.impl;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.IManager;
import thunder.hack.events.impl.EventPostTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.modules.Module;
import thunder.hack.modules.combat.AntiBot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CombatManager implements IManager {
    public HashMap<String, Integer> popList = new HashMap<>();

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (Module.fullNullCheck()) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket pac) {
            if (pac.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
                Entity ent = pac.getEntity(MC.world);
                if (!(ent instanceof PlayerEntity)) return;
                if (popList == null) {
                    popList = new HashMap<>();
                }
                if (popList.get(ent.getName().getString()) == null) {
                    popList.put(ent.getName().getString(), 1);
                } else if (popList.get(ent.getName().getString()) != null) {
                    popList.put(ent.getName().getString(), popList.get(ent.getName().getString()) + 1);
                }
                ThunderHack.EVENT_BUS.post(new TotemPopEvent((PlayerEntity) ent, popList.get(ent.getName().getString())));
            }
        }
    }

    @EventHandler
    public void onPostTick(EventPostTick event) {
        if (Module.fullNullCheck())
            return;
        for (PlayerEntity player : MC.world.getPlayers()) {
            if (AntiBot.bots.contains(player)) continue;

            if (player.getHealth() <= 0 && popList.containsKey(player.getName().getString()))
                popList.remove(player.getName().getString(), popList.get(player.getName().getString()));
        }
    }

    public int getPops(@NotNull PlayerEntity entity) {
        if (popList.get(entity.getName().getString()) == null) return 0;
        return popList.get(entity.getName().getString());
    }

    public List<PlayerEntity> getTargets(float range) {
        return MC.world.getPlayers().stream()
                .filter(e -> !e.isDead())
                .filter(entityPlayer -> !ThunderHack.friendManager.isFriend(entityPlayer.getName().getString()))
                .filter(entityPlayer -> entityPlayer != MC.player)
                .filter(entityPlayer -> MC.player.squaredDistanceTo(entityPlayer) < range * range)
                .sorted(Comparator.comparing(e -> MC.player.distanceTo(e)))
                .collect(Collectors.toList());
    }

    public PlayerEntity getNearestTarget(float range) {
        return getTargets(range).stream().min(Comparator.comparing(t -> MC.player.distanceTo(t))).orElse(null);
    }

    public PlayerEntity getTargetByHP(float range) {
        return getTargets(range).stream().min(Comparator.comparing(t -> (t.getHealth() + t.getAbsorptionAmount()))).orElse(null);
    }

    public PlayerEntity getTargetByFOV(float range) {
        return getTargets(range).stream().min(Comparator.comparing(this::getFOVAngle)).orElse(null);
    }

    private float getFOVAngle(@NotNull LivingEntity e) {
        double difX = e.getX() - MC.player.getPos().x;
        double difZ = e.getZ() - MC.player.getPos().z;
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        double plYaw = MathHelper.wrapDegrees(MC.player.getYaw());
        return (float) Math.abs(yaw - plYaw);
    }
}
