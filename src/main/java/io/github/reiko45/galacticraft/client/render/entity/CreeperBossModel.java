package io.github.reiko45.galacticraft.client.render.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.CreeperBossEntity;
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
 * 1.12.2 ModelCreeperBoss: a four-legged creeper in an oxygen-tank helmet with two more heads on necks. It loses a head
 * at two thirds and at one third of its health (1.12.2 headsRemaining).
 */
public class CreeperBossModel extends HierarchicalModel<CreeperBossEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("creeper_boss"), "main");
    private final ModelPart root;
    private final ModelPart headMain;
    private final ModelPart oxygenTank;
    private final ModelPart headLeft;
    private final ModelPart neckLeft;
    private final ModelPart headRight;
    private final ModelPart neckRight;
    private final ModelPart rightLegFront;
    private final ModelPart leftLegFront;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CreeperBossModel(ModelPart root) {
        this.root = root;
        this.headMain = root.getChild("head_main");
        this.oxygenTank = root.getChild("oxygen_tank");
        this.headLeft = root.getChild("head_left");
        this.neckLeft = root.getChild("neck_left");
        this.headRight = root.getChild("head_right");
        this.neckRight = root.getChild("neck_right");
        this.rightLegFront = root.getChild("right_leg_front");
        this.leftLegFront = root.getChild("left_leg_front");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("neck_right", CubeListBuilder.create().texOffs(16, 20).addBox(-2.5F, -9.0F, -1.5F, 5, 9, 3),
                PartPose.offsetAndRotation(-3.0F, 10.0F, 0.0F, 0.0F, 0.0F, -1.169371F));
        root.addOrReplaceChild("neck_left", CubeListBuilder.create().texOffs(16, 20).mirror().addBox(-2.5F, -9.0F, -1.5F, 5, 9, 3),
                PartPose.offsetAndRotation(3.0F, 10.0F, 0.0F, 0.0F, 0.0F, 1.169371F));
        root.addOrReplaceChild("head_main", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("body_main", CubeListBuilder.create().texOffs(16, 16).mirror().addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("right_leg_front", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(-2.0F, 18.0F, -4.0F));
        root.addOrReplaceChild("left_leg_front", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(2.0F, 18.0F, -4.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(-4.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(2.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("oxygen_tank", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-5.0F, -9.0F, -5.0F, 10, 10, 10),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("head_left", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(1.0F, -9.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(3.0F, 6.0F, 0.1F, 0.0F, 0.0F, 0.7853982F));
        root.addOrReplaceChild("head_right", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-9.0F, -9.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(-3.0F, 6.0F, -0.1F, 0.0F, 0.0F, -0.7853982F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(CreeperBossEntity boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.headMain.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.headMain.xRot = headPitch * Mth.DEG_TO_RAD;
        this.oxygenTank.yRot = this.headMain.yRot;
        this.oxygenTank.xRot = this.headMain.xRot;
        this.rightLegFront.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLegFront.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 2.0F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 2.0F * limbSwingAmount;
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount;
        final int heads = boss.headsRemaining();
        this.headLeft.visible = heads > 2;
        this.neckLeft.visible = heads > 2;
        this.headRight.visible = heads > 1;
        this.neckRight.visible = heads > 1;
        this.headMain.visible = heads > 0;
        this.oxygenTank.visible = heads > 0;
    }
}
