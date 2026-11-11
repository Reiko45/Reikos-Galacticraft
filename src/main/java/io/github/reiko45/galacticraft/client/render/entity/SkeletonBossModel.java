package io.github.reiko45.galacticraft.client.render.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SkeletonBossEntity;
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
 * 1.12.2 ModelEvolvedSkeletonBoss: a 32-part skeleton (mesh generated from the 1.12.2 code). The arms aim forwards
 * (the 1.12.2 bow pose), sway idly, lift while it holds someone and drop slowly while it dies.
 */
public class SkeletonBossModel extends HierarchicalModel<SkeletonBossEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("skeleton_boss"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart spine;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public SkeletonBossModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("upperhead");
        this.spine = root.getChild("spine");
        this.rightArm = root.getChild("rightarm");
        this.leftArm = root.getChild("leftarm");
        this.rightLeg = root.getChild("rightleg");
        this.leftLeg = root.getChild("leftleg");
    }

    public static LayerDefinition createLayer() {
            final MeshDefinition mesh = new MeshDefinition();
            final PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("upperhead", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4.0F, -8.0F, -6.0F, 8.0F, 8.0F, 8.0F),
                    PartPose.offsetAndRotation(0.0F, -24.0F, 6.0F, 0.122173F, 0.0F, 0.0F));
            root.addOrReplaceChild("pelvis", CubeListBuilder.create().texOffs(32, 19).mirror().addBox(-6.0F, 0.0F, -3.0F, 12.0F, 5.0F, 5.0F),
                    PartPose.offsetAndRotation(0.0F, -2.0F, 5.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("sternum", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.5F, 0.0F, -1.0F, 3.0F, 9.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -21.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightleg", CubeListBuilder.create().texOffs(56, 33).mirror().addBox(-2.0F, 0.0F, -2.0F, 3.0F, 26.0F, 3.0F),
                    PartPose.offsetAndRotation(-5.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightarm", CubeListBuilder.create().texOffs(56, 33).mirror().addBox(-2.0F, -2.0F, -1.5F, 3.0F, 24.0F, 3.0F),
                    PartPose.offsetAndRotation(-8.0F, -20.0F, 5.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("spine", CubeListBuilder.create().texOffs(32, 33).mirror().addBox(-1.5F, 0.0F, -1.0F, 3.0F, 22.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, -24.0F, 6.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftarm", CubeListBuilder.create().texOffs(56, 33).mirror().addBox(-1.0F, -2.0F, -1.5F, 3.0F, 24.0F, 3.0F),
                    PartPose.offsetAndRotation(8.0F, -20.0F, 5.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftleg", CubeListBuilder.create().texOffs(56, 33).mirror().addBox(-2.0F, 0.0F, -2.0F, 3.0F, 26.0F, 3.0F),
                    PartPose.offsetAndRotation(6.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftfrontbotrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -13.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftfronttoprib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -22.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftfront2ndrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -19.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftfront3rdrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -16.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftsidebotrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -13.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftside3rdrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftside2ndrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -19.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftsidetoprib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -22.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightsidetoprib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -22.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightside2ndrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -19.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightside3rdrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightsidebotrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, -6.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -13.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightfrontbotrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -13.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightfront3rdrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -16.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightfront2ndrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -19.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightfronttoprib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -22.0F, 2.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftbacktoprib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -22.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftback2ndrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -19.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftback3rdrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leftbackbotrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(7.0F, -13.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightbackbotrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -13.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightback3rdrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -16.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightback2ndrib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -19.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("rightbacktoprib", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(-7.0F, -22.0F, 7.0F, 0.0F, 0.0F, 0.0F));
            return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    public ModelPart arm(boolean right) {
        return right ? this.rightArm : this.leftArm;
    }

    /** 1.12.2 setRotationAngles, keeping only what its last assignments leave in effect. */
    @Override
    public void setupAnim(SkeletonBossEntity boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.yRot = yaw;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        final float swing = this.attackTime;
        this.spine.yRot = Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI) * 0.2F;
        this.rightArm.z = Mth.sin(this.spine.yRot) * 5.0F;
        this.rightArm.x = -Mth.cos(this.spine.yRot) * 5.0F;
        this.leftArm.z = -Mth.sin(this.spine.yRot) * 5.0F;
        this.leftArm.x = Mth.cos(this.spine.yRot) * 5.0F;

        final float f6 = Mth.sin(swing * Mth.PI);
        final float f7 = Mth.sin((1.0F - (1.0F - swing) * (1.0F - swing)) * Mth.PI);
        this.rightArm.yRot = 0.5F * yaw - (0.1F - f6 * 0.6F);
        this.leftArm.yRot = 0.5F * yaw + 0.1F - f6 * 0.6F;
        this.rightArm.xRot = -Mth.HALF_PI - (f6 * 1.2F - f7 * 0.4F) + Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot = -Mth.HALF_PI - (f6 * 1.2F - f7 * 0.4F) - Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.rightArm.zRot = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot = -(Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);

        if (boss.deathTime > 0) {
            final float drop = boss.deathTime * boss.deathTime / 5.0F * Mth.DEG_TO_RAD;
            this.leftArm.xRot = -Mth.HALF_PI + drop / 3.0F;
            this.rightArm.xRot = -Mth.HALF_PI + drop / 2.0F;
        }
        final int throwing = boss.throwTicks();
        if (throwing > 0) {
            final float lift = Mth.cos(throwing * 0.05F) * 1.2F + 0.05F;
            this.rightArm.xRot -= lift;
            this.leftArm.xRot -= lift;
        }
    }
}
