package com.lordobesity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LordObesityMod.MODID);

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(LordObesityMod.MODID, name)));
    }

    public static final RegistryObject<SoundEvent> BOSS_MUSIC = reg("boss_music");
    public static final RegistryObject<SoundEvent> AMBIENT = reg("ambient");
    public static final RegistryObject<SoundEvent> HURT = reg("hurt");
    public static final RegistryObject<SoundEvent> DEATH = reg("death");
    public static final RegistryObject<SoundEvent> ROAR = reg("roar");
    public static final RegistryObject<SoundEvent> BURP = reg("burp");
    public static final RegistryObject<SoundEvent> SLAM = reg("slam");
    public static final RegistryObject<SoundEvent> BOING = reg("boing");
    public static final RegistryObject<SoundEvent> SCARE = reg("scare");
    public static final RegistryObject<SoundEvent> HONK = reg("honk");
    public static final RegistryObject<SoundEvent> WHISTLE = reg("whistle");
}
