package com.lordobesity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class MaskEvents {
    /** Cuanto mas grande te hace la mascara (hitbox y vista). */
    public static final float SIZE = 1.5f;

    private static final WeakHashMap<Player, Boolean> WORN = new WeakHashMap<>();

    public static boolean wearsMask(Player p) {
        return p.getInventory() != null && p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LORD_OBESITY_MASK.get());
    }

    /** Cambia el tamano del jugador mientras lleva la mascara. */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size e) {
        if (e.getEntity() instanceof Player p && wearsMask(p)) {
            e.setNewSize(e.getNewSize().scale(SIZE));
            e.setNewEyeHeight(e.getNewEyeHeight() * SIZE);
        }
    }

    /** Detecta ponerse / quitarse la mascara para recalcular el tamano. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Player p = e.player;
        boolean worn = wearsMask(p);
        Boolean last = WORN.get(p);
        if (last == null) last = Boolean.FALSE;
        if (worn != last) {
            WORN.put(p, worn);
            p.refreshDimensions();
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.level().isClientSide) return;
        if (!wearsMask(p)) return;
        if (e.getDistance() < 3.0f) return;

        float dmg = Math.min(e.getDistance() * 1.5f, 20.0f);
        e.setDamageMultiplier(0.25f);
        AABB box = p.getBoundingBox().inflate(5.0, 1.5, 5.0);
        for (LivingEntity t : p.level().getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive())) {
            t.hurt(p.damageSources().playerAttack(p), dmg);
            Vec3 dir = t.position().subtract(p.position()).normalize();
            t.push(dir.x * 0.9, 0.5, dir.z * 0.9);
            t.hurtMarked = true;
        }
        p.level().playSound(null, p.blockPosition(), ModSounds.SLAM.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 50, 2.5, 0.2, 2.5, 0.05);
        }
    }
}
