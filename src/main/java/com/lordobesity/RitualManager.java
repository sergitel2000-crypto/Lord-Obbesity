package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Echar sombrero, chaqueta y baston en un caldero invoca a Lord Obesity con una animacion. */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class RitualManager {
    private static class Ritual {
        ServerLevel level;
        BlockPos pos;
        UUID owner;
        int t = 0;
    }

    private static final List<Ritual> ACTIVE = new ArrayList<>();

    private static int pieceBit(ItemStack st) {
        if (st.is(ModItems.LORD_OBESITY_HAT.get())) return 1;
        if (st.is(ModItems.LORD_OBESITY_JACKET.get())) return 2;
        if (st.is(ModItems.LORD_OBESITY_CANE.get())) return 4;
        return 0;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack stack = e.getItemStack();
        int bit = pieceBit(stack);
        if (bit == 0) return;
        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof AbstractCauldronBlock)) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;

        Player p = e.getEntity();
        RitualData data = RitualData.get(sl);
        int mask = data.getMask(pos);
        if ((mask & bit) != 0) {
            p.displayClientMessage(Component.literal("Esa pieza ya está en el caldero"), true);
            return;
        }
        if (!p.getAbilities().instabuild) stack.shrink(1);
        mask |= bit;
        data.setMask(pos, mask);

        sl.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.5f, 0.8f);
        sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.8);
        int count = Integer.bitCount(mask);
        p.displayClientMessage(Component.literal("Piezas en el caldero: " + count + "/3"), true);

        if (mask == 7) {
            data.remove(pos);
            Ritual r = new Ritual();
            r.level = sl;
            r.pos = pos.immutable();
            r.owner = p.getUUID();
            ACTIVE.add(r);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) e.level;
        Iterator<Ritual> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Ritual r = it.next();
            if (r.level != sl) continue;
            if (tickRitual(r)) it.remove();
        }
    }

    private static void bolt(ServerLevel l, double x, double y, double z) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(l);
        if (b == null) return;
        b.moveTo(x, y, z);
        b.setVisualOnly(true);
        l.addFreshEntity(b);
    }

    /** Devuelve true cuando el ritual ha terminado. */
    private static boolean tickRitual(Ritual r) {
        ServerLevel l = r.level;
        Vec3 c = new Vec3(r.pos.getX() + 0.5, r.pos.getY() + 1.0, r.pos.getZ() + 0.5);
        int t = r.t++;

        if (t == 0) {
            l.playSound(null, r.pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0f, 0.6f);
            ServerPlayer owner = l.getServer().getPlayerList().getPlayer(r.owner);
            if (owner != null) owner.displayClientMessage(Component.literal("Algo despierta en el caldero..."), true);
        }

        if (t < 100) {
            double prog = t / 100.0;
            for (int i = 0; i < 3; i++) {
                double a = t * 0.3 + i * (Math.PI * 2 / 3);
                double rad = 1.7 - prog * 1.0;
                double y = c.y + prog * 3.2;
                l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * rad, y, c.z + Math.sin(a) * rad, 1, 0, 0, 0, 0);
                l.sendParticles(ParticleTypes.END_ROD, c.x + Math.cos(a + Math.PI) * rad, y * 0.5 + c.y * 0.5, c.z + Math.sin(a + Math.PI) * rad, 1, 0, 0, 0, 0);
            }
            if (t % 3 == 0) l.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1.0, c.z, 8, 1.2, 1.2, 1.2, 0.5);
            if (t % 20 == 0) {
                l.playSound(null, r.pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0f, 0.6f + (float) prog * 1.3f);
            }
        }

        if (t == 60 || t == 85) {
            for (int k = 0; k < 28; k++) {
                double a = k * Math.PI * 2 / 28;
                l.sendParticles(ParticleTypes.CLOUD, c.x + Math.cos(a) * 2.4, c.y - 0.8, c.z + Math.sin(a) * 2.4, 1, 0, 0.05, 0, 0.1);
            }
            l.playSound(null, r.pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 3.0f, 0.7f);
        }
        if (t == 70) l.playSound(null, r.pos, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.BLOCKS, 1.5f, 0.6f);

        if (t == 100 || t == 107 || t == 114) {
            double ox = (l.random.nextDouble() - 0.5) * 5.0;
            double oz = (l.random.nextDouble() - 0.5) * 5.0;
            bolt(l, c.x + ox, r.pos.getY(), c.z + oz);
        }

        if (t == 125) {
            bolt(l, c.x, r.pos.getY(), c.z);
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
            l.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 1.5, c.z, 200, 1.8, 2.5, 1.8, 0.15);
            l.sendParticles(ParticleTypes.NOTE, c.x, c.y + 2.5, c.z, 20, 1.5, 1.5, 1.5, 1.0);
            l.playSound(null, r.pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 3.0f, 0.8f);
            l.playSound(null, r.pos, ModSounds.HONK.get(), SoundSource.HOSTILE, 3.0f, 0.9f);

            LordObesityEntity boss = ModEntities.LORD_OBESITY.get().create(l);
            if (boss != null) {
                boss.moveTo(c.x, c.y, c.z, l.random.nextFloat() * 360.0f, 0.0f);
                boss.initStage(LordObesityEntity.MERCHANT, r.owner);
                l.addFreshEntity(boss);
            }
            return true;
        }
        return false;
    }
}
