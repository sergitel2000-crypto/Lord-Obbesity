package com.lordobesity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Si llevas la máscara puesta, tu cuerpo se dibuja como Lord Obesity. */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID, value = Dist.CLIENT)
public class PlayerTransformClient {
    private static final float SCALE = 0.5f * MaskEvents.SIZE; // ajustado a la hitbox mas grande

    private static LordObesityModel<Player> model;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        if (!MaskEvents.wearsMask(p)) return;
        e.setCanceled(true);

        if (model == null) {
            model = new LordObesityModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(LordObesityModel.LAYER));
        }
        float pt = e.getPartialTick();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();

        float bodyYaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
        float headYaw = Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot);
        float pitch = Mth.lerp(pt, p.xRotO, p.getXRot());
        float limbAmt = Math.min(p.walkAnimation.speed(pt), 1.0f);
        float limbSwing = p.walkAnimation.position(pt);

        ps.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        ps.scale(-1.0f, -1.0f, 1.0f);
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(0.0f, -1.501f, 0.0f);

        model.setupAnim(p, limbSwing, limbAmt, p.tickCount + pt, headYaw - bodyYaw, pitch);
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(
                RenderType.entityCutoutNoCull(LordObesityRenderer.TEXTURE));
        model.renderToBuffer(ps, vc, e.getPackedLight(), OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        ps.popPose();
    }
}
