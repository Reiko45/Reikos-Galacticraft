/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.tile
 * .TileEntityShortRangeTelepadRenderer). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 1.12.2 TileEntityShortRangeTelepadRenderer: the telepad_short OBJ's pad and canopy, squeezed to 0.745 across. */
public class ShortRangeTelepadRenderer implements BlockEntityRenderer<ShortRangeTelepadBlockEntity> {
    public static final ModelResourceLocation TOP = BakedModels.location("block/short_range_telepad/top");
    public static final ModelResourceLocation BOTTOM = BakedModels.location("block/short_range_telepad/bottom");

    public ShortRangeTelepadRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(TOP);
        event.register(BOTTOM);
    }

    @Override
    public void render(ShortRangeTelepadBlockEntity telepad, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.745F, 1.0F, 0.745F);
        BakedModels.draw(poseStack, consumer, BOTTOM, light);
        poseStack.translate(0.0F, -0.7F, 0.0F);
        BakedModels.draw(poseStack, consumer, TOP, light);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ShortRangeTelepadBlockEntity telepad) {
        return new AABB(telepad.getBlockPos()).inflate(1.0D, 0.0D, 1.0D).expandTowards(0.0D, 3.0D, 0.0D);
    }
}
