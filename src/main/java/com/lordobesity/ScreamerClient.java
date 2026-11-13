package com.lordobesity;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Screamer: destello blanco, imagen a pantalla completa temblando y camara sacudida. */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID, value = Dist.CLIENT)
public class ScreamerClient {
    private static final ResourceLocation TEX = new ResourceLocation(LordObesityMod.MODID, "textures/gui/screamer.png");
    private static final int TOTAL = 46;
    private static int ticks = 0;

    /** Solo lo ve el jugador al que le han dado el susto (el que esta cerca de la aparicion). */
    public static boolean trigger(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        if (mc.player.distanceToSqr(x, y, z) > 14.0 * 14.0) return false;
        ticks = TOTAL;
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && ticks > 0) ticks--;
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post e) {
        if (ticks <= 0) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = e.getWindow().getGuiScaledWidth();
        int h = e.getWindow().getGuiScaledHeight();
        float age = TOTAL - ticks;
        if (age < 2) {
            g.fill(0, 0, w, h, 0xFFFFFFFF);
            return;
        }
        float alpha = age < 28 ? 1.0f : Math.max(0.0f, 1.0f - (age - 28) / 18.0f);
        int size = (int) (Math.max(w, h) * (1.12f + age * 0.004f));
        int ox = (int) ((Math.random() - 0.5) * 16);
        int oy = (int) ((Math.random() - 0.5) * 16);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        g.blit(TEX, (w - size) / 2 + ox, (h - size) / 2 + oy, size, size, 0.0f, 0.0f, 512, 512, 512, 512);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (ticks <= 0) return;
        float k = ticks / (float) TOTAL;
        e.setRoll(e.getRoll() + (float) (Math.sin(ticks * 2.3) * 7.0 * k));
        e.setPitch(e.getPitch() + (float) ((Math.random() - 0.5) * 4.0 * k));
        e.setYaw(e.getYaw() + (float) ((Math.random() - 0.5) * 4.0 * k));
    }
}
