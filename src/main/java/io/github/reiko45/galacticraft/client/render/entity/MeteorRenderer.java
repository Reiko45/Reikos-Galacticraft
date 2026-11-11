package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.MeteorEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12.2 RenderMeteor + ModelMeteor: a lumpy 20-pixel cube spun about two axes, scaled by size / 2. 1.12.2 drew the
 * model at 0.1 blocks per pixel instead of the usual 1/16.
 */
public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("meteor"), "main");
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/meteor.png");

    private final ModelPart model;

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = context.bakeLayer(LAYER);
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        // ModelMeteor shapes 0-12 (all texture offset 0, 0, mirrored, 128x64)
        final float[][] boxes = {
                {0, -7, -13, 2, 4, 4}, {-10, -10, -10, 20, 20, 20}, {-5, -8, -12, 5, 9, 1}, {0, -6, 11, 4, 13, 1},
                {-9, 10, -9, 18, 1, 18}, {11, 3, -8, 1, 5, 5}, {-7, -8, 10, 7, 12, 2}, {-9, -9, 10, 18, 18, 1},
                {-11, -9, -9, 1, 18, 18}, {10, -9, -9, 1, 18, 18}, {-9, -9, -11, 18, 18, 1}, {-9, -9, -11, 18, 18, 1},
                {-9, -11, -9, 18, 1, 18}};
        final CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, 0).mirror();
        for (float[] b : boxes) {
            cubes.addBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        mesh.getRoot().addOrReplaceChild("rock", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void render(MeteorEntity meteor, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(entityYaw));
        final float scale = meteor.size() / 2.0F * 1.6F;
        poseStack.scale(scale, scale, scale);
        this.model.render(poseStack, buffers.getBuffer(RenderType.entitySolid(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(meteor, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity meteor) {
        return TEXTURE;
    }
}
