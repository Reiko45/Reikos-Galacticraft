/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.tile
 * .TileEntityMinerBaseRenderer). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 TileEntityMinerBaseRenderer: the master draws the whole base (the minerbase0 OBJ at 0.05 scale, centred in the
 * 2 x 2 x 2 block, its dock facing the base's front), lit by the average light of its eight blocks.
 */
public class MinerBaseRenderer implements BlockEntityRenderer<MinerBaseBlockEntity> {
    public static final ModelResourceLocation MODEL = BakedModels.location("block/miner_base_model");

    public MinerBaseRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(MinerBaseBlockEntity base, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!base.isMaster() || base.getLevel() == null) {
            return;
        }
        int block = 0;
        int sky = 0;
        for (BlockPos part : MinerBaseBlockEntity.parts(base.getBlockPos())) {
            final int partLight = LevelRenderer.getLightColor(base.getLevel(), part);
            block += LightTexture.block(partLight);
            sky += LightTexture.sky(partLight);
        }
        final int averaged = LightTexture.pack(block / 8, sky / 8);
        poseStack.pushPose();
        poseStack.translate(1.0F, 1.0F, 1.0F);
        poseStack.scale(0.05F, 0.05F, 0.05F);
        switch (base.facing()) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> {
            }
        }
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), MODEL, averaged);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MinerBaseBlockEntity base) {
        return new AABB(base.getBlockPos()).expandTowards(1.0D, 1.0D, 1.0D);
    }
}
