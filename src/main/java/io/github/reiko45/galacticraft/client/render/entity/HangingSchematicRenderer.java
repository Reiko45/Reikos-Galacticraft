/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.entities.RenderSchematic).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.entity.HangingSchematicEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.12.2 RenderSchematic: the schematic's item texture stretched over a 2 x 2 painting, 1/16 thick, with the back and
 * edges taken from a corner of the same texture; each quarter is lit from the block it covers.
 */
public class HangingSchematicRenderer extends EntityRenderer<HangingSchematicEntity> {
    private static final float HALF_DEPTH = 0.03125F;

    public HangingSchematicRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(HangingSchematicEntity entity) {
        final ResourceLocation item = BuiltInRegistries.ITEM.getKey(entity.schematicItem().getItem());
        return item.withPath(path -> "textures/item/" + path + ".png");
    }

    @Override
    public void render(HangingSchematicEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
        final PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                final float x1 = -1.0F + i + 1;
                final float x0 = -1.0F + i;
                final float y1 = -1.0F + j + 1;
                final float y0 = -1.0F + j;
                final int quarterLight = quarterLight(entity, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F);
                final float u0 = (2 - i) / 2.0F;
                final float u1 = (2 - (i + 1)) / 2.0F;
                final float v0 = (2 - j) / 2.0F;
                final float v1 = (2 - (j + 1)) / 2.0F;
                // front
                vertex(pose, consumer, x1, y0, -HALF_DEPTH, u1, v0, 0, 0, -1, quarterLight);
                vertex(pose, consumer, x0, y0, -HALF_DEPTH, u0, v0, 0, 0, -1, quarterLight);
                vertex(pose, consumer, x0, y1, -HALF_DEPTH, u0, v1, 0, 0, -1, quarterLight);
                vertex(pose, consumer, x1, y1, -HALF_DEPTH, u1, v1, 0, 0, -1, quarterLight);
                // back (1.12.2 u 0.75-0.8125, v 0-0.0625)
                vertex(pose, consumer, x1, y1, HALF_DEPTH, 0.75F, 0.0F, 0, 0, 1, quarterLight);
                vertex(pose, consumer, x0, y1, HALF_DEPTH, 0.8125F, 0.0F, 0, 0, 1, quarterLight);
                vertex(pose, consumer, x0, y0, HALF_DEPTH, 0.8125F, 0.0625F, 0, 0, 1, quarterLight);
                vertex(pose, consumer, x1, y0, HALF_DEPTH, 0.75F, 0.0625F, 0, 0, 1, quarterLight);
                // top and bottom
                vertex(pose, consumer, x1, y1, -HALF_DEPTH, 0.75F, 0.001953125F, 0, 1, 0, quarterLight);
                vertex(pose, consumer, x0, y1, -HALF_DEPTH, 0.8125F, 0.001953125F, 0, 1, 0, quarterLight);
                vertex(pose, consumer, x0, y1, HALF_DEPTH, 0.8125F, 0.001953125F, 0, 1, 0, quarterLight);
                vertex(pose, consumer, x1, y1, HALF_DEPTH, 0.75F, 0.001953125F, 0, 1, 0, quarterLight);
                vertex(pose, consumer, x1, y0, HALF_DEPTH, 0.75F, 0.001953125F, 0, -1, 0, quarterLight);
                vertex(pose, consumer, x0, y0, HALF_DEPTH, 0.8125F, 0.001953125F, 0, -1, 0, quarterLight);
                vertex(pose, consumer, x0, y0, -HALF_DEPTH, 0.8125F, 0.001953125F, 0, -1, 0, quarterLight);
                vertex(pose, consumer, x1, y0, -HALF_DEPTH, 0.75F, 0.001953125F, 0, -1, 0, quarterLight);
                // sides
                vertex(pose, consumer, x1, y1, HALF_DEPTH, 0.7519531F, 0.0F, -1, 0, 0, quarterLight);
                vertex(pose, consumer, x1, y0, HALF_DEPTH, 0.7519531F, 0.0625F, -1, 0, 0, quarterLight);
                vertex(pose, consumer, x1, y0, -HALF_DEPTH, 0.7519531F, 0.0625F, -1, 0, 0, quarterLight);
                vertex(pose, consumer, x1, y1, -HALF_DEPTH, 0.7519531F, 0.0F, -1, 0, 0, quarterLight);
                vertex(pose, consumer, x0, y1, -HALF_DEPTH, 0.7519531F, 0.0F, 1, 0, 0, quarterLight);
                vertex(pose, consumer, x0, y0, -HALF_DEPTH, 0.7519531F, 0.0625F, 1, 0, 0, quarterLight);
                vertex(pose, consumer, x0, y0, HALF_DEPTH, 0.7519531F, 0.0625F, 1, 0, 0, quarterLight);
                vertex(pose, consumer, x0, y1, HALF_DEPTH, 0.7519531F, 0.0F, 1, 0, 0, quarterLight);
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    /** 1.12.2 setLightmap: the light of the block a quarter covers. */
    private static int quarterLight(HangingSchematicEntity entity, float along, float up) {
        int x = entity.getBlockX();
        final int y = Mth.floor(entity.getY() + up);
        int z = entity.getBlockZ();
        final Direction direction = entity.getDirection();
        switch (direction) {
            case NORTH -> x = Mth.floor(entity.getX() + along);
            case WEST -> z = Mth.floor(entity.getZ() - along);
            case SOUTH -> x = Mth.floor(entity.getX() - along);
            case EAST -> z = Mth.floor(entity.getZ() + along);
            default -> {
            }
        }
        return LevelRenderer.getLightColor(entity.level(), new BlockPos(x, y, z));
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v,
                               int nx, int ny, int nz, int light) {
        consumer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
