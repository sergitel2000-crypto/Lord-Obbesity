package com.lordobesity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Arranca la música cuando hay un Lord Obesity cerca y silencia la música normal del juego. */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID, value = Dist.CLIENT)
public class BossMusicClient {
    private static BossMusicInstance current;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            current = null;
            return;
        }

        if (current != null && mc.getSoundManager().isActive(current)) {
            mc.getMusicManager().stopPlaying();      // que no se pisen con la música de Minecraft
            return;
        }
        current = null;

        List<LordObesityEntity> bosses = mc.level.getEntitiesOfClass(
                LordObesityEntity.class,
                mc.player.getBoundingBox().inflate(BossMusicInstance.startDistance()),
                LordObesityEntity::isBossFight);
        if (!bosses.isEmpty()) {
            current = new BossMusicInstance(bosses.get(0));
            mc.getSoundManager().play(current);
            mc.getMusicManager().stopPlaying();
        }
    }
}
