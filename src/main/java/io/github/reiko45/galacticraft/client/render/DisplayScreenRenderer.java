/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.tile.TileEntityScreenRenderer and
 * client.screen.DrawGameScreen). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.block.DisplayScreenBlock;
import io.github.reiko45.galacticraft.block.entity.DisplayScreenBlockEntity;
import io.github.reiko45.galacticraft.client.screen.display.GameScreens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/**
 * 1.12.2 TileEntityScreenRenderer: the picture on the front of a screen, drawn once for a whole big screen from its top
 * left block (1.12.2 counted the blocks drawn each frame to draw it once), full bright, with 1.12.2's turns for each
 * facing.
 */
public class DisplayScreenRenderer implements BlockEntityRenderer<DisplayScreenBlockEntity> {
    public DisplayScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DisplayScreenBlockEntity screen, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!screen.drawsScreen() || screen.getLevel() == null) {
            return;
        }
        poseStack.pushPose();
        switch (screen.getBlockState().getValue(DisplayScreenBlock.FACING)) {
            case NORTH -> {
                poseStack.translate(0.0F, 0.0F, -0.87F);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.translate(0.0F, 0.0F, -1.0F);
            }
            case SOUTH -> {
                poseStack.translate(0.0F, 0.0F, 0.87F);
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
                poseStack.translate(1.0F, -1.0F, 1.0F);
                poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
            }
            case WEST -> {
                poseStack.translate(-0.87F, 0.0F, 0.0F);
                poseStack.mulPose(Axis.ZN.rotationDegrees(90.0F));
                poseStack.translate(-1.0F, 0.0F, 1.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            default -> {
                poseStack.translate(0.87F, 0.0F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                poseStack.translate(1.0F, -1.0F, 0.0F);
                poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
            }
        }
        final float sizeX = screen.width();
        final float sizeY = screen.height();
        poseStack.translate(0.0F, 0.91F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(-sizeX, 0.0F, 0.0F);
        GameScreens.render(screen, screen.imageType, partialTick + screen.getLevel().getDayTime(), sizeX, sizeY, poseStack, buffers, partialTick);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DisplayScreenBlockEntity screen) {
        return screen.renderBox();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
