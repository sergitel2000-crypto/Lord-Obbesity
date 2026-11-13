package com.lordobesity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Modelo de armadura del sombrero: copa + ala ancha (textura 64x64 logica). */
public class LordHatModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(LordObesityMod.MODID, "lord_hat"), "main");

    public LordHatModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.0f), 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0f, -13.0f, -4.0f, 8.0f, 6.0f, 8.0f, new CubeDeformation(0.3f))
                        .texOffs(0, 16).addBox(-7.0f, -8.0f, -7.0f, 14.0f, 1.0f, 14.0f, new CubeDeformation(0.0f)),
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);   // sin capa extra
        return LayerDefinition.create(mesh, 64, 64);
    }
}
