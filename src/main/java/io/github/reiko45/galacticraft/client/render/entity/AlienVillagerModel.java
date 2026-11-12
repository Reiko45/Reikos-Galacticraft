/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.model.ModelAlienVillager on 1.12.2's
 * ModelVillager). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.AlienVillagerEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** 1.12.2 ModelAlienVillager: a villager (head, nose, robe, folded arms, legs) with a big brain on top of its head. */
public class AlienVillagerModel extends HierarchicalModel<AlienVillagerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("alien_villager"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AlienVillagerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    /** The 1.12.2 boxes, each with its tiny extra inflation (which kept the overlapping faces from flickering). */
    public static LayerDefinition createBodyLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, new CubeDeformation(0.001F)), PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2, new CubeDeformation(0.002F)), PartPose.offset(0.0F, -2.0F, 0.0F));
        // 1.12.2 drew the brain on its own, turned with the head
        head.addOrReplaceChild("brain", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4.0F, -16.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6, new CubeDeformation(0.003F))
                .texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, new CubeDeformation(0.504F)), PartPose.ZERO);
        // ModelVillager#setRotationAngles put the folded arms at (0, 3, -1) tipped 0.75 forward
        root.addOrReplaceChild("arms", CubeListBuilder.create()
                .texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4, new CubeDeformation(0.005F))
                .texOffs(44, 22).addBox(4.0F, -2.0F, -2.0F, 4, 8, 4, new CubeDeformation(0.0001F))
                .texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4, new CubeDeformation(0.0004F)),
                PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.0006F)), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 22).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.0002F)), PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(AlienVillagerEntity villager, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount * 0.5F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
    }
}
