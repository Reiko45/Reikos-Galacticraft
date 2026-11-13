/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.entities.RenderMeteorChunk and
 * client.model.ModelMeteorChunk). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.MeteorChunkEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.12.2 RenderMeteorChunk: a lumpy chunk of six boxes (ModelMeteorChunk) at 0.3 scale, turned by its tumble, cold or
 * glowing hot.
 */
public class MeteorChunkRenderer extends EntityRenderer<MeteorChunkEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("meteor_chunk"), "main");
    private static final ResourceLocation COLD = GalacticraftCore.id("textures/block/meteor_chunk.png");
    private static final ResourceLocation HOT = GalacticraftCore.id("textures/block/meteor_chunk_hot.png");

    private final ModelPart model;

    public MeteorChunkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
        this.model = context.bakeLayer(LAYER);
    }

    /** 1.12.2 ModelMeteorChunk (16 x 16 texture). */
    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final float[][] boxes = {
                {4, 0, -5, 1, 8, 6},
                {-3, 1, -7, 5, 8, 1},
                {-6, -1, -5, 1, 8, 6},
                {-3, -3, -4, 6, 1, 5},
                {-3, 0, 3, 5, 8, 1},
                {-5, -2, -6, 9, 12, 9},
        };
        for (int i = 0; i < boxes.length; i++) {
            final float[] b = boxes[i];
            root.addOrReplaceChild("box" + i, CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, b[3], b[4], b[5]),
                    PartPose.offset(b[0], b[1], b[2]));
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void render(MeteorChunkEntity chunk, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(0.3F, 0.3F, 0.3F);
        // 1.12.2 turned it by its yaw about x and its pitch about z (its pitch and yaw tumbled while it flew)
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, chunk.yRotO, chunk.getYRot()) + Mth.lerp(partialTick, chunk.tumbleYawO, chunk.tumbleYaw)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, chunk.xRotO, chunk.getXRot()) + Mth.lerp(partialTick, chunk.tumblePitchO, chunk.tumblePitch)));
        this.model.render(poseStack, buffers.getBuffer(RenderType.entityCutout(getTextureLocation(chunk))), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(chunk, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorChunkEntity chunk) {
        return chunk.isHot() ? HOT : COLD;
    }
}
