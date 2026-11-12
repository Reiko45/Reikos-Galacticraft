/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.entity
 * .RenderSmallAsteroid). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.entity.SmallAsteroidEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.12.2 RenderSmallAsteroid: a block of asteroid rock half a block up, tumbling about its corner (1.12.2 rotated the
 * block model about its own origin).
 */
public class SmallAsteroidRenderer extends EntityRenderer<SmallAsteroidEntity> {
    public SmallAsteroidRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SmallAsteroidEntity asteroid, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, asteroid.xRotO, asteroid.getXRot())));
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, asteroid.yRotO, asteroid.getYRot())));
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(GCBlocks.ASTEROID_ROCK_0.get().defaultBlockState(), poseStack, buffers, light,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(asteroid, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SmallAsteroidEntity asteroid) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
