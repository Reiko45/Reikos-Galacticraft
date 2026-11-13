/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.tile.TileEntityArclampRenderer).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.block.ArcLampBlock;
import io.github.reiko45.galacticraft.block.entity.ArcLampBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * 1.12.2 TileEntityArclampRenderer: the lamp head (arclamp_metal OBJ, tilted 45 degrees out from its plate and turned
 * the way it aims) and its face, a square drawn full bright: white while lit, near black when switched off.
 */
public class ArcLampRenderer implements BlockEntityRenderer<ArcLampBlockEntity> {
    public static final ModelResourceLocation LAMP = BakedModels.location("entity/arclamp_metal");
    private static final float FRAME_A = -3.4331F;
    private static final float FRAME_B = 3.4331F;
    private static final float FRAME_Y = 2.3703F;

    public ArcLampRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcLampBlockEntity lamp, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final int side = lamp.getBlockState().hasProperty(ArcLampBlock.FACING) ? lamp.getBlockState().getValue(ArcLampBlock.FACING).get3DDataValue() : 0;
        int metaFacing = lamp.facing();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (side) {
            case 1 -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                if (metaFacing < 2) {
                    metaFacing ^= 1;
                }
            }
            case 2 -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                metaFacing ^= 1;
            }
            case 3 -> poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            case 4 -> {
                poseStack.mulPose(Axis.ZN.rotationDegrees(90.0F));
                metaFacing -= 2;
                if (metaFacing < 0) {
                    metaFacing = 1 - metaFacing;
                }
            }
            case 5 -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                metaFacing += 2;
                if (metaFacing > 3) {
                    metaFacing = 5 - metaFacing;
                }
            }
            default -> {
            }
        }
        poseStack.translate(0.0F, -0.175F, 0.0F);
        switch (metaFacing) {
            case 1 -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case 2 -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case 3 -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> {
            }
        }
        poseStack.mulPose(Axis.XN.rotationDegrees(45.0F));
        poseStack.scale(0.048F, 0.048F, 0.048F);
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), LAMP, light);
        // the lamp's face: full bright white while lit, near black when off
        final float grey = lamp.isLit() ? 1.0F : 26.0F / 255.0F;
        final VertexConsumer face = buffers.getBuffer(RenderType.debugQuads());
        final Matrix4f matrix = poseStack.last().pose();
        face.addVertex(matrix, FRAME_A, FRAME_Y, FRAME_B).setColor(grey, grey, grey, 1.0F);
        face.addVertex(matrix, FRAME_B, FRAME_Y, FRAME_B).setColor(grey, grey, grey, 1.0F);
        face.addVertex(matrix, FRAME_B, FRAME_Y, FRAME_A).setColor(grey, grey, grey, 1.0F);
        face.addVertex(matrix, FRAME_A, FRAME_Y, FRAME_A).setColor(grey, grey, grey, 1.0F);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ArcLampBlockEntity lamp) {
        return lamp.renderBounds();
    }
}
