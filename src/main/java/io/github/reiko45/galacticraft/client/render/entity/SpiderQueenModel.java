/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.model.ModelSpiderQueen). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SpiderQueenEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 1.12.2 ModelSpiderQueen: a spider's head and thorax, a great boxed abdomen, and eight three-jointed legs. The right
 * legs (2, 4, 6, 8) sway and lift in a wave as she walks; the left ones mirror them, and each joint follows the end of
 * the one before.
 */
public class SpiderQueenModel extends HierarchicalModel<SpiderQueenEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("spider_queen"), "main");
    private static final float[] RIGHT_OFFSETS = {-0.4903446F, -0.1936386F, 0.3648668F, 0.6615727F};

    private final ModelPart root;
    /** [leg 1-8][segment 0-2]. */
    private final ModelPart[][] legs = new ModelPart[8][3];

    public SpiderQueenModel(ModelPart root) {
        this.root = root;
        for (int leg = 0; leg < 8; leg++) {
            for (int segment = 0; segment < 3; segment++) {
                this.legs[leg][segment] = root.getChild("leg" + (leg + 1) + segment);
            }
        }
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 4).mirror().addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, -3.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg10", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 20.0F, 4.0F, 0.0F, 3.631943F, -0.7330383F));
        root.addOrReplaceChild("leg11", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-9.7F, 13.2F, 7.6F, 0.0F, 3.631937F, 0.3823201F));
        root.addOrReplaceChild("leg12", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-16.4F, 16.2F, 11.1F, 0.0F, 3.631937F, 1.461656F));
        root.addOrReplaceChild("leg20", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 20.0F, 4.0F, 0.0F, -0.4903446F, -0.7330383F));
        root.addOrReplaceChild("leg21", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(9.666667F, 13.2F, 7.6F, 0.0F, -0.4903503F, 0.3823201F));
        root.addOrReplaceChild("leg22", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(16.4F, 16.2F, 11.1F, 0.0F, -0.4903503F, 1.461656F));
        root.addOrReplaceChild("leg30", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 20.0F, 2.0F, 0.0F, 3.335237F, -0.7330383F));
        root.addOrReplaceChild("leg31", CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-10.7F, 13.2F, 3.6F, 0.0F, 3.335231F, 0.3823201F));
        root.addOrReplaceChild("leg32", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-17.6F, 16.2F, 4.9F, 0.0F, 3.335231F, 1.461656F));
        root.addOrReplaceChild("leg40", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 20.0F, 2.0F, 0.0F, -0.1936386F, -0.7330383F));
        root.addOrReplaceChild("leg41", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(10.66667F, 13.2F, 3.6F, 0.0F, -0.1936443F, 0.3823201F));
        root.addOrReplaceChild("leg42", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(17.6F, 16.2F, 4.9F, 0.0F, -0.1936443F, 1.461656F));
        root.addOrReplaceChild("leg50", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 20.0F, 0.5F, 0.0F, 2.7838F, -0.7330383F));
        root.addOrReplaceChild("leg51", CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-10.0F, 13.2F, -2.2F, 0.0F, 2.783794F, 0.3823201F));
        root.addOrReplaceChild("leg52", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-16.5F, 16.2F, -4.7F, 0.0F, 2.783794F, 1.461656F));
        root.addOrReplaceChild("leg60", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 20.0F, 0.5F, 0.0F, 0.3648668F, -0.7330383F));
        root.addOrReplaceChild("leg61", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(10.0F, 13.2F, -2.2F, 0.0F, 0.3648611F, 0.3823201F));
        root.addOrReplaceChild("leg62", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(16.53333F, 16.2F, -4.7F, 0.0F, 0.3648611F, 1.461656F));
        root.addOrReplaceChild("leg70", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 20.0F, -2.0F, 0.0F, 2.495821F, -0.7330383F));
        root.addOrReplaceChild("leg71", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-8.8F, 13.33333F, -6.466667F, 0.0F, 2.495821F, 0.4206553F));
        root.addOrReplaceChild("leg72", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-14.5F, 16.66667F, -10.9F, 0.0F, 2.495821F, 1.201406F));
        root.addOrReplaceChild("leg80", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 20.0F, -2.0F, 0.0F, 0.6615727F, -0.7330383F));
        root.addOrReplaceChild("leg81", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(8.8F, 13.33333F, -6.466667F, 0.0F, 0.6368846F, 0.4206553F));
        root.addOrReplaceChild("leg82", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(14.53333F, 16.66667F, -10.9F, 0.0F, 0.6615671F, 1.201406F));
        root.addOrReplaceChild("rearend", CubeListBuilder.create().texOffs(0, 41).mirror().addBox(-6.0F, -7.0F, -6.0F, 12.0F, 11.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 13.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rearback", CubeListBuilder.create().texOffs(26, 31).mirror().addBox(-5.0F, -6.0F, 6.0F, 10.0F, 9.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 13.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rearleft", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(6.0F, -6.0F, -5.0F, 1.0F, 9.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 13.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rearright", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-7.0F, -6.0F, -5.0F, 1.0F, 9.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 13.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /** 1.12.2 setRotationAngles. */
    @Override
    public void setupAnim(SpiderQueenEntity queen, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float movement = limbSwing;
        for (int i = 0; i < 4; i++) {
            final ModelPart right = this.legs[i * 2 + 1][0];
            right.yRot = Mth.cos(movement) * 0.2F + RIGHT_OFFSETS[i];
            right.zRot = -0.7330383F;
            // lifted while it swings forward (the derivative of cos is -sin)
            if (-Mth.sin(movement) * 0.2F > 0.0F) {
                right.zRot = -0.7330383F + -Mth.sin(movement) * -0.5F;
            }
            movement -= 1.0F;
        }
        for (int i = 0; i < 4; i++) {
            // copyLeg1LeftToRight: the left leg mirrors the right one
            final ModelPart right = this.legs[i * 2 + 1][0];
            final ModelPart left = this.legs[i * 2][0];
            left.xRot = right.xRot;
            left.yRot = (float) (Math.PI - right.yRot);
            left.zRot = right.zRot;
        }
        for (int leg = 0; leg < 8; leg++) {
            for (int segment = 1; segment < 3; segment++) {
                this.legs[leg][segment].yRot = this.legs[leg][0].yRot;
                this.legs[leg][segment].xRot = this.legs[leg][0].xRot;
            }
            follow(10.0F, this.legs[leg][0], this.legs[leg][1]);
            follow(8.0F, this.legs[leg][1], this.legs[leg][2]);
        }
    }

    /** 1.12.2 copyLegAngles: a joint starts where the segment before it ends. */
    private static void follow(float length, ModelPart parent, ModelPart child) {
        child.x = parent.x + length * (Mth.cos(parent.zRot) * Mth.cos(parent.yRot));
        child.y = parent.y + length * Mth.sin(parent.zRot);
        child.z = parent.z + length * (-Mth.sin(parent.yRot) * Mth.cos(parent.zRot));
    }
}
