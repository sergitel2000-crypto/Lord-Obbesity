package com.lordobesity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mueve al jugador mientras el gancho del baston esta activo. */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class CaneEvents {
    private static void stop(Player p, CompoundTag d) {
        d.remove("lo_ht");
        d.remove("lo_hx");
        d.remove("lo_hy");
        d.remove("lo_hz");
        p.setDeltaMovement(p.getDeltaMovement().scale(0.6));
        p.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Player p = e.player;
        if (p.level().isClientSide) return;
        CompoundTag d = p.getPersistentData();
        if (!d.contains("lo_ht")) return;

        int t = d.getInt("lo_ht");
        Vec3 target = new Vec3(d.getDouble("lo_hx"), d.getDouble("lo_hy"), d.getDouble("lo_hz"));
        Vec3 to = target.subtract(p.getEyePosition());
        boolean holding = p.getMainHandItem().getItem() instanceof LordObesityCaneItem
                || p.getOffhandItem().getItem() instanceof LordObesityCaneItem;
        if (t <= 0 || to.length() < 2.0 || !holding || !p.isAlive()) {
            stop(p, d);
            return;
        }
        p.setDeltaMovement(to.normalize().scale(1.15));
        p.fallDistance = 0.0f;
        p.hurtMarked = true;
        d.putInt("lo_ht", t - 1);

        if (p.level() instanceof ServerLevel sl && t % 3 == 0) {
            double len = to.length();
            Vec3 dir = to.normalize();
            Vec3 start = p.getEyePosition().add(0, -0.4, 0);
            for (double s = 1.0; s < len; s += 1.5) {
                sl.sendParticles(ParticleTypes.END_ROD, start.x + dir.x * s, start.y + dir.y * s, start.z + dir.z * s, 1, 0, 0, 0, 0);
            }
        }
    }
}
