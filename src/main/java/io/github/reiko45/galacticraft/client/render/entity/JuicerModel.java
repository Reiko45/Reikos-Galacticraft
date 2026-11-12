/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.model.ModelJuicer). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.JuicerEntity;
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
 * 1.12.2 ModelJuicer: a flat two-plated body, a small head, eight two-jointed legs (the outer segment hung off the inner
 * one) and a tail of two segments ending in a stinger, raised higher while it is attacking. The left legs step in a
 * wave; the right ones mirror them.
 */
public class JuicerModel extends HierarchicalModel<JuicerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("juicer"), "main");
    private static final String[] SIDES = {"left", "right"};
    private static final String[] LEGS = {"front", "mid_front", "mid_back", "back"};

    private final ModelPart root;
    /** [side][leg][segment], side 0 left, 1 right. */
    private final ModelPart[][][] legs = new ModelPart[2][4][2];
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart stinger;
    /** 1.12.2 legLength0: the inner segment's length. */
    private static final float LEG_LENGTH = 3.0F;

    public JuicerModel(ModelPart root) {
        this.root = root;
        for (int side = 0; side < 2; side++) {
            for (int leg = 0; leg < 4; leg++) {
                final String name = "leg_" + SIDES[side] + "_" + LEGS[leg];
                this.legs[side][leg][0] = root.getChild(name + "1");
                this.legs[side][leg][1] = this.legs[side][leg][0].getChild(name + "2");
            }
        }
        this.tail0 = root.getChild("tail0");
        this.tail1 = this.tail0.getChild("tail1");
        this.stinger = this.tail1.getChild("stinger");
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartDefinition body1Def = root.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(24, 22).mirror().addBox(-3.0F, -1.0F, -4.0F, 6.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        final PartDefinition body2Def = root.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.5F, -1.5F, -3.5F, 5.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        final PartDefinition headDef = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -4.0F, -0.3717861F, 0.0F, 0.0F));
        final PartDefinition legRightFront1Def = root.addOrReplaceChild("leg_right_front1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, 0.0F, -3.0F, 0.0F, 2.453788F, 1.115358F));
        final PartDefinition legRightFront2Def = legRightFront1Def.addOrReplaceChild("leg_right_front2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -3.0F, -1.0F, 0.0F, 0.37178600000000017F, -1.9704661F));
        final PartDefinition legLeftFront1Def = root.addOrReplaceChild("leg_left_front1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, 0.0F, -3.0F, 0.0F, 0.7807508F, -1.115358F));
        final PartDefinition legLeftFront2Def = legLeftFront1Def.addOrReplaceChild("leg_left_front2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, -3.0F, -1.0F, 0.0F, -0.07435720000000001F, 1.9704661F));
        final PartDefinition legRightMidFront1Def = root.addOrReplaceChild("leg_right_mid_front1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, 0.0F, -1.0F, 0.0F, 2.93711F, 1.115358F));
        final PartDefinition legRightMidFront2Def = legRightMidFront1Def.addOrReplaceChild("leg_right_mid_front2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -3.0F, 0.0F, 0.0F, 0.074357F, -1.9704661F));
        final PartDefinition legLeftMidFront1Def = root.addOrReplaceChild("leg_left_mid_front1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, 0.0F, -1.0F, 0.0F, 0.1858931F, -1.115358F));
        final PartDefinition legLeftMidFront2Def = legLeftMidFront1Def.addOrReplaceChild("leg_left_mid_front2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, -3.0F, 0.0F, 0.0F, 0.1487144F, 1.9704661F));
        final PartDefinition legRightMidBack1Def = root.addOrReplaceChild("leg_right_mid_back1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, 0.0F, 1.0F, 0.0F, -3.030057F, 1.115358F));
        final PartDefinition legRightMidBack2Def = legRightMidBack1Def.addOrReplaceChild("leg_right_mid_back2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -3.0F, 0.0F, 0.0F, 0.055767999999999596F, -1.9704661F));
        final PartDefinition legLeftMidBack1Def = root.addOrReplaceChild("leg_left_mid_back1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, 0.0F, 1.0F, 0.0F, -0.0371786F, -1.115358F));
        final PartDefinition legLeftMidBack2Def = legLeftMidBack1Def.addOrReplaceChild("leg_left_mid_back2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, -3.0F, 0.0F, 0.0F, -0.11153579999999999F, 1.9704661F));
        final PartDefinition legRightBack1Def = root.addOrReplaceChild("leg_right_back1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, 0.0F, 3.0F, 0.0F, -2.658271F, 1.115358F));
        final PartDefinition legRightBack2Def = legRightBack1Def.addOrReplaceChild("leg_right_back2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -3.0F, 1.0F, 0.0F, -0.13012500000000005F, -1.9704661F));
        final PartDefinition legLeftBack1Def = root.addOrReplaceChild("leg_left_back1", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, 0.0F, 3.0F, 0.0F, -0.3346075F, -1.115358F));
        final PartDefinition legLeftBack2Def = legLeftBack1Def.addOrReplaceChild("leg_left_back2", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, -3.0F, 0.0F, 0.0F, -0.18589309999999998F, 1.9704661F));
        final PartDefinition backDef = root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.5F, -0.5F, -2.5F, 3.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        final PartDefinition tail0Def = root.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(42, 17).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.5205006F, 0.0F, 0.0F));
        final PartDefinition tail1Def = tail0Def.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(42, 12).mirror().addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, 2.1389063999999998F, 0.0F, 0.0F));
        final PartDefinition stingerDef = tail1Def.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(48, 9).mirror().addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.2F, -4.133333F, -1.1722629999999998F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /** 1.12.2 setRotationAngles. */
    @Override
    public void setupAnim(JuicerEntity juicer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        final float[] offsets = {0.5F, 0.05F, -0.1F, -0.5F};
        float movement = limbSwing;
        for (int leg = 0; leg < 4; leg++) {
            final ModelPart inner = this.legs[0][leg][0];
            final ModelPart outer = this.legs[0][leg][1];
            inner.yRot = Mth.cos(movement) * 0.2F + offsets[leg];
            inner.zRot = (float) (-Math.PI / 3.0D);
            outer.yRot = Mth.cos(movement) * 0.5F + offsets[leg];
            outer.zRot = (float) (2.0D * Math.PI / 3.0D);
            movement -= 1.0F;
        }
        for (int side = 0; side < 2; side++) {
            for (int leg = 0; leg < 4; leg++) {
                // copyLegAngles: the outer segment hangs off the end of the inner one
                final ModelPart inner = this.legs[side][leg][0];
                final ModelPart outer = this.legs[side][leg][1];
                outer.x = LEG_LENGTH * (inner.x < 0.0F ? -1.0F : 1.0F) * (Mth.sin(inner.xRot) + Mth.cos(inner.yRot));
                outer.y = 0.0F;
                outer.z = 0.0F;
            }
        }
        for (int leg = 0; leg < 4; leg++) {
            // copyLeg1LeftToRight and copyLeftToRight
            final ModelPart leftInner = this.legs[0][leg][0];
            final ModelPart rightInner = this.legs[1][leg][0];
            rightInner.xRot = leftInner.xRot;
            rightInner.yRot = (float) (Math.PI - leftInner.yRot);
            rightInner.zRot = -leftInner.zRot;
            final ModelPart leftOuter = this.legs[0][leg][1];
            final ModelPart rightOuter = this.legs[1][leg][1];
            rightOuter.xRot = leftOuter.xRot;
            rightOuter.yRot = -leftOuter.yRot;
            rightOuter.zRot = leftOuter.zRot;
        }
        final boolean attacking = juicer.isAggressive();
        this.tail0.y = -1.5F;
        this.tail0.xRot = attacking ? 0.52F : 0.1F;
        this.tail0.yRot = 0.0F;
        this.tail0.zRot = 0.0F;
        this.tail1.xRot = attacking ? 2.659407F : 2.7F;
        this.tail1.z = 4.0F * (Mth.sin(this.tail0.zRot) + Mth.cos(this.tail0.yRot));
        this.tail1.y = 0.5F;
        this.tail1.x = 0.0F;
        this.stinger.xRot = -this.tail0.xRot - (this.tail0.xRot - 2.659407F) - 0.5205006F;
        this.stinger.z = 4.0F * (Mth.sin(this.tail1.zRot) + Mth.cos(this.tail1.yRot));
        this.stinger.y = 0.5F;
        this.stinger.x = 0.0F;
    }
}
