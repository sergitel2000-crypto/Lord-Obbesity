package com.lordobesity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Modelo 3D de Lord Obesity. Sirve para el jefe y para el jugador transformado. */
public class LordObesityModel<T extends LivingEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(LordObesityMod.MODID, "lord_obesity"), "main");

    private final ModelPart root, body, head, rightArm, leftArm, rightLeg, leftLeg;

    public LordObesityModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-14, -22, -9, 28, 22, 18)
                        .texOffs(0, 40).addBox(-11, -14, -15, 22, 16, 6),
                PartPose.offset(0, 12, 0));

        PartDefinition h = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 62).addBox(-7, -14, -7, 14, 14, 14)
                        .texOffs(56, 84).addBox(-6, -3, -8, 12, 5, 3),
                PartPose.offset(0, -22, 0));
        h.addOrReplaceChild("brim", CubeListBuilder.create()
                .texOffs(0, 92).addBox(-11, -15, -11, 22, 2, 22), PartPose.ZERO);
        h.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(56, 62).addBox(-7, -23, -7, 14, 8, 14), PartPose.ZERO);

        body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(92, 0).addBox(-4, -2, -4, 8, 22, 8), PartPose.offset(-18, -18, 0));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(92, 0).addBox(-4, -2, -4, 8, 22, 8), PartPose.offset(18, -18, 0));

        r.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(88, 90).addBox(-4, 0, -4, 8, 12, 8), PartPose.offset(-7, 12, 0));
        r.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(88, 90).addBox(-4, 0, -4, 8, 12, 8), PartPose.offset(7, 12, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static float sm(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        int state = 0;
        float t = 0;
        if (e instanceof LordObesityEntity lo) {
            state = lo.getState();
            t = Math.max(0f, ageInTicks - lo.animStart);
        }
        float breathe = Mth.sin(ageInTicks * 0.1f);
        float swing = Mth.cos(limbSwing * 0.5f) * limbSwingAmount;

        // ----- pose base (idle / caminar) -----
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        rightLeg.xRot = swing * 1.1f;
        leftLeg.xRot = -swing * 1.1f;
        rightArm.xRot = -swing * 0.8f;
        leftArm.xRot = swing * 0.8f;
        rightArm.zRot = 0.08f + breathe * 0.02f;
        leftArm.zRot = -0.08f - breathe * 0.02f;
        body.xRot = 0f;
        body.xScale = 1.0f + breathe * 0.02f;
        body.yScale = 1.0f;
        body.zScale = 1.0f + breathe * 0.03f;

        switch (state) {
            case LordObesityEntity.SLAM -> {
                // brazos arriba -> golpe contra el suelo -> recuperación
                if (t < 25) {
                    float p = sm(t / 25f);
                    body.xRot = -0.3f * p;
                    rightArm.xRot = leftArm.xRot = -3.0f * p;
                    rightArm.zRot = 0.25f * p;
                    leftArm.zRot = -0.25f * p;
                    head.xRot -= 0.3f * p;
                } else if (t < 32) {
                    float q = sm((t - 25) / 7f);
                    body.xRot = Mth.lerp(q, -0.3f, 0.55f);
                    rightArm.xRot = leftArm.xRot = Mth.lerp(q, -3.0f, -0.3f);
                    head.xRot += 0.4f * q;
                } else {
                    float q = sm((t - 32) / 8f);
                    body.xRot = Mth.lerp(q, 0.55f, 0f);
                    rightArm.xRot = leftArm.xRot = Mth.lerp(q, -0.3f, 0f);
                    head.xRot += 0.4f * (1f - q);
                }
            }
            case LordObesityEntity.CHARGE -> {
                if (t < 20) {
                    float p = sm(t / 20f);
                    body.xRot = 0.35f * p;
                    rightArm.xRot = leftArm.xRot = 0.6f * p;
                    rightLeg.xRot = Mth.sin(t * 1.5f) * 0.8f * p; // rasca el suelo
                    leftLeg.xRot = 0f;
                    head.xRot += 0.4f * p;
                } else {
                    body.xRot = 0.6f;
                    rightArm.xRot = leftArm.xRot = 1.3f;
                    rightArm.zRot = 0.2f;
                    leftArm.zRot = -0.2f;
                    rightLeg.xRot = Mth.cos(ageInTicks * 1.1f) * 1.1f;
                    leftLeg.xRot = -rightLeg.xRot;
                    head.xRot += 0.5f;
                }
            }
            case LordObesityEntity.BURP -> {
                if (t < 30) {
                    float p = sm(t / 30f);
                    body.xRot = -0.35f * p;
                    head.xRot += -0.7f * p;
                    body.xScale = 1f + 0.12f * p;
                    body.yScale = 1f + 0.05f * p;
                    body.zScale = 1f + 0.18f * p;
                    rightArm.xRot = leftArm.xRot = 0f;
                    rightArm.zRot = 0.9f * p;
                    leftArm.zRot = -0.9f * p;
                } else if (t < 40) {
                    float q = sm((t - 30) / 10f);
                    body.xRot = Mth.lerp(q, -0.35f, 0.2f);
                    head.xRot += Mth.lerp(q, -0.7f, 0.5f);
                    body.xScale = Mth.lerp(q, 1.12f, 1f);
                    body.yScale = Mth.lerp(q, 1.05f, 1f);
                    body.zScale = Mth.lerp(q, 1.18f, 1f);
                    rightArm.zRot = Mth.lerp(q, 0.9f, 0.1f);
                    leftArm.zRot = -rightArm.zRot;
                } else {
                    float q = sm((t - 40) / 10f);
                    body.xRot = Mth.lerp(q, 0.2f, 0f);
                    head.xRot += Mth.lerp(q, 0.5f, 0f);
                }
            }
            case LordObesityEntity.JUMP -> {
                if (t < 15) {
                    float p = sm(t / 15f);
                    body.xRot = 0.45f * p;
                    rightArm.xRot = leftArm.xRot = 0.9f * p;
                    head.xRot += 0.3f * p;
                } else {
                    body.xRot = -0.2f;
                    rightArm.xRot = leftArm.xRot = -2.8f;
                    rightArm.zRot = 0.3f;
                    leftArm.zRot = -0.3f;
                }
            }
            case LordObesityEntity.BEAM -> {
                if (t < 20) {
                    float p = sm(t / 20f);
                    head.xRot += -0.5f * p;
                    body.xRot = -0.25f * p;
                    rightArm.zRot = 1.2f * p;
                    leftArm.zRot = -1.2f * p;
                    rightArm.xRot = leftArm.xRot = 0f;
                } else {
                    head.xRot += -0.3f + 0.05f * Mth.sin(ageInTicks * 0.8f);
                    body.xRot = -0.25f;
                    rightArm.xRot = leftArm.xRot = 0f;
                    rightArm.zRot = 1.2f;
                    leftArm.zRot = -1.2f;
                    body.xScale = 1.1f + 0.03f * Mth.sin(ageInTicks * 1.5f);
                    body.zScale = 1.15f + 0.03f * Mth.sin(ageInTicks * 1.5f);
                }
            }
            case LordObesityEntity.SUCK -> {
                float p = sm(Math.min(t, 15f) / 15f);
                if (t < 58) {
                    rightArm.xRot = leftArm.xRot = -1.5f * p;
                    rightArm.zRot = 0.3f * p;
                    leftArm.zRot = -0.3f * p;
                    head.xRot += -0.4f * p;
                    body.xRot = -0.15f * p;
                    float shiver = t >= 15 ? 0.015f * Mth.sin(ageInTicks * 2.0f) : 0f;
                    body.xScale = 1f + 0.22f * p + shiver;
                    body.yScale = 1f + 0.06f * p;
                    body.zScale = 1f + 0.26f * p + shiver;
                } else {
                    float q = sm((t - 58) / 12f);
                    rightArm.xRot = leftArm.xRot = Mth.lerp(q, -1.5f, 0f);
                    head.xRot += Mth.lerp(q, 0.3f, 0f);
                    body.xRot = Mth.lerp(q, 0.2f, 0f);
                    body.xScale = Mth.lerp(q, 0.9f, 1f);
                    body.zScale = Mth.lerp(q, 0.9f, 1f);
                }
            }
            case LordObesityEntity.SUMMON -> {
                float p = sm(Math.min(t, 15f) / 15f);
                float shake = Mth.sin(ageInTicks * 1.4f) * 0.2f;
                rightArm.xRot = leftArm.xRot = -2.6f * p + shake * p;
                rightArm.zRot = 0.4f * p;
                leftArm.zRot = -0.4f * p;
                head.xRot += -0.4f * p;
                body.xRot = -0.2f * p;
            }
            case LordObesityEntity.ROAR -> {
                float p = sm(Math.min(t, 10f) / 10f);
                rightArm.xRot = leftArm.xRot = -2.4f * p;
                rightArm.zRot = 0.5f * p;
                leftArm.zRot = -0.5f * p;
                head.xRot += -0.6f * p;
                body.xRot = -0.3f * p;
                body.xScale = 1f + 0.1f * p + 0.03f * Mth.sin(ageInTicks * 1.8f);
                body.zScale = 1f + 0.1f * p + 0.03f * Mth.sin(ageInTicks * 1.8f);
            }
            case LordObesityEntity.EMERGE -> {
                float p = sm(t / 70f);
                rightArm.xRot = leftArm.xRot = -2.2f * (1f - p);
                head.xRot += -0.3f * (1f - p);
            }
            case LordObesityEntity.TALK -> {
                rightArm.xRot = -0.7f + Mth.sin(ageInTicks * 0.45f) * 0.5f;
                leftArm.xRot = -0.7f + Mth.sin(ageInTicks * 0.45f + 2.0f) * 0.5f;
                rightArm.zRot = 0.35f;
                leftArm.zRot = -0.35f;
                head.xRot += Mth.sin(ageInTicks * 0.5f) * 0.08f;
            }
            default -> { }
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay,
                               float r, float g, float b, float a) {
        root.render(pose, vc, light, overlay, r, g, b, a);
    }
}
