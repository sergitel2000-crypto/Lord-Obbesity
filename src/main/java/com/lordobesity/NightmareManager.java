package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Grietas en el suelo que te tragan y te llevan a una de tres pesadillas de 1 minuto:
 *  0 = mundo de estructuras abstractas, 1 = casa encantada, 2 = persecucion (huye de Lord Obesity y llega a la salida).
 */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class NightmareManager {
    public static final ResourceKey<Level> NIGHTMARE = key("nightmare");
    public static final ResourceKey<Level> HOUSE = key("nightmare_house");
    public static final ResourceKey<Level> CHASE = key("nightmare_chase");

    private static final int STAY_TICKS = 1200;      // 1 minuto en todos los mundos
    private static final DustParticleOptions BLACK = new DustParticleOptions(new Vector3f(0.03f, 0.0f, 0.0f), 1.8f);
    private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.1f), 1.1f);

    private static ResourceKey<Level> key(String name) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(LordObesityMod.MODID, name));
    }

    public static ResourceKey<Level> worldKey(int w) {
        return w == 1 ? HOUSE : (w == 2 ? CHASE : NIGHTMARE);
    }

    public static boolean isNightmare(ResourceKey<Level> k) {
        return NIGHTMARE.equals(k) || HOUSE.equals(k) || CHASE.equals(k);
    }

    private static class Crack {
        ServerLevel level;
        UUID player;
        List<Vec3> pts = new ArrayList<>();
        int ticks = 0;
    }

    private static class Sinking {
        ServerLevel level;
        UUID player;
        int world;
        int ticks = 0;
    }

    private static class Leaving {
        UUID player;
        int ticks = 0;
    }

    private static final List<Crack> CRACKS = new ArrayList<>();
    private static final List<Sinking> SINKING = new ArrayList<>();
    private static final List<Leaving> LEAVING = new ArrayList<>();

    // ------------------------------------------------------------------ crear la grieta
    public static boolean createCrack(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        if (isNightmare(sl.dimension())) return false;
        if (sl.getServer().getLevel(NIGHTMARE) == null) return false;
        for (Crack c : CRACKS) if (c.player.equals(p.getUUID())) return false;
        for (Sinking s : SINKING) if (s.player.equals(p.getUUID())) return false;

        RandomSource rnd = sl.random;
        double ang = Math.toRadians(p.getYRot()) + (rnd.nextDouble() - 0.5) * 0.9;
        double dist = 5.0 + rnd.nextDouble() * 3.0;
        double cx = p.getX() - Math.sin(ang) * dist, cz = p.getZ() + Math.cos(ang) * dist;
        double axis = rnd.nextDouble() * Math.PI;
        double ax = Math.cos(axis), az = Math.sin(axis);
        boolean sky = !sl.dimensionType().hasCeiling() && sl.canSeeSky(p.blockPosition());

        Crack c = new Crack();
        c.level = sl;
        c.player = p.getUUID();
        double off = 0;
        for (int i = -8; i <= 8; i++) {
            off += (rnd.nextDouble() - 0.5) * 0.9;
            double x = cx + ax * i * 0.8 - az * off;
            double z = cz + az * i * 0.8 + ax * off;
            double y = sky ? sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) : p.getY();
            c.pts.add(new Vec3(x, y, z));
        }
        CRACKS.add(c);
        sl.playSound(null, BlockPos.containing(cx, p.getY(), cz), ModSounds.CRACK.get(), SoundSource.HOSTILE, 3.5f, 1.0f);
        sl.playSound(null, p.blockPosition(), ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
        p.displayClientMessage(Component.literal("El suelo se resquebraja..."), true);
        return true;
    }

    // ------------------------------------------------------------------ tick
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) e.level;
        tickCracks(sl);
        tickSinking(sl);
        if (CHASE.equals(sl.dimension()) && sl.getGameTime() % 4 == 0) tickChase(sl);
        if (sl.dimension() == Level.OVERWORLD && sl.getGameTime() % 20 == 0) tickStays(sl);
    }

    private static boolean nearCrack(Crack c, ServerPlayer p) {
        for (int i = 0; i + 1 < c.pts.size(); i++) {
            Vec3 a = c.pts.get(i), b = c.pts.get(i + 1);
            double abx = b.x - a.x, abz = b.z - a.z;
            double len2 = abx * abx + abz * abz;
            double t = len2 < 1.0E-6 ? 0 : Math.max(0, Math.min(1, ((p.getX() - a.x) * abx + (p.getZ() - a.z) * abz) / len2));
            double dx = p.getX() - (a.x + abx * t), dz = p.getZ() - (a.z + abz * t);
            double y = a.y + (b.y - a.y) * t;
            if (dx * dx + dz * dz < 1.6 * 1.6 && Math.abs(p.getY() - y) < 2.5) return true;
        }
        return false;
    }

    private static void crackParticles(ServerLevel sl, Crack c) {
        for (int i = 0; i + 1 < c.pts.size(); i++) {
            Vec3 a = c.pts.get(i), b = c.pts.get(i + 1);
            for (int k = 0; k < 2; k++) {
                double t = k / 2.0;
                double x = a.x + (b.x - a.x) * t, y = a.y + (b.y - a.y) * t + 0.05, z = a.z + (b.z - a.z) * t;
                sl.sendParticles(BLACK, x, y, z, 2, 0.15, 0.0, 0.15, 0.0);
                if (sl.random.nextFloat() < 0.35f) sl.sendParticles(GLOW, x, y + 0.05, z, 1, 0.1, 0.0, 0.1, 0.0);
                if (sl.random.nextFloat() < 0.12f) sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.1, z, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private static void tickCracks(ServerLevel sl) {
        Iterator<Crack> it = CRACKS.iterator();
        while (it.hasNext()) {
            Crack c = it.next();
            if (c.level != sl) continue;
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(c.player);
            c.ticks++;
            if (p == null || p.level() != sl || c.ticks > 500) { it.remove(); continue; }
            if (c.ticks % 3 == 0) crackParticles(sl, c);
            if (c.ticks % 60 == 0) sl.playSound(null, p.blockPosition(), ModSounds.RUMBLE.get(), SoundSource.HOSTILE, 0.8f, 1.2f);
            if (nearCrack(c, p)) {
                startSinking(sl, p);
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ hundirse (elige mundo al azar)
    private static void startSinking(ServerLevel sl, ServerPlayer p) {
        MinecraftServer server = sl.getServer();
        List<Integer> worlds = new ArrayList<>();
        for (int w = 0; w < 3; w++) if (server.getLevel(worldKey(w)) != null) worlds.add(w);
        if (worlds.isEmpty()) return;
        Sinking s = new Sinking();
        s.level = sl;
        s.player = p.getUUID();
        s.world = worlds.get(sl.random.nextInt(worlds.size()));
        SINKING.add(s);
        sl.playSound(null, p.blockPosition(), ModSounds.INHALE.get(), SoundSource.HOSTILE, 2.5f, 0.7f);
        sl.playSound(null, p.blockPosition(), ModSounds.BOOM.get(), SoundSource.HOSTILE, 1.5f, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false));
        p.displayClientMessage(Component.literal("¡El suelo cede!"), true);
    }

    private static void tickSinking(ServerLevel sl) {
        Iterator<Sinking> it = SINKING.iterator();
        while (it.hasNext()) {
            Sinking s = it.next();
            if (s.level != sl) continue;
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(s.player);
            if (p == null || p.level() != sl) { it.remove(); continue; }
            s.ticks++;
            sl.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 0.2, p.getZ(), 4, 0.5, 0.1, 0.5, 0.1);
            sl.sendParticles(BLACK, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.6, 0.05, 0.6, 0.0);
            if (s.ticks == 22) {
                it.remove();
                enterWorld(sl, p, s.world);
            }
        }
    }

    // ------------------------------------------------------------------ entrar
    /** Entra ya en una pesadilla (comando de pruebas). world: 0, 1 o 2. */
    public static void enterNow(ServerPlayer p, int world) {
        if (isNightmare(p.level().dimension())) return;
        enterWorld(p.serverLevel(), p, world);
    }

    private static void enterWorld(ServerLevel from, ServerPlayer p, int world) {
        MinecraftServer server = from.getServer();
        ServerLevel nl = server.getLevel(worldKey(world));
        if (nl == null) {
            p.removeEffect(MobEffects.BLINDNESS);
            return;
        }
        NightmareData d = NightmareData.get(server);
        double x, y = 5.0, z;
        float yaw;
        String msg;
        switch (world) {
            case 1 -> {
                if (!d.builtHouse) { NightmareBuilder.buildHouse(nl); d.builtHouse = true; d.setDirty(); }
                x = 0.5; z = NightmareBuilder.HOUSE_SPAWN_Z + 0.5; yaw = 180.0f;
                msg = "La casa te estaba esperando...";
            }
            case 2 -> {
                NightmareBuilder.buildChase(nl);                         // laberinto nuevo cada vez
                x = NightmareBuilder.CHASE_START + 0.5; z = NightmareBuilder.CHASE_START + 0.5; yaw = -45.0f;
                msg = "¡Hay una luz al fondo! ¡Corre!";
            }
            default -> {
                if (!d.built) { NightmareBuilder.build(nl); d.built = true; d.setDirty(); }
                x = 0.5; z = 0.5; yaw = p.getYRot();
                msg = "Esto no es real... ¿verdad?";
            }
        }
        NightmareBuilder.ensureBook(nl, world);                      // el fragmento de lore de este mundo
        NightmareData.Entry en = new NightmareData.Entry();
        en.player = p.getUUID();
        en.returnTime = server.overworld().getGameTime() + STAY_TICKS;
        en.dim = from.dimension().location().toString();
        en.x = p.getX();
        en.y = p.getY();
        en.z = p.getZ();
        en.yaw = p.getYRot();
        en.pitch = p.getXRot();
        en.stage = 0;
        en.world = world;
        d.entries.add(en);
        d.setDirty();

        p.teleportTo(nl, x, y, z, yaw, 0.0f);
        p.fallDistance = 0.0f;
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, STAY_TICKS + 100, 0, false, false));
        nl.playSound(null, p.blockPosition(), ModSounds.DRONE.get(), SoundSource.AMBIENT, 2.0f, 1.0f);
        nl.playSound(null, p.blockPosition(), ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT, 2.0f, 0.7f);
        p.displayClientMessage(Component.literal(msg), true);
    }

    // ------------------------------------------------------------------ estancia (1 minuto)
    private static void tickStays(ServerLevel overworld) {
        MinecraftServer server = overworld.getServer();
        NightmareData d = NightmareData.get(server);
        if (d.entries.isEmpty()) return;
        long now = overworld.getGameTime();

        Iterator<NightmareData.Entry> it = d.entries.iterator();
        while (it.hasNext()) {
            NightmareData.Entry en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.player);
            if (p == null) { en.returnTime += 20; d.setDirty(); continue; }
            if (!isNightmare(p.level().dimension())) { it.remove(); d.setDirty(); continue; }   // murio o salio

            if (now >= en.returnTime) {
                leave(p, en);
                it.remove();
                d.setDirty();
                continue;
            }
            ServerLevel nl = p.serverLevel();
            long elapsed = STAY_TICKS - (en.returnTime - now);
            if (elapsed % 100 < 20) eerie(nl, p, en.world, elapsed);
            scheduleScares(nl, p, en, elapsed, d);
        }
    }

    private static void eerie(ServerLevel nl, ServerPlayer p, int world, long elapsed) {
        RandomSource r = nl.random;
        SoundEvent[] pool;
        if (world == 1) {
            pool = new SoundEvent[]{ModSounds.CREAK.get(), ModSounds.KNOCK.get(), ModSounds.FOOTSTEP.get(), ModSounds.WHISPER.get(),
                    ModSounds.MUSIC_BOX.get(), ModSounds.BREATH.get()};
        } else if (world == 2) {
            pool = new SoundEvent[]{ModSounds.HEARTBEAT.get(), ModSounds.BREATH.get(), ModSounds.EVIL_LAUGH.get(), ModSounds.SCRAPE.get()};
        } else {
            pool = new SoundEvent[]{ModSounds.WHISPER.get(), ModSounds.EVIL_LAUGH.get(), ModSounds.BREATH.get(),
                    ModSounds.HEARTBEAT.get(), ModSounds.CHITTER.get()};
        }
        double a = r.nextDouble() * Math.PI * 2, dist = 8 + r.nextDouble() * 10;
        nl.playSound(null, p.getX() + Math.cos(a) * dist, p.getY(), p.getZ() + Math.sin(a) * dist,
                pool[r.nextInt(pool.length)], SoundSource.HOSTILE, 2.5f, 0.8f + r.nextFloat() * 0.3f);
        if (world != 2 && elapsed % 400 < 20) nl.playSound(null, p.blockPosition(), ModSounds.DRONE.get(), SoundSource.AMBIENT, 2.0f, 1.0f);
    }

    private static void scheduleScares(ServerLevel nl, ServerPlayer p, NightmareData.Entry en, long elapsed, NightmareData d) {
        if (en.world == 2) {                               // persecucion: a los 6 s despierta el perseguidor
            if (en.stage == 0 && elapsed >= 120) {
                en.stage = 1;
                d.setDirty();
                spawnChaser(nl, p);
            }
            return;
        }
        int[] marks = en.world == 1 ? new int[]{70, 260, 480, 760} : new int[]{100, 380, 640, 900};
        for (int i = 0; i < marks.length; i++) {
            if (en.stage == i && elapsed >= marks[i]) {
                en.stage = i + 1;
                d.setDirty();
                scared(nl, p, en.world, i);
            }
        }
    }

    private static void scared(ServerLevel nl, ServerPlayer p, int world, int i) {
        if (world == 1) {
            switch (i) {
                case 0 -> HauntManager.spawnStalker(nl, p);
                case 1 -> HauntManager.spawnScareEntity(nl, p, 1);
                case 2 -> HauntManager.spawnEyes(nl, p);
                default -> HauntManager.spawnSteps(nl, p);
            }
        } else {
            switch (i) {
                case 0 -> HauntManager.spawnScareEntity(nl, p, 0);
                case 1 -> HauntManager.spawnScareEntity(nl, p, 1);
                case 2 -> HauntManager.spawnStalker(nl, p);
                default -> {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
                    HauntManager.spawnScareEntity(nl, p, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ persecucion
    private static void spawnChaser(ServerLevel nl, ServerPlayer p) {
        LordObesityEntity c = ModEntities.LORD_OBESITY.get().create(nl);
        if (c == null) return;
        c.moveTo(NightmareBuilder.CHASE_START + 0.5, 5.0, NightmareBuilder.CHASE_START + 0.5, 0.0f, 0.0f);
        c.initChaser(p);
        nl.addFreshEntity(c);
        p.displayClientMessage(Component.literal("¡Algo ha despertado! ¡CORRE!"), true);
    }

    private static List<LordObesityEntity> chasers(ServerLevel nl, ServerPlayer p) {
        return nl.getEntitiesOfClass(LordObesityEntity.class, p.getBoundingBox().inflate(160),
                x -> x.getStage() == LordObesityEntity.CHASER && x.isAlive());
    }

    private static void tickChase(ServerLevel nl) {
        MinecraftServer server = nl.getServer();
        NightmareData d = NightmareData.get(server);

        // cuenta atras tras ser atrapado (deja que se vea el screamer un instante)
        Iterator<Leaving> lv = LEAVING.iterator();
        while (lv.hasNext()) {
            Leaving l = lv.next();
            l.ticks += 4;
            if (l.ticks < 16) continue;
            lv.remove();
            for (Iterator<NightmareData.Entry> it = d.entries.iterator(); it.hasNext(); ) {
                NightmareData.Entry en = it.next();
                if (!en.player.equals(l.player)) continue;
                ServerPlayer p = server.getPlayerList().getPlayer(l.player);
                if (p != null && CHASE.equals(p.level().dimension())) leave(p, en);
                it.remove();
                d.setDirty();
            }
        }

        for (NightmareData.Entry en : new ArrayList<>(d.entries)) {
            if (en.world != 2) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(en.player);
            if (p == null || p.level() != nl) continue;
            boolean already = false;
            for (Leaving l : LEAVING) if (l.player.equals(en.player)) already = true;
            if (already) continue;

            // salida
            double dx = p.getX() - (NightmareBuilder.CHASE_EXIT + 0.5), dz = p.getZ() - (NightmareBuilder.CHASE_EXIT + 0.5);
            if (dx * dx + dz * dz < 3.2 * 3.2) {
                nl.playSound(null, p.blockPosition(), ModSounds.SHIMMER.get(), SoundSource.PLAYERS, 2.5f, 1.0f);
                p.displayClientMessage(Component.literal("¡Has escapado!"), true);
                leave(p, en);
                d.entries.remove(en);
                d.setDirty();
                continue;
            }
            // atrapado
            for (LordObesityEntity c : chasers(nl, p)) {
                if (c.distanceTo(p) < 2.3f) {
                    c.setState(LordObesityEntity.SCREAM);
                    nl.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.SCREAM.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
                    nl.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.STING.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                    p.displayClientMessage(Component.literal("Te ha atrapado..."), true);
                    Leaving l = new Leaving();
                    l.player = p.getUUID();
                    LEAVING.add(l);
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ salir
    private static void leave(ServerPlayer p, NightmareData.Entry en) {
        MinecraftServer server = p.server;
        ServerLevel nl = p.serverLevel();
        ServerLevel back = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(en.dim)));
        double x = en.x, y = en.y, z = en.z;
        if (back == null) {
            back = server.overworld();
            BlockPos sp = back.getSharedSpawnPos();
            x = sp.getX() + 0.5;
            y = sp.getY();
            z = sp.getZ() + 0.5;
        }
        List<Entity> remove = new ArrayList<>();
        for (Entity e : nl.getAllEntities()) {
            if (e instanceof LordObesityEntity lo && (lo.getStage() == LordObesityEntity.SCARE || lo.getStage() == LordObesityEntity.CHASER)) {
                remove.add(e);
            }
        }
        for (Entity e : remove) e.discard();

        p.removeEffect(MobEffects.NIGHT_VISION);
        p.removeEffect(MobEffects.DARKNESS);
        p.teleportTo(back, x, y + 0.2, z, en.yaw, en.pitch);
        p.fallDistance = 0.0f;
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        back.playSound(null, p.blockPosition(), ModSounds.INHALE.get(), SoundSource.AMBIENT, 2.0f, 1.2f);
        back.playSound(null, p.blockPosition(), ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT, 2.0f, 1.0f);
        p.displayClientMessage(Component.literal("Despiertas... ¿ha sido un sueño?"), true);
    }
}
