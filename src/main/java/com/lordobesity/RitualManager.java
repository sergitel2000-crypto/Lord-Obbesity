package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Ritual: con un caldero LLENO DE AGUA, se tiran dentro (tecla de soltar) el sombrero, la chaqueta y el baston.
 * El agua se va tiñendo de amarillo y, con las tres piezas, empieza la invocacion.
 */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class RitualManager {
    private static class Ritual {
        ServerLevel level;
        BlockPos pos;
        UUID owner;
        int t = 0;
    }

    private static final List<Ritual> ACTIVE = new ArrayList<>();
    private static final List<ItemEntity> TRACKED = new ArrayList<>();
    private static final DustParticleOptions YELLOW_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.1f), 1.3f);
    private static final int FINALE = 230;

    private static int pieceBit(ItemStack st) {
        if (st.is(ModItems.LORD_OBESITY_HAT.get())) return 1;
        if (st.is(ModItems.LORD_OBESITY_JACKET.get())) return 2;
        if (st.is(ModItems.LORD_OBESITY_CANE.get())) return 4;
        return 0;
    }

    private static boolean isActive(ServerLevel l, BlockPos pos) {
        for (Ritual r : ACTIVE) if (r.level == l && r.pos.equals(pos)) return true;
        return false;
    }

    private static void notifyNear(ServerLevel l, BlockPos pos, String text) {
        for (ServerPlayer p : l.players()) {
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 20 * 20) {
                p.displayClientMessage(Component.literal(text), true);
            }
        }
    }

    // ------------------------------------------------------------------ objetos tirados al caldero
    @SubscribeEvent
    public static void onItemJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof ItemEntity ie && pieceBit(ie.getItem()) != 0 && !TRACKED.contains(ie)) {
            TRACKED.add(ie);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) e.level;

        tickItems(sl);
        if (sl.getGameTime() % 10 == 0) tickTint(sl);

        Iterator<Ritual> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Ritual r = it.next();
            if (r.level != sl) continue;
            if (tickRitual(r)) it.remove();
        }
    }

    private static void tickItems(ServerLevel sl) {
        Iterator<ItemEntity> it = TRACKED.iterator();
        while (it.hasNext()) {
            ItemEntity ie = it.next();
            if (!ie.isAlive()) { it.remove(); continue; }
            if (ie.level() != sl) continue;
            int bit = pieceBit(ie.getItem());
            if (bit == 0) { it.remove(); continue; }

            BlockPos pos = ie.blockPosition();
            BlockState bs = sl.getBlockState(pos);
            boolean water = bs.is(Blocks.WATER_CAULDRON) && bs.getValue(LayeredCauldronBlock.LEVEL) == 3;
            boolean ritual = bs.is(ModBlocks.RITUAL_CAULDRON.get());
            if (!water && !ritual) {
                if (bs.getBlock() instanceof AbstractCauldronBlock && ie.tickCount % 40 == 0) {
                    notifyNear(sl, pos, "El caldero tiene que estar lleno de agua");
                }
                continue;
            }
            if (ie.getY() > pos.getY() + 0.95) continue;
            if (isActive(sl, pos)) continue;

            RitualData data = RitualData.get(sl);
            int mask = data.getMask(pos);
            if ((mask & bit) != 0) {
                if (ie.tickCount % 40 == 0) notifyNear(sl, pos, "Esa pieza ya está en el caldero");
                continue;
            }

            ItemStack st = ie.getItem();
            if (st.getCount() > 1) { st.shrink(1); ie.setItem(st); } else { ie.discard(); }

            UUID owner = null;
            Entity thrower = ie.getOwner();
            if (thrower instanceof Player pl) owner = pl.getUUID();

            if (water) sl.setBlock(pos, ModBlocks.RITUAL_CAULDRON.get().defaultBlockState(), 3);
            mask |= bit;
            data.setMask(pos, mask);
            int count = Integer.bitCount(mask);

            Vec3 c = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            sl.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.3f, 0.9f);
            sl.playSound(null, pos, ModSounds.SHIMMER.get(), SoundSource.BLOCKS, 0.8f, 0.7f + 0.15f * count);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 40, 0.3, 0.1, 0.3, 0.2);
            sl.sendParticles(YELLOW_DUST, c.x, c.y + 0.2, c.z, 30, 0.35, 0.2, 0.35, 0.0);
            sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.3, c.z, 12, 0.2, 0.4, 0.2, 0.03);
            notifyNear(sl, pos, "Piezas en el caldero: " + count + "/3");

            if (mask == 7) startRitual(sl, pos, owner);
        }
    }

    private static void startRitual(ServerLevel sl, BlockPos pos, UUID owner) {
        if (isActive(sl, pos)) return;
        Ritual r = new Ritual();
        r.level = sl;
        r.pos = pos.immutable();
        r.owner = owner;
        ACTIVE.add(r);
    }

    /** El agua se va volviendo amarilla paso a paso segun las piezas echadas. */
    private static void tickTint(ServerLevel sl) {
        RitualData data = RitualData.get(sl);
        for (long key : data.positions()) {
            BlockPos pos = BlockPos.of(key);
            if (!sl.isLoaded(pos)) continue;
            BlockState bs = sl.getBlockState(pos);
            if (!bs.is(ModBlocks.RITUAL_CAULDRON.get())) { data.remove(pos); continue; }
            int mask = data.getMask(pos);
            int target = Math.min(9, Integer.bitCount(mask) * 3);
            int tint = bs.getValue(RitualCauldronBlock.TINT);
            if (tint < target) {
                sl.setBlock(pos, bs.setValue(RitualCauldronBlock.TINT, tint + 1), 3);
                sl.sendParticles(YELLOW_DUST, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.0);
                sl.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.02);
            } else if (mask == 7 && tint >= 9 && !isActive(sl, pos)) {
                startRitual(sl, pos, null);   // reanuda un ritual interrumpido al recargar el mundo
            }
        }
    }

    // ------------------------------------------------------------------ animacion
    private static void bolt(ServerLevel l, double x, double y, double z) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(l);
        if (b == null) return;
        b.moveTo(x, y, z);
        b.setVisualOnly(true);
        l.addFreshEntity(b);
    }

    private static void playersNear(ServerLevel l, Vec3 c, double r, java.util.function.Consumer<ServerPlayer> f) {
        for (ServerPlayer p : l.players()) if (p.distanceToSqr(c) < r * r) f.accept(p);
    }

    /** Devuelve true cuando el ritual ha terminado. */
    private static boolean tickRitual(Ritual r) {
        ServerLevel l = r.level;
        BlockPos bp = r.pos;
        Vec3 c = new Vec3(bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);
        double gy = bp.getY() + 1.05;
        RandomSource rnd = l.random;
        int t = r.t++;
        double R = 4.0;

        // ---- 0: despertar
        if (t == 0) {
            l.playSound(null, bp, ModSounds.RUMBLE.get(), SoundSource.AMBIENT, 4.0f, 1.0f);
            l.playSound(null, bp, ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT, 3.0f, 0.8f);
            l.setWeatherParameters(0, 1800, true, true);
            playersNear(l, c, 28, p -> {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 280, 0, false, false));
                p.displayClientMessage(Component.literal("El agua se agita... algo escucha."), true);
            });
        }
        if (t == 12 || t == 112) l.playSound(null, bp, ModSounds.WHISPER.get(), SoundSource.AMBIENT, 3.0f, 0.9f);
        if (t < 200 && t % 20 == 10) {
            l.playSound(null, bp, ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT, 2.5f, 0.7f + t / 330f);
        }

        // ---- agua hirviendo
        if (t < 215 && t % 3 == 0) {
            l.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y, c.z, 8, 0.28, 0.05, 0.28, 0.02);
            l.sendParticles(YELLOW_DUST, c.x, c.y + 0.15, c.z, 6 + t / 30, 0.3, 0.15, 0.3, 0.0);
            if (t > 40) l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 0.2, c.z, 2, 0.2, 0.3, 0.2, 0.02);
        }

        // ---- circulo de invocacion que se va dibujando
        if (t >= 50 && t < 215 && t % 2 == 0) {
            double p = Math.min(1.0, (t - 50) / 110.0);
            int n = 36, shown = (int) (p * n);
            for (int i = 0; i < shown; i++) {
                double a = i * Math.PI * 2 / n;
                l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * R, gy, c.z + Math.sin(a) * R, 1, 0.02, 0, 0.02, 0.0);
                l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * (R - 0.6), gy, c.z + Math.sin(a) * (R - 0.6), 1, 0.02, 0, 0.02, 0.0);
            }
            if (p > 0.55) {                               // estrella de cinco puntas
                for (int k = 0; k < 5; k++) {
                    double a0 = k * Math.PI * 2 / 5 - Math.PI / 2, a1 = ((k + 2) % 5) * Math.PI * 2 / 5 - Math.PI / 2;
                    for (int s = 0; s < 9; s++) {
                        double q = s / 9.0;
                        double x = Math.cos(a0) * R * (1 - q) + Math.cos(a1) * R * q;
                        double z = Math.sin(a0) * R * (1 - q) + Math.sin(a1) * R * q;
                        l.sendParticles(ParticleTypes.END_ROD, c.x + x, gy + 0.02, c.z + z, 1, 0, 0, 0, 0.0);
                    }
                }
            }
        }
        // almas que suben en espiral
        if (t >= 50 && t < 215 && t % 3 == 0) {
            for (int j = 0; j < 6; j++) {
                double a = t * 0.16 + j * Math.PI / 3;
                double h = ((t * 0.05 + j * 0.45) % 3.0);
                l.sendParticles(ParticleTypes.SOUL, c.x + Math.cos(a) * 2.4, gy + h, c.z + Math.sin(a) * 2.4, 1, 0, 0.02, 0, 0.0);
            }
        }

        // ---- el suelo se agrieta
        if (t == 70 || t == 95 || t == 120) {
            BlockState below = l.getBlockState(bp.below());
            if (!below.isAir()) {
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), c.x, gy, c.z, 60, 2.0, 0.1, 2.0, 0.3);
            }
            l.playSound(null, bp, SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT, 1.2f, 0.6f);
        }

        // ---- tormenta que se acerca
        if (t >= 100 && t <= 170 && (t - 100) % 14 == 0) {
            double rad = 13.0 - (t - 100) / 70.0 * 7.0;
            double a = rnd.nextDouble() * Math.PI * 2;
            double x = c.x + Math.cos(a) * rad, z = c.z + Math.sin(a) * rad;
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            bolt(l, x, y, z);
        }
        if (t == 150) l.playSound(null, bp, ModSounds.SHIMMER.get(), SoundSource.AMBIENT, 2.5f, 0.55f);
        if (t == 170) {
            l.playSound(null, bp, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.AMBIENT, 2.0f, 0.5f);
            l.playSound(null, bp, ModSounds.STING.get(), SoundSource.AMBIENT, 1.5f, 0.6f);
        }

        // ---- la energia converge en el caldero
        if (t >= 170 && t < FINALE) {
            double q = (t - 170) / (double) (FINALE - 170);
            for (int k = 0; k < 5; k++) {
                double a = k * Math.PI * 2 / 5 - Math.PI / 2;
                double vx = Math.cos(a) * R, vz = Math.sin(a) * R;
                double x = vx * (1 - q), z = vz * (1 - q);
                l.sendParticles(ParticleTypes.END_ROD, c.x + x, gy + q * 2.6, c.z + z, 2, 0.04, 0.04, 0.04, 0.0);
                l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + vx, gy + rnd.nextDouble() * 3.2, c.z + vz, 1, 0.08, 0.0, 0.08, 0.0);
            }
            if (t % 6 == 0) l.sendParticles(ParticleTypes.CLOUD, c.x, gy, c.z, 0, Math.cos(t), 0.02, Math.sin(t), 0.35);
        }
        if (t == 205) {
            l.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 1.2, c.z, 1, 0, 0, 0, 0);
            l.playSound(null, bp, ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT, 3.5f, 1.3f);
        }

        // ---- 230: aparicion
        if (t == FINALE) {
            bolt(l, c.x, bp.getY(), c.z);
            l.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1.5, c.z, 2, 0, 0, 0, 0);
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 2, 0.5, 0.2, 0.5, 0);
            l.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.5, c.z, 400, 0.4, 0.8, 0.4, 0.6);
            l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1.0, c.z, 200, 0.5, 0.6, 0.5, 0.4);
            l.sendParticles(YELLOW_DUST, c.x, c.y + 1.5, c.z, 120, 1.5, 1.5, 1.5, 0.0);
            for (int k = 0; k < 48; k++) {
                double a = k * Math.PI * 2 / 48;
                l.sendParticles(ParticleTypes.CLOUD, c.x, gy, c.z, 0, Math.cos(a), 0.02, Math.sin(a), 1.0);
                l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, gy, c.z, 0, Math.cos(a), 0.05, Math.sin(a), 0.7);
            }
            l.playSound(null, bp, ModSounds.BOOM.get(), SoundSource.HOSTILE, 5.0f, 0.9f);
            l.playSound(null, bp, ModSounds.EVIL_LAUGH.get(), SoundSource.HOSTILE, 4.0f, 0.85f);
            l.playSound(null, bp, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.6f);
            playersNear(l, c, 12, p -> {
                Vec3 d = p.position().subtract(c);
                Vec3 flat = new Vec3(d.x, 0, d.z);
                flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
                p.push(flat.x * 0.9, 0.5, flat.z * 0.9);
                p.hurtMarked = true;
            });

            LordObesityEntity boss = ModEntities.LORD_OBESITY.get().create(l);
            if (boss != null) {
                boss.moveTo(c.x, c.y, c.z, rnd.nextFloat() * 360.0f, 0.0f);
                boss.initStage(LordObesityEntity.MERCHANT, r.owner);
                l.addFreshEntity(boss);
            }
            // el agua se evapora
            l.setBlock(bp, Blocks.CAULDRON.defaultBlockState(), 3);
            RitualData.get(l).remove(bp);
            l.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 40, 0.3, 0.6, 0.3, 0.05);
            return true;
        }
        return false;
    }
}
