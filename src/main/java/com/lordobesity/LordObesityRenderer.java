package com.lordobesity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class LordObesityRenderer extends MobRenderer<LordObesityEntity, LordObesityModel<LordObesityEntity>> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(LordObesityMod.MODID, "textures/entity/lord_obesity.png");
    public static final ResourceLocation TEXTURE_MEGA =
            new ResourceLocation(LordObesityMod.MODID, "textures/entity/lord_obesity_mega.png");

    public LordObesityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LordObesityModel<>(ctx.bakeLayer(LordObesityModel.LAYER)), 1.8f);
    }

    @Override
    public void render(LordObesityEntity entity, float yaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light) {
        this.shadowRadius = 1.8f * entity.getModelScale();
        ps.pushPose();
        float depth = entity.getBuryDepth(partialTick);
        if (depth > 0.0f) ps.translate(0.0, -depth, 0.0);   // emerge / se hunde bajo tierra
        super.render(entity, yaw, partialTick, ps, buffer, light);
        ps.popPose();
    }

    @Override
    protected void scale(LordObesityEntity entity, PoseStack ps, float partialTick) {
        float s = entity.getModelScale();
        ps.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(LordObesityEntity entity) {
        return entity.isMega() ? TEXTURE_MEGA : TEXTURE;
    }
}
