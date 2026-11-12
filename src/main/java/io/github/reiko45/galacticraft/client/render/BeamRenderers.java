/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.tile
 * .TileEntityBeamReflectorRenderer, TileEntityBeamReceiverRenderer and client.render.NetworkRenderer). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.beam.LaserNode;
import io.github.reiko45.galacticraft.block.entity.BeamOutputBlockEntity;
import io.github.reiko45.galacticraft.block.entity.BeamReceiverBlockEntity;
import io.github.reiko45.galacticraft.block.entity.BeamReflectorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

/** The beam turret renderers (1.12.2 drew both blocks only through these) and the beams between nodes. */
public final class BeamRenderers {
    public static final ModelResourceLocation REFLECTOR_BASE = BakedModels.location("block/beam_reflector/base");
    public static final ModelResourceLocation REFLECTOR_AXLE = BakedModels.location("block/beam_reflector/axle");
    public static final ModelResourceLocation REFLECTOR_BLASTER = BakedModels.location("block/beam_reflector/energy_blaster");
    public static final ModelResourceLocation REFLECTOR_RING = BakedModels.location("block/beam_reflector/ring");
    public static final ModelResourceLocation RECEIVER_MAIN = BakedModels.location("block/beam_receiver/main");
    public static final ModelResourceLocation RECEIVER_HEAD = BakedModels.location("block/beam_receiver/receiver");
    public static final ModelResourceLocation RECEIVER_RING = BakedModels.location("block/beam_receiver/ring");
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private BeamRenderers() {
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : new ModelResourceLocation[]{REFLECTOR_BASE, REFLECTOR_AXLE, REFLECTOR_BLASTER, REFLECTOR_RING,
                RECEIVER_MAIN, RECEIVER_HEAD, RECEIVER_RING}) {
            event.register(model);
        }
    }

    /**
     * 1.12.2 NetworkRenderer: six thin lines around the axis from this node's output to its target's input, turning 10
     * degrees a tick, in the beam colour.
     */
    static void renderBeam(BeamOutputBlockEntity node, PoseStack poseStack, MultiBufferSource buffers) {
        final LaserNode target = node.target();
        if (target == null) {
            return;
        }
        final Vec3 output = node.outputPoint();
        final float length = (float) output.distanceTo(target.inputPoint());
        final int color = node.color();
        final int r = color >> 16 & 255;
        final int g = color >> 8 & 255;
        final int b = color & 255;
        poseStack.pushPose();
        poseStack.translate(output.x - node.getBlockPos().getX(), output.y - node.getBlockPos().getY(), output.z - node.getBlockPos().getZ());
        poseStack.mulPose(Axis.YP.rotationDegrees(node.yaw + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-node.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(node.ticks * 10.0F));
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        final PoseStack.Pose pose = poseStack.last();
        for (Direction direction : Direction.values()) {
            final float x = direction.getStepX() / 40.0F;
            final float y = direction.getStepY() / 40.0F;
            final float z = direction.getStepZ() / 40.0F;
            lines.addVertex(pose, x, y, z).setColor(r, g, b, 255).setNormal(pose, 0.0F, 0.0F, 1.0F);
            lines.addVertex(pose, x, y, length + z).setColor(r, g, b, 255).setNormal(pose, 0.0F, 0.0F, 1.0F);
        }
        poseStack.popPose();
    }

    static AABB renderBox(BeamOutputBlockEntity node) {
        final LaserNode target = node.target();
        final AABB own = new AABB(node.getBlockPos()).expandTowards(0.0D, 1.0D, 0.0D);
        return target == null ? own : own.minmax(new AABB(target.nodePos()));
    }

    /** 1.12.2 TileEntityBeamReflectorRenderer: base, axle turned to the target's bearing, the blaster tilted, the ring spinning. */
    public static final class Reflector implements BlockEntityRenderer<BeamReflectorBlockEntity> {
        public Reflector(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(BeamReflectorBlockEntity reflector, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            BakedModels.draw(poseStack, consumer, REFLECTOR_BASE, light);
            poseStack.mulPose(Axis.YP.rotationDegrees(reflector.yaw));
            BakedModels.draw(poseStack, consumer, REFLECTOR_AXLE, light);
            final float headY = 1.13228F;
            poseStack.translate(0.0F, headY, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(reflector.pitch));
            poseStack.translate(0.0F, -headY, 0.0F);
            BakedModels.draw(poseStack, consumer, REFLECTOR_BLASTER, light);
            poseStack.translate(0.0F, headY, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(reflector.ticks * 5.0F));
            poseStack.translate(0.0F, -headY, 0.0F);
            BakedModels.draw(poseStack, consumer, REFLECTOR_RING, light);
            poseStack.popPose();
            renderBeam(reflector, poseStack, buffers);
        }

        @Override
        public boolean shouldRenderOffScreen(BeamReflectorBlockEntity reflector) {
            return true;
        }

        @Override
        public AABB getRenderBoundingBox(BeamReflectorBlockEntity reflector) {
            return renderBox(reflector);
        }
    }

    /**
     * 1.12.2 TileEntityBeamReceiverRenderer: turned to its machine, its head plain green when receiving, blue when sending,
     * near-black when it has nothing to do; the ring spins (50 degrees a tick) while it is attached to something useful.
     */
    public static final class Receiver implements BlockEntityRenderer<BeamReceiverBlockEntity> {
        public Receiver(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(BeamReceiverBlockEntity receiver, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            final Direction facing = receiver.facing();
            if (facing == null) {
                return;
            }
            final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.scale(0.85F, 0.85F, 0.85F);
            switch (facing) {
                case DOWN -> {
                    poseStack.translate(0.7F, -0.15F, 0.0F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                }
                case UP -> {
                    poseStack.translate(-0.7F, 1.3F, 0.0F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                }
                case EAST -> {
                    poseStack.translate(0.7F, -0.15F, 0.0F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                }
                case SOUTH -> {
                    poseStack.translate(0.0F, -0.15F, 0.7F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                }
                case WEST -> poseStack.translate(-0.7F, -0.15F, 0.0F);
                case NORTH -> {
                    poseStack.translate(0.0F, -0.15F, -0.7F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                }
            }
            BakedModels.draw(poseStack, consumer, RECEIVER_MAIN, light);
            final float[] color = switch (receiver.mode()) {
                case RECEIVE -> new float[]{0.0F, 204.0F / 255.0F, 0.0F};
                case EXTRACT -> new float[]{0.0F, 0.0F, 153.0F / 255.0F};
                default -> new float[]{25.0F / 255.0F, 25.0F / 255.0F, 25.0F / 255.0F};
            };
            BakedModels.drawTinted(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)), RECEIVER_HEAD, light, color[0], color[1], color[2]);
            final float dX = 0.34772F;
            final float dY = 0.75097F;
            poseStack.translate(dX, dY, 0.0F);
            if (receiver.mode() != BeamReceiverBlockEntity.Mode.UNDEFINED) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-receiver.ticks * 50.0F));
            }
            poseStack.translate(-dX, -dY, 0.0F);
            BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), RECEIVER_RING, light);
            poseStack.popPose();
            renderBeam(receiver, poseStack, buffers);
        }

        @Override
        public boolean shouldRenderOffScreen(BeamReceiverBlockEntity receiver) {
            return true;
        }

        @Override
        public AABB getRenderBoundingBox(BeamReceiverBlockEntity receiver) {
            return renderBox(receiver);
        }
    }
}
