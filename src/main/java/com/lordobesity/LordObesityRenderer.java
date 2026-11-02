package com.lordobesity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class LordObesityRenderer extends MobRenderer<LordObesityEntity, LordObesityModel<LordObesityEntity>> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(LordObesityMod.MODID, "textures/entity/lord_obesity.png");

    public LordObesityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LordObesityModel<>(ctx.bakeLayer(LordObesityModel.LAYER)), 1.8f);
    }

    @Override
    public ResourceLocation getTextureLocation(LordObesityEntity entity) {
        return TEXTURE;
    }
}
