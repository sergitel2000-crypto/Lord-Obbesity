package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;

/** Tras hundirse: da sustos a lo lejos y, pasados 5-15 min, vuelve gigante. */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class HauntManager {
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) e.level;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (sl.getGameTime() % 20 != 0) return;

        HauntData data = HauntData.get(sl.getServer());
        if (data.entries.isEmpty()) return;
        long now = sl.getGameTime();
        Iterator<HauntData.Entry> it = data.entries.iterator();
        while (it.hasNext()) {
            HauntData.Entry en = it.next();
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(en.player);
            if (p == null || !p.isAlive() || p.isSpectator()) {
                // el reloj se pausa mientras el jugador no esta
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
                en.nextScare = now + 700 + sl.random.nextInt(1500);
                data.setDirty();
            }
        }
    }

    /** Busca un punto del suelo a 'dist' bloques del jugador, en la direccion 'ang' (radianes). */
    private static double[] findSpot(ServerLevel sl, ServerPlayer p, double ang, double dist) {
        double x = p.getX() + (-Math.sin(ang)) * dist;
        double z = p.getZ() + Math.cos(ang) * dist;
        BlockPos probe = new BlockPos((int) Math.floor(x), p.getBlockY(), (int) Math.floor(z));
        if (!sl.hasChunkAt(probe)) return null;
        double y;
        if (sl.dimensionType().hasCeiling()) y = p.getY();
        else y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        return new double[]{x, y, z};
    }

    private static void spawnScare(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        double yaw = Math.toRadians(p.getYRot());
        for (int i = 0; i < 12; i++) {
            double ang = yaw + (sl.random.nextDouble() - 0.5) * Math.PI / 2.0;
            double dist = 24 + sl.random.nextDouble() * 12;
            double[] s = findSpot(sl, p, ang, dist);
            if (s == null) continue;
            LordObesityEntity e = ModEntities.LORD_OBESITY.get().create(sl);
            if (e == null) return;
            e.moveTo(s[0], s[1], s[2], 0.0f, 0.0f);
            e.initScare();
            sl.addFreshEntity(e);
            return;
        }
    }

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

        LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
        if (b != null) {
            b.moveTo(best[0], best[1], best[2]);
            b.setVisualOnly(true);
            sl.addFreshEntity(b);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, best[0], best[1] + 1, best[2], 3, 2, 0.5, 2, 0);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.5f);
        p.sendSystemMessage(Component.literal("[Lord Obesity] ¡¡HE VUELTO... Y AHORA SOY ENORME!!"));
    }
}
