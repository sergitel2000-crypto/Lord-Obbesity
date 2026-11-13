package com.lordobesity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class LoreEvents {
    @SubscribeEvent
    public static void onPickup(PlayerEvent.ItemPickupEvent e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        if (e.getStack().getItem() instanceof LoreBookItem) {
            p.displayClientMessage(Component.literal("Has encontrado un fragmento... Clic derecho para leerlo."), true);
            p.level().playSound(null, p.blockPosition(), ModSounds.WHISPER.get(), SoundSource.PLAYERS, 1.5f, 0.8f);
        }
    }
}
