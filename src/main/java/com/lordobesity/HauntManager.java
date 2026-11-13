package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Tras hundirse: sustos variados y, a los 5-10 min, vuelve gigante.
 * Sustos: te observa de lejos (screamer), a tu espalda (screamer), acechador que avanza cuando no lo miras,
 * ojos brillantes en la oscuridad, pasos que se acercan, apagon con caja de musica, solo sonidos y grietas a la pesadilla.
 */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class HauntManager {
    private static final DustParticleOptions EYE_GLOW = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.1f), 1.2f);

    private static class Eyes {
        ServerLevel level;
        UUID player;
        double x, y, z;
        int ticks = 0;
    }

    private static class Steps {
        ServerLevel level;
        UUID player;
        double x, y, z;
        int ticks = 0;
    }

    private static final List<Eyes> EYES = new ArrayList<>();
    private static final List<Steps> STEPS = new ArrayList<>();

    // ------------------------------------------------------------------ reloj del acecho
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) e.level;
        tickEyes(sl);
        tickSteps(sl);

        if (sl.dimension() != Level.OVERWORLD) return;
        if (sl.getGameTime() % 20 != 0) return;
        HauntData data = HauntData.get(sl.getServer());
        if (data.entries.isEmpty()) return;
        long now = sl.getGameTime();
        Iterator<HauntData.Entry> it = data.entries.iterator();
        while (it.hasNext()) {
            HauntData.Entry en = it.next();
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(en.player);
            boolean away = p == null || !p.isAlive() || p.isSpectator()
                    || NightmareManager.isNightmare(p.level().dimension());   // en la pesadilla el reloj se pausa
            if (away) {
                en.returnTime += 20;
                en.nextScare += 20;
                data.setDirty();
                continue;
            }
            if (now >= en.returnTime) {
                spawnMega(p);
                it.remove();
                data.setDirty();
                continue;
            }
            if (now >= en.nextScare) {
                spawnScare(p);
                en.nextScare = now + 300 + sl.random.nextInt(700);   // un susto cada 15-50 s
                data.setDirty();
            }
        }
    }

    // ------------------------------------------------------------------ utilidades
    /** Busca un punto del suelo a 'dist' bloques del jugador, en la direccion 'ang' (radianes). */
    private static double[] findSpot(ServerLevel sl, ServerPlayer p, double ang, double dist) {
        double x = p.getX() + (-Math.sin(ang)) * dist;
        double z = p.getZ() + Math.cos(ang) * dist;
        BlockPos probe = new BlockPos((int) Math.floor(x), p.getBlockY(), (int) Math.floor(z));
        if (!sl.hasChunkAt(probe)) return null;
        double y;
        if (sl.dimensionType().hasCeiling() || !sl.canSeeSky(p.blockPosition())) y = p.getY();
        else y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        return new double[]{x, y, z};
    }

    /** Solo sonido: algo respira, susurra, se rie, late o suena una caja de musica cerca de ti. */
    private static void audioScare(ServerLevel sl, ServerPlayer p) {
        double ang = sl.random.nextDouble() * Math.PI * 2;
        double dist = 10 + sl.random.nextDouble() * 12;
        double x = p.getX() + Math.cos(ang) * dist, z = p.getZ() + Math.sin(ang) * dist;
        SoundEvent[] pool = {ModSounds.WHISPER.get(), ModSounds.EVIL_LAUGH.get(), ModSounds.BREATH.get(),
                ModSounds.HEARTBEAT.get(), ModSounds.CHITTER.get(), ModSounds.DRONE.get(), ModSounds.CREAK.get(),
                ModSounds.KNOCK.get(), ModSounds.MUSIC_BOX.get()};
        sl.playSound(null, x, p.getY(), z, pool[sl.random.nextInt(pool.length)], SoundSource.HOSTILE, 3.0f, 0.8f + sl.random.nextFloat() * 0.3f);
    }

    /** kind 0 = te observa desde lejos (screamer al mirarlo), 1 = aparece a tu espalda (screamer al girarte). */
    public static boolean spawnScareEntity(ServerLevel sl, ServerPlayer p, int kind) {
        double yaw = Math.toRadians(p.getYRot());
        for (int i = 0; i < 12; i++) {
            double ang, dist;
            if (kind == 0) {
                ang = yaw + (sl.random.nextDouble() - 0.5) * Math.PI / 2.0;
                dist = 26 + sl.random.nextDouble() * 14;
            } else {
                ang = yaw + Math.PI + (sl.random.nextDouble() - 0.5) * 0.7;
                dist = 7 + sl.random.nextDouble() * 3;
            }
            double[] s = findSpot(sl, p, ang, dist);
            if (s == null) continue;
            LordObesityEntity e = ModEntities.LORD_OBESITY.get().create(sl);
            if (e == null) return false;
            e.moveTo(s[0], s[1], s[2], 0.0f, 0.0f);
            e.initScare(kind);
            sl.addFreshEntity(e);
            if (kind == 1) sl.playSound(null, p.blockPosition(), ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, 1.5f, 0.7f);
            return true;
        }
        return false;
    }

    /** Acechador: inmovil mientras lo miras, avanza cuando apartas la vista. */
    public static boolean spawnStalker(ServerLevel sl, ServerPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        for (int i = 0; i < 12; i++) {
            double ang = yaw + (sl.random.nextDouble() - 0.5) * Math.PI / 2.0;
            double[] s = findSpot(sl, p, ang, 30 + sl.random.nextDouble() * 8);
            if (s == null) continue;
            LordObesityEntity e = ModEntities.LORD_OBESITY.get().create(sl);
            if (e == null) return false;
            e.moveTo(s[0], s[1], s[2], 0.0f, 0.0f);
            e.initScare(4);
            sl.addFreshEntity(e);
            sl.playSound(null, s[0], s[1], s[2], ModSounds.SCRAPE.get(), SoundSource.HOSTILE, 2.5f, 0.7f);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ ojos en la oscuridad
    public static boolean spawnEyes(ServerLevel sl, ServerPlayer p) {
        double[] s = findSpot(sl, p, sl.random.nextDouble() * Math.PI * 2, 16 + sl.random.nextDouble() * 8);
        if (s == null) return false;
        Eyes e = new Eyes();
        e.level = sl;
        e.player = p.getUUID();
        e.x = s[0];
        e.y = s[1] + 1.7;
        e.z = s[2];
        EYES.add(e);
        return true;
    }

    private static void tickEyes(ServerLevel sl) {
        Iterator<Eyes> it = EYES.iterator();
        while (it.hasNext()) {
            Eyes e = it.next();
            if (e.level != sl) continue;
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(e.player);
            e.ticks++;
            if (p == null || p.level() != sl || e.ticks > 260) { it.remove(); continue; }
            double dx = p.getX() - e.x, dz = p.getZ() - e.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 7.0) {                                  // al acercarte, desaparecen con un susurro pegado al oido
                sl.playSound(null, e.x, e.y, e.z, ModSounds.WHISPER.get(), SoundSource.HOSTILE, 3.0f, 0.7f);
                sl.sendParticles(ParticleTypes.SMOKE, e.x, e.y - 0.5, e.z, 12, 0.3, 0.6, 0.3, 0.02);
                it.remove();
                continue;
            }
            if ((e.ticks % 56) < 46 && e.ticks % 2 == 0) {      // parpadean
                double ux = -dz / dist * 0.3, uz = dx / dist * 0.3;
                sl.sendParticles(EYE_GLOW, e.x + ux, e.y, e.z + uz, 2, 0.01, 0.01, 0.01, 0.0);
                sl.sendParticles(EYE_GLOW, e.x - ux, e.y, e.z - uz, 2, 0.01, 0.01, 0.01, 0.0);
            }
            if (e.ticks % 90 == 20) sl.playSound(null, e.x, e.y, e.z, ModSounds.BREATH.get(), SoundSource.HOSTILE, 1.6f, 0.8f);
        }
    }

    // ------------------------------------------------------------------ pasos que se acercan
    public static boolean spawnSteps(ServerLevel sl, ServerPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        double[] s = findSpot(sl, p, yaw + Math.PI + (sl.random.nextDouble() - 0.5) * 1.0, 16);
        if (s == null) return false;
        Steps st = new Steps();
        st.level = sl;
        st.player = p.getUUID();
        st.x = s[0];
        st.y = s[1];
        st.z = s[2];
        STEPS.add(st);
        return true;
    }

    private static void tickSteps(ServerLevel sl) {
        Iterator<Steps> it = STEPS.iterator();
        while (it.hasNext()) {
            Steps st = it.next();
            if (st.level != sl) continue;
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(st.player);
            st.ticks++;
            if (p == null || p.level() != sl || st.ticks > 400) { it.remove(); continue; }
            if (st.ticks % 9 != 0) continue;
            double dx = p.getX() - st.x, dz = p.getZ() - st.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist <= 2.6) {                                 // lo tienes encima: respiracion y susurro, nada mas
                sl.playSound(null, st.x, p.getY(), st.z, ModSounds.BREATH.get(), SoundSource.HOSTILE, 3.0f, 0.8f);
                sl.playSound(null, st.x, p.getY(), st.z, ModSounds.WHISPER.get(), SoundSource.HOSTILE, 2.5f, 0.8f);
                it.remove();
                continue;
            }
            st.x += dx / dist * 1.15;
            st.z += dz / dist * 1.15;
            st.y = p.getY();
            sl.playSound(null, st.x, st.y, st.z, ModSounds.FOOTSTEP.get(), SoundSource.HOSTILE, 2.4f, 0.7f + sl.random.nextFloat() * 0.15f);
        }
    }

    // ------------------------------------------------------------------ eleccion del susto
    public static void spawnScare(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        double r = sl.random.nextDouble();
        boolean ok;
        if (r < 0.10) { audioScare(sl, p); return; }
        else if (r < 0.28) ok = spawnScareEntity(sl, p, 0);
        else if (r < 0.42) ok = spawnScareEntity(sl, p, 1);
        else if (r < 0.56) ok = spawnStalker(sl, p);
        else if (r < 0.68) ok = NightmareManager.createCrack(p);          // grieta que lleva a una pesadilla
        else if (r < 0.80) ok = spawnEyes(sl, p);
        else if (r < 0.92) ok = spawnSteps(sl, p);
        else {                                                             // apagon con caja de musica
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
            sl.playSound(null, p.blockPosition(), ModSounds.MUSIC_BOX.get(), SoundSource.HOSTILE, 2.2f, 1.0f);
            sl.playSound(null, p.blockPosition(), ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, 2.0f, 0.7f);
            ok = spawnScareEntity(sl, p, 1);
        }
        if (!ok) audioScare(sl, p);
    }

    // ------------------------------------------------------------------ regreso del Devorador
    private static void spawnMega(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        LordObesityEntity boss = ModEntities.LORD_OBESITY.get().create(sl);
        if (boss == null) return;
        boss.initStage(LordObesityEntity.MEGA, p.getUUID());
        double[] best = null;
        for (int i = 0; i < 24; i++) {
            double ang = sl.random.nextDouble() * Math.PI * 2;
            double[] s = findSpot(sl, p, ang, 18 + sl.random.nextDouble() * 6);
            if (s == null) continue;
            boss.moveTo(s[0], s[1], s[2], 0.0f, 0.0f);
            if (sl.noCollision(boss)) { best = s; break; }
            if (best == null) best = s;
        }
        if (best == null) best = new double[]{p.getX() + 16, p.getY(), p.getZ()};
        boss.moveTo(best[0], best[1], best[2], sl.random.nextFloat() * 360.0f, 0.0f);
        boss.setTarget(p);
        sl.addFreshEntity(boss);

        sl.setWeatherParameters(0, 2400, true, true);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
        for (int i = 0; i < 4; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2, d = 4 + sl.random.nextDouble() * 8;
            LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
            if (b != null) {
                b.moveTo(best[0] + Math.cos(a) * d, best[1], best[2] + Math.sin(a) * d);
                b.setVisualOnly(true);
                sl.addFreshEntity(b);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, best[0], best[1] + 1, best[2], 3, 2, 0.5, 2, 0);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, best[0], best[1] + 1, best[2], 120, 2.5, 1.5, 2.5, 0.2);
        sl.playSound(null, p.blockPosition(), ModSounds.RUMBLE.get(), SoundSource.HOSTILE, 4.0f, 0.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.5f);
        p.sendSystemMessage(Component.literal("[Lord Obesity] ¡¡HE VUELTO... Y AHORA SOY ENORME!!"));
    }
}
