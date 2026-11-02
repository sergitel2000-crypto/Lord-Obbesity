package com.lordobesity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Música de jefe: en bucle, con fundido de entrada y de salida. */
public class BossMusicInstance extends AbstractTickableSoundInstance {
    private static final double START_DIST = 40.0;
    private static final double STOP_DIST = 55.0;

    private final LordObesityEntity boss;
    private boolean fadingOut = false;

    public BossMusicInstance(LordObesityEntity boss) {
        super(ModSounds.BOSS_MUSIC.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.boss = boss;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.relative = true;                        // suena igual en todo momento, sin posición
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    public static double startDistance() {
        return START_DIST;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean bossGone = boss == null || !boss.isAlive() || boss.isRemoved();
        boolean tooFar = mc.player == null || mc.player.distanceTo(boss) > STOP_DIST;
        if (bossGone || tooFar) fadingOut = true;

        if (fadingOut) {
            this.volume -= 0.02f;                    // ~2,5 s de fundido
            if (this.volume <= 0.0f) {
                this.volume = 0.0f;
                this.stop();
            }
        } else if (this.volume < 1.0f) {
            this.volume = Math.min(1.0f, this.volume + 0.02f);
        }
    }
}
