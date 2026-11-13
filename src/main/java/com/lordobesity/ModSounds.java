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
    public static final RegistryObject<SoundEvent> WHISPER = reg("whisper");
    public static final RegistryObject<SoundEvent> RUMBLE = reg("rumble");
    public static final RegistryObject<SoundEvent> BOOM = reg("boom");
    public static final RegistryObject<SoundEvent> EVIL_LAUGH = reg("evil_laugh");
    public static final RegistryObject<SoundEvent> SCREAM = reg("scream");
    public static final RegistryObject<SoundEvent> STING = reg("sting");
    public static final RegistryObject<SoundEvent> BREATH = reg("breath");
    public static final RegistryObject<SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final RegistryObject<SoundEvent> SHIMMER = reg("shimmer");
    public static final RegistryObject<SoundEvent> WHOOSH = reg("whoosh");
    public static final RegistryObject<SoundEvent> INHALE = reg("inhale");
    public static final RegistryObject<SoundEvent> BEAM = reg("beam");
    public static final RegistryObject<SoundEvent> CHITTER = reg("chitter");
    public static final RegistryObject<SoundEvent> HOOK = reg("hook");
    public static final RegistryObject<SoundEvent> CRACK = reg("crack");
    public static final RegistryObject<SoundEvent> DRONE = reg("drone");
    public static final RegistryObject<SoundEvent> MINI_DEATH = reg("mini_death");
    public static final RegistryObject<SoundEvent> MINI_HURT = reg("mini_hurt");
    public static final RegistryObject<SoundEvent> FOOTSTEP = reg("footstep");
    public static final RegistryObject<SoundEvent> SCRAPE = reg("scrape");
    public static final RegistryObject<SoundEvent> MUSIC_BOX = reg("music_box");
    public static final RegistryObject<SoundEvent> CREAK = reg("creak");
    public static final RegistryObject<SoundEvent> KNOCK = reg("knock");
}
