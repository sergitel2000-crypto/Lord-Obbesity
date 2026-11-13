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

/**
 * Modelo 3D articulado de Lord Obesity (textura 128x128, mismas zonas que la textura original):
 * caderas -> pecho -> cabeza / (hombro -> codo -> muneca), caderas -> (muslo -> rodilla -> pie), barriga con rebote propio.
 */
public class LordObesityModel<T extends LivingEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(LordObesityMod.MODID, "lord_obesity"), "main");

    private final ModelPart root, hips, body, belly, head, crown, brim;
    private final ModelPart rightArm, rightForearm, rightHand, leftArm, leftForearm, leftHand;
    private final ModelPart rightLeg, rightShin, rightFoot, leftLeg, leftShin, leftFoot;
    private final ModelPart[] all;

    public LordObesityModel(ModelPart root) {
        this.root = root;
        this.hips = root.getChild("hips");
        this.belly = hips.getChild("belly");
        this.body = hips.getChild("chest");
        this.head = body.getChild("head");
        this.crown = head.getChild("crown");
        this.brim = head.getChild("brim");
        this.rightArm = body.getChild("right_arm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.rightHand = rightForearm.getChild("right_hand");
        this.leftArm = body.getChild("left_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.leftHand = leftForearm.getChild("left_hand");
        this.rightLeg = hips.getChild("right_leg");
        this.rightShin = rightLeg.getChild("right_shin");
        this.rightFoot = rightShin.getChild("right_foot");
        this.leftLeg = hips.getChild("left_leg");
        this.leftShin = leftLeg.getChild("left_shin");
        this.leftFoot = leftShin.getChild("left_foot");
        this.all = new ModelPart[]{hips, belly, body, head, crown, brim, rightArm, rightForearm, rightHand,
                leftArm, leftForearm, leftHand, rightLeg, rightShin, rightFoot, leftLeg, leftShin, leftFoot};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // Caderas / abdomen (mitad inferior del torso original)
        PartDefinition hips = r.addOrReplaceChild("hips", CubeListBuilder.create()
                .texOffs(0, 11).addBox(-14, -11, -9, 28, 11, 18), PartPose.offset(0, 12, 0));
        // Barriga: parte propia para que rebote
        hips.addOrReplaceChild("belly", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-11, -8, -3, 22, 16, 6), PartPose.offset(0, -6, -12));
        // Pecho (mitad superior del torso original)
        PartDefinition chest = hips.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-14, -11, -9, 28, 11, 18), PartPose.offset(0, -11, 0));

        PartDefinition h = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 62).addBox(-7, -14, -7, 14, 14, 14), PartPose.offset(0, -11, 0));
        h.addOrReplaceChild("brim", CubeListBuilder.create()
                .texOffs(0, 92).addBox(-11, -15, -11, 22, 2, 22), PartPose.ZERO);
        h.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(56, 62).addBox(-7, -23, -7, 14, 8, 14), PartPose.ZERO);

        // Brazos: hombro (10) -> codo (8) -> muneca/mano (4) = 22 como el brazo original
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "right" : "left";
            PartDefinition arm = chest.addOrReplaceChild(n + "_arm", CubeListBuilder.create()
                    .texOffs(92, 0).addBox(-4, -2, -4, 8, 10, 8), PartPose.offset(side * 18, -7, 0));
            PartDefinition fore = arm.addOrReplaceChild(n + "_forearm", CubeListBuilder.create()
                    .texOffs(92, 10).addBox(-4, 0, -4, 8, 8, 8), PartPose.offset(0, 8, 0));
            fore.addOrReplaceChild(n + "_hand", CubeListBuilder.create()
                    .texOffs(92, 18).addBox(-4, 0, -4, 8, 4, 8), PartPose.offset(0, 8, 0));
        }
        // Piernas: muslo (5) -> rodilla (4) -> pie (3) = 12 como la pierna original
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "right" : "left";
            PartDefinition thigh = hips.addOrReplaceChild(n + "_leg", CubeListBuilder.create()
                    .texOffs(88, 90).addBox(-4, 0, -4, 8, 5, 8), PartPose.offset(side * 7, 0, 0));
            PartDefinition shin = thigh.addOrReplaceChild(n + "_shin", CubeListBuilder.create()
                    .texOffs(88, 95).addBox(-4, 0, -4, 8, 4, 8), PartPose.offset(0, 5, 0));
            shin.addOrReplaceChild(n + "_foot", CubeListBuilder.create()
                    .texOffs(88, 99).addBox(-4, 0, -4, 8, 3, 8), PartPose.offset(0, 4, 0));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static float sm(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static void reset(ModelPart p) {
        p.xRot = 0f;
        p.yRot = 0f;
        p.zRot = 0f;
        p.xScale = 1f;
        p.yScale = 1f;
        p.zScale = 1f;
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
        for (ModelPart p : all) reset(p);          // el modelo se comparte entre entidades
        hips.x = 0f;
        hips.z = 0f;

        float breathe = Mth.sin(ageInTicks * 0.1f);
        float phase = limbSwing * 0.5f;
        float amp = Mth.clamp(limbSwingAmount * 1.5f, 0.0f, 1.0f);
        float sP = Mth.sin(phase), cP = Mth.cos(phase);
        float idle = Mth.sin(ageInTicks * 0.05f);

        // ----- caderas y columna: rebote, balanceo y torsion al andar
        hips.y = 12.0f + amp * 1.2f * Math.abs(sP) + breathe * 0.12f;
        hips.zRot = sP * 0.07f * amp + Mth.sin(ageInTicks * 0.045f) * 0.02f;
        hips.yRot = cP * 0.10f * amp;
        body.yRot = -hips.yRot * 1.5f + netHeadYaw * Mth.DEG_TO_RAD * 0.25f;
        body.zRot = -hips.zRot * 0.8f;
        body.xRot = 0.05f * amp;
        body.xScale = 1.0f + breathe * 0.02f;
        body.yScale = 1.0f + breathe * 0.012f;
        body.zScale = 1.0f + breathe * 0.03f;

        // ----- cuello / cabeza / sombrero
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.75f;
        head.xRot = headPitch * Mth.DEG_TO_RAD - body.xRot;
        head.zRot = -body.zRot * 0.5f + Mth.sin(ageInTicks * 0.05f + 1.0f) * 0.025f;
        crown.xRot = -head.xRot * 0.12f + sP * 0.05f * amp;
        brim.zRot = Mth.sin(ageInTicks * 0.07f) * 0.012f;

        // ----- piernas: muslo, rodilla (se dobla al adelantar la pierna) y tobillo
        rightLeg.xRot = cP * 0.95f * amp;
        leftLeg.xRot = -cP * 0.95f * amp;
        rightShin.xRot = 0.06f + 1.15f * Math.max(0f, sP) * amp;
        leftShin.xRot = 0.06f + 1.15f * Math.max(0f, -sP) * amp;
        rightFoot.xRot = -(rightLeg.xRot + rightShin.xRot) * 0.55f;
        leftFoot.xRot = -(leftLeg.xRot + leftShin.xRot) * 0.55f;

        // ----- brazos: hombro opuesto a la pierna, codo que se flexiona, muneca con retardo
        rightArm.xRot = -cP * 0.7f * amp + Mth.sin(ageInTicks * 0.09f) * 0.03f;
        leftArm.xRot = cP * 0.7f * amp - Mth.sin(ageInTicks * 0.09f) * 0.03f;
        rightArm.zRot = 0.10f + breathe * 0.02f + sP * 0.04f * amp;
        leftArm.zRot = -0.10f - breathe * 0.02f + sP * 0.04f * amp;
        rightForearm.xRot = -(0.20f + 0.55f * Math.max(0f, cP) * amp);
        leftForearm.xRot = -(0.20f + 0.55f * Math.max(0f, -cP) * amp);
        rightHand.xRot = -rightForearm.xRot * 0.35f + Mth.sin(ageInTicks * 0.12f) * 0.04f;
        leftHand.xRot = -leftForearm.xRot * 0.35f + Mth.sin(ageInTicks * 0.12f + 1.7f) * 0.04f;
        rightHand.zRot = idle * 0.03f;
        leftHand.zRot = -idle * 0.03f;

        // ----- barriga: rebota con cada paso y respira
        belly.xRot = Mth.sin(phase * 2.0f - 0.9f) * 0.08f * amp + breathe * 0.03f;
        belly.yScale = 1.0f + 0.05f * Mth.sin(phase * 2.0f) * amp + breathe * 0.015f;

        boolean shinSet = false;
        boolean armsSet = false;

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
                shinSet = true;
                if (t < 15) {
                    float p = sm(t / 15f);
                    body.xRot = 0.45f * p;
                    rightArm.xRot = leftArm.xRot = 0.9f * p;
                    head.xRot += 0.3f * p;
                    rightLeg.xRot = leftLeg.xRot = -0.8f * p;          // se agacha
                    rightShin.xRot = leftShin.xRot = 1.3f * p;
                    rightFoot.xRot = leftFoot.xRot = -0.5f * p;
                    hips.y += 3.0f * p;
                } else {
                    body.xRot = -0.2f;
                    rightArm.xRot = leftArm.xRot = -2.8f;
                    rightArm.zRot = 0.3f;
                    leftArm.zRot = -0.3f;
                    rightLeg.xRot = leftLeg.xRot = -0.5f;              // piernas encogidas en el aire
                    rightShin.xRot = leftShin.xRot = 0.9f;
                    rightFoot.xRot = leftFoot.xRot = -0.3f;
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
                float p = sm(t / (float) LordObesityEntity.EMERGE_TICKS);
                rightArm.xRot = leftArm.xRot = -2.2f * (1f - p);
                head.xRot += -0.3f * (1f - p);
            }
            case LordObesityEntity.SCREAM -> {
                float sh = Mth.sin(ageInTicks * 3.0f) * 0.12f;
                rightArm.xRot = leftArm.xRot = -2.6f + sh;
                rightArm.zRot = 0.6f;
                leftArm.zRot = -0.6f;
                head.xRot += -0.7f + sh * 0.5f;
                body.xRot = -0.25f;
                body.xScale = 1.08f + sh * 0.2f;
            }
            case LordObesityEntity.DANCE -> {
                // "griddy": rodillas flexionadas, pasos laterales rapidos, brazos abiertos alternando
                shinSet = true;
                armsSet = true;
                float b = ageInTicks * 0.62f;
                float s1 = Mth.sin(b), c1 = Mth.cos(b);
                float fade = sm(Math.min(t, 8f) / 8f) * (1f - sm((t - 78f) / 12f));
                hips.y += 3.0f * fade + Math.abs(s1) * 1.5f * fade;
                hips.zRot = s1 * 0.12f * fade;
                hips.yRot = c1 * 0.20f * fade;
                body.zRot = -s1 * 0.10f * fade;
                body.xRot = 0.28f * fade;
                head.yRot += -c1 * 0.30f * fade;
                head.zRot += s1 * 0.08f * fade;
                crown.xRot += s1 * 0.10f * fade;
                rightLeg.xRot = (-0.55f + s1 * 0.35f) * fade;
                leftLeg.xRot = (-0.55f - s1 * 0.35f) * fade;
                rightLeg.zRot = (0.22f + Math.max(0f, s1) * 0.25f) * fade;
                leftLeg.zRot = (-0.22f - Math.max(0f, -s1) * 0.25f) * fade;
                rightShin.xRot = (1.1f + c1 * 0.3f) * fade;
                leftShin.xRot = (1.1f - c1 * 0.3f) * fade;
                rightFoot.xRot = -0.45f * fade;
                leftFoot.xRot = -0.45f * fade;
                rightArm.zRot = (1.05f + c1 * 0.15f) * fade;
                leftArm.zRot = (-1.05f - c1 * 0.15f) * fade;
                rightArm.xRot = (-0.3f + c1 * 0.95f) * fade;
                leftArm.xRot = (-0.3f - c1 * 0.95f) * fade;
                rightForearm.xRot = (-0.8f - s1 * 0.55f) * fade;
                leftForearm.xRot = (-0.8f + s1 * 0.55f) * fade;
                rightHand.xRot = s1 * 0.4f * fade;
                leftHand.xRot = -s1 * 0.4f * fade;
                belly.xRot += s1 * 0.16f * fade;
                belly.yScale += 0.08f * s1 * fade;
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


        // ----- articulaciones derivadas de las poses de ataque
        if (state != LordObesityEntity.IDLE) {
            if (!armsSet) {
                rightForearm.xRot = -0.25f - 0.15f * Math.abs(rightArm.xRot);
                leftForearm.xRot = -0.25f - 0.15f * Math.abs(leftArm.xRot);
                rightHand.xRot = -rightForearm.xRot * 0.3f;
                leftHand.xRot = -leftForearm.xRot * 0.3f;
            }
            if (!shinSet) {
                rightShin.xRot = Mth.clamp(-rightLeg.xRot * 1.2f, 0f, 1.6f) + 0.05f;
                leftShin.xRot = Mth.clamp(-leftLeg.xRot * 1.2f, 0f, 1.6f) + 0.05f;
                rightFoot.xRot = -(rightLeg.xRot + rightShin.xRot) * 0.55f;
                leftFoot.xRot = -(leftLeg.xRot + leftShin.xRot) * 0.55f;
            }
        }

        // ---- mini Lord Obesity: cuerpos deformes, encorvados y con tics
        if (e instanceof LordObesityEntity lo && lo.getStage() == LordObesityEntity.MINI) {
            float k = (lo.getId() % 7) / 6.0f;                    // cada mini sale distinto
            float tw = Mth.sin(ageInTicks * 7.0f + lo.getId()) * 0.12f;
            head.xScale = 1.5f + 0.4f * k;
            head.yScale = 1.3f + 0.3f * k;
            head.zScale = 1.4f;
            head.zRot = (k - 0.5f) * 0.9f + tw;                   // cabeza ladeada
            rightArm.yScale = 1.9f + 0.4f * k;                    // un brazo larguisimo y fino
            rightArm.xScale = 0.65f;
            leftArm.yScale = 0.55f;                               // otro corto y gordo
            leftArm.xScale = 1.4f;
            rightLeg.yScale = 0.75f;
            leftLeg.yScale = 1.2f;
            body.xScale *= 0.75f;
            body.xRot = Math.max(body.xRot, 0.55f);               // encorvado
            rightArm.xRot += tw * 3.0f;
            leftArm.zRot += tw * 2.0f;
            head.yRot += Mth.sin(ageInTicks * 3.1f + lo.getId()) * 0.2f;
        }

        // la barriga sigue al pecho cuando se infla (eructo, succion...)
        belly.xScale = body.xScale;
        belly.zScale = body.zScale;
        belly.yScale = belly.yScale * body.yScale;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay,
                               float r, float g, float b, float a) {
        root.render(pose, vc, light, overlay, r, g, b, a);
    }
}
