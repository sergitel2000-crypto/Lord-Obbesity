package com.lordobesity;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LordObesityMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(LordObesityModel.LAYER, LordObesityModel::createBodyLayer);
        e.registerLayerDefinition(LordHatModel.LAYER, LordHatModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.LORD_OBESITY.get(), LordObesityRenderer::new);
    }

    private static int lerp(int a, int b, float f) {
        return Math.round(a + (b - a) * f);
    }

    /** El agua del caldero ritual pasa del color del bioma al amarillo segun TINT (0-9). */
    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, tintIndex) -> {
            int base = (level != null && pos != null) ? BiomeColors.getAverageWaterColor(level, pos) : 0x3F76E4;
            float f = state.getValue(RitualCauldronBlock.TINT) / 9.0f;
            int r = lerp((base >> 16) & 255, 0xF6, f);
            int g = lerp((base >> 8) & 255, 0xC8, f);
            int b = lerp(base & 255, 0x28, f);
            return (r << 16) | (g << 8) | b;
        }, ModBlocks.RITUAL_CAULDRON.get());
    }
}
