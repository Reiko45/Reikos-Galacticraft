/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.tile.TileEntityDishRenderer).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.block.entity.CommsDishBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * 1.12.2 TileEntityDishRenderer: the telesupport post, the telefork turning on it and the teledish tipping in the
 * fork, with 1.12.2's scales and offsets.
 */
public class CommsDishRenderer implements BlockEntityRenderer<CommsDishBlockEntity> {
    public static final ModelResourceLocation SUPPORT = BakedModels.location("entity/telesupport");
    public static final ModelResourceLocation FORK = BakedModels.location("entity/telefork");
    public static final ModelResourceLocation DISH = BakedModels.location("entity/teledish");

    public CommsDishRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CommsDishBlockEntity dish, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final float hour = dish.rotation(partialTick) % 360.0F;
        final float declination = dish.elevation(partialTick) % 360.0F;
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.scale(1.6F, 1.25F, 1.6F);
        BakedModels.draw(poseStack, consumer, SUPPORT, light);
        poseStack.scale(1.25F, 1.6F, 1.25F);
        poseStack.translate(0.0F, 2.88F * 0.15F / 1.6F, 0.0F);
        poseStack.scale(0.85F, 0.85F, 0.85F);
        poseStack.mulPose(Axis.YN.rotationDegrees(hour));
        BakedModels.draw(poseStack, consumer, FORK, light);
        poseStack.translate(0.0F, 2.3F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(declination));
        poseStack.translate(0.0F, -2.3F, 0.0F);
        BakedModels.draw(poseStack, consumer, DISH, light);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CommsDishBlockEntity dish) {
        return dish.renderBox();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
