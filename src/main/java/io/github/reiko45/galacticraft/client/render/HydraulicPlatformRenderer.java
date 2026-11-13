/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.tile.TileEntityPlatformRenderer).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.HydraulicPlatformBlock;
import io.github.reiko45.galacticraft.block.entity.HydraulicPlatformBlockEntity;
import io.github.reiko45.galacticraft.client.PlatformLift;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * 1.12.2 TileEntityPlatformRenderer, drawn from a platform's north-west corner: the platform's floor (a 1.125-block panel
 * in the hole the four corners leave), which rides with the player while the lift runs, with amber edge lights; and,
 * while someone stands on it, a ring of light round it, green, or red for a moment when the way is blocked.
 */
public class HydraulicPlatformRenderer implements BlockEntityRenderer<HydraulicPlatformBlockEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("hydraulic_platform"), "main");
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/platform_moving.png");

    private final ModelPart panel;

    public HydraulicPlatformRenderer(BlockEntityRendererProvider.Context context) {
        this.panel = context.bakeLayer(LAYER).getChild("panel");
    }

    /** 1.12.2 ModelPlatform: one 44 x 7 x 44 box (128 x 128 texture), drawn 1.125 blocks wide. */
    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("panel", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-22, -3.5F, -22, 44, 7, 44), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(HydraulicPlatformBlockEntity platform, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (platform.getBlockState().getValue(HydraulicPlatformBlock.TYPE) != HydraulicPlatformBlock.Corner.NW) {
            return;
        }
        final boolean moving = platform.isMoving();
        if (moving && !PlatformLift.drawsMovingFloor(platform)) {
            return; // the floor of the platform passed through rides with the player, drawn by the lower one
        }
        float yOffset = 0.0F;
        if (moving && Minecraft.getInstance().player != null) {
            final var player = Minecraft.getInstance().player;
            yOffset = (float) (Mth.lerp(partialTick, player.yo, player.getY()) - platform.getBlockPos().getY() - HydraulicPlatformBlock.HEIGHT);
        }
        poseStack.pushPose();
        poseStack.translate(1.0F, 0.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.79F + yOffset, 0.0F);
        final float scale = 1.125F / 44.0F * 16.0F;
        poseStack.scale(scale, scale, scale);
        this.panel.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        final VertexConsumer lights = buffers.getBuffer(RenderType.debugQuads());
        if (moving) {
            // amber strips round the edge of the moving floor
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.26F + yOffset, 0.0F);
            final float radius = 1.126F / 2.0F;
            for (int side = 0; side < 4; side++) {
                strip(lights, poseStack.last().pose(), radius, 0.552F, 0.578F, radius - 0.04F, 1.0F, 0.84F, 65.0F / 255.0F);
                strip(lights, poseStack.last().pose(), radius, 0.482F, 0.504666F, radius - 0.04F, 1.0F, 0.84F, 65.0F / 255.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            poseStack.popPose();
        }
        if (platform.lightOn()) {
            final float grey = 125.0F / 255.0F;
            final float r = platform.lightColor() == 1 ? 1.0F : grey;
            final float g = platform.lightColor() == 1 ? 115.0F / 255.0F : 1.0F;
            final float b = platform.lightColor() == 1 ? 115.0F / 255.0F : grey;
            for (int side = 0; side < 4; side++) {
                final Matrix4f pose = poseStack.last().pose();
                lights.addVertex(pose, -0.58F, 0.9376F, 0.72F).setColor(r, g, b, 1.0F);
                lights.addVertex(pose, 0.58F, 0.9376F, 0.72F).setColor(r, g, b, 1.0F);
                lights.addVertex(pose, 0.58F, 0.9376F, 0.7F).setColor(r, g, b, 1.0F);
                lights.addVertex(pose, -0.58F, 0.9376F, 0.7F).setColor(r, g, b, 1.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.popPose();
    }

    /** One upright strip on the -x side, {@code y0} to {@code y1}, {@code half} either side of the middle. */
    private static void strip(VertexConsumer consumer, Matrix4f pose, float radius, float y0, float y1, float half, float r, float g, float b) {
        consumer.addVertex(pose, -radius, y0, half).setColor(r, g, b, 1.0F);
        consumer.addVertex(pose, -radius, y1, half).setColor(r, g, b, 1.0F);
        consumer.addVertex(pose, -radius, y1, -half).setColor(r, g, b, 1.0F);
        consumer.addVertex(pose, -radius, y0, -half).setColor(r, g, b, 1.0F);
    }

    @Override
    public AABB getRenderBoundingBox(HydraulicPlatformBlockEntity platform) {
        return platform.renderBox();
    }

    @Override
    public boolean shouldRenderOffScreen(HydraulicPlatformBlockEntity platform) {
        return platform.isMoving();
    }
}
