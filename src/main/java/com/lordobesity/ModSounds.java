package com.lordobesity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LordObesityMod.MODID);

    public static final RegistryObject<SoundEvent> BOSS_MUSIC =
            SOUNDS.register("boss_music", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(LordObesityMod.MODID, "boss_music")));
}
