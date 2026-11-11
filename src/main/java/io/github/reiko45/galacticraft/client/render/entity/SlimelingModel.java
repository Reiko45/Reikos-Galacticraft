package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * 1.12.2 ModelSlimeling (built with scale 16, its smaller set of boxes): a round body, a three-part tail behind and a
 * neck and head on top, tinted the slimeling's colour. Walking wags the tail and bobs the neck back and forth.
 */
public class SlimelingModel extends HierarchicalModel<SlimelingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("slimeling"), "main");
    private final ModelPart root;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart neck;
    private final ModelPart head;
    private int tint = -1;

    public SlimelingModel(ModelPart root) {
        this.root = root;
        final ModelPart body = root.getChild("body");
        this.tail1 = body.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.neck = body.getChild("neck");
        this.head = this.neck.getChild("head");
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition body = mesh.getRoot().addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(82, 0).mirror().addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10), PartPose.ZERO);
        final PartDefinition tail1 = body.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-4.5F, -2.0F, -10.0F, 9, 7, 8), PartPose.ZERO);
        final PartDefinition tail2 = tail1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(20, 0).mirror().addBox(-3.5F, 0.0F, -14.0F, 7, 5, 7), PartPose.ZERO);
        tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-2.5F, 2.0F, -16.0F, 5, 3, 5), PartPose.ZERO);
        final PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(122, 0).mirror().addBox(-4.5F, -9.5F, -2.0F, 9, 9, 8), PartPose.ZERO);
        neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(156, 0).mirror().addBox(-3.5F, -14.7F, 1.5F, 7, 7, 7), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(SlimelingEntity slimeling, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final float wag = Mth.cos(limbSwing * 0.6662F) * 0.2F * limbSwingAmount;
        this.tail1.yRot = wag;
        this.tail2.yRot = wag;
        this.tail3.yRot = wag;
        // 1.12.2 offsetZ was in blocks; part positions are in pixels
        final float sway = Mth.cos(0.5F * limbSwing * 0.6662F) * limbSwingAmount * 16.0F;
        this.tail1.z = sway * 0.2F;
        this.tail2.z = sway * 0.2F;
        this.tail3.z = sway * 0.2F;
        this.neck.z = -sway * 0.1F;
        this.head.z = -sway * 0.1F;
        this.tint = FastColor.ARGB32.colorFromFloat(1.0F, slimeling.red(), slimeling.green(), slimeling.blue());
    }

    /** 1.12.2 RenderSlimeling#preRenderCallback: GL11.glColor3f(red, green, blue). */
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, FastColor.ARGB32.multiply(color, this.tint));
    }
}
