/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.render.tile.TileEntityLaserTurretRenderer).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 TileEntityLaserTurretRenderer, on the block above the base: the base connectors, the phalange turning and half
 * tipping with the aim, the axle and barrels (lit while it has a target) tipping the rest of the way; while it charges,
 * a green sight triangle and the orb growing and spinning at the muzzle; for a few ticks after a shot, the green beam to
 * the target.
 */
public class LaserTurretRenderer implements BlockEntityRenderer<LaserTurretBlockEntity> {
    public static final ModelResourceLocation BASE = BakedModels.location("entity/laser_turret/base");
    public static final ModelResourceLocation PHALANGE = BakedModels.location("entity/laser_turret/phalange");
    public static final ModelResourceLocation AXLE = BakedModels.location("entity/laser_turret/axle");
    public static final ModelResourceLocation TURRETS = BakedModels.location("entity/laser_turret/turrets");
    public static final ModelResourceLocation TURRETS_OFF = BakedModels.location("entity/laser_turret/turrets_off");
    public static final ModelResourceLocation ORB_INNER = BakedModels.location("entity/laser_turret/orb_inner");
    public static final ModelResourceLocation ORB_OUTER = BakedModels.location("entity/laser_turret/orb_outer");

    public LaserTurretRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : new ModelResourceLocation[]{BASE, PHALANGE, AXLE, TURRETS, TURRETS_OFF, ORB_INNER, ORB_OUTER}) {
            event.register(model);
        }
    }

    @Override
    public void render(LaserTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final float yaw = Mth.rotLerp(partialTick, turret.prevYaw, turret.yaw);
        final float pitch = Mth.lerp(partialTick, turret.prevPitch, turret.pitch);
        final VertexConsumer solid = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.pushPose();
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        poseStack.pushPose();
        poseStack.scale(0.9F, 1.0F, 0.9F);
        BakedModels.draw(poseStack, solid, BASE, light);
        poseStack.popPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        // the phalange takes part of the tilt, the barrels the rest
        final float partialRot = Math.signum(pitch) * pitch * (pitch / 120.0F);
        poseStack.mulPose(Axis.ZN.rotationDegrees(partialRot));
        poseStack.pushPose();
        poseStack.scale(1.1F, 1.0F, 1.0F);
        BakedModels.draw(poseStack, solid, PHALANGE, light);
        poseStack.popPose();
        poseStack.mulPose(Axis.ZN.rotationDegrees(pitch - partialRot));
        BakedModels.draw(poseStack, solid, AXLE, light);
        BakedModels.draw(poseStack, solid, turret.active() ? TURRETS : TURRETS_OFF, light);
        poseStack.popPose();

        final int charge = turret.chargeLevel();
        if (charge > 0) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.ZN.rotationDegrees(pitch));
            poseStack.translate(-0.6F, 0.28F, 0.0F);
            final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            final PoseStack.Pose pose = poseStack.last();
            final float[][] triangle = {{0.09F, 0.0F, 0.275F}, {0.0F, 0.0F, 0.0F}, {0.09F, 0.0F, -0.275F}};
            for (int i = 0; i < 3; i++) {
                final float[] a = triangle[i];
                final float[] b = triangle[(i + 1) % 3];
                final float nx = b[0] - a[0];
                final float nz = b[2] - a[2];
                final float length = Mth.sqrt(nx * nx + nz * nz);
                lines.addVertex(pose, a[0], a[1], a[2]).setColor(0, 255, 0, 255).setNormal(pose, nx / length, 0.0F, nz / length);
                lines.addVertex(pose, b[0], b[1], b[2]).setColor(0, 255, 0, 255).setNormal(pose, nx / length, 0.0F, nz / length);
            }
            final float spin = (float) Math.pow(charge / 5.0F + 1.0F, 2.5F);
            final float spinNext = (float) Math.pow((charge + 1) / 5.0F + 1.0F, 2.5F);
            final float rotate = spin + (spinNext - spin) * partialTick;
            final float scale = 0.01F + charge / 600.0F;
            poseStack.scale(scale, scale, scale);
            final VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotate));
            BakedModels.draw(poseStack, glow, ORB_INNER, LightTexture.FULL_BRIGHT);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotate));
            BakedModels.draw(poseStack, glow, ORB_OUTER, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        if (turret.timeSinceShot > 0 && turret.timeSinceShot < 5 && turret.getLevel() != null) {
            final Entity target = turret.getLevel().getEntity(turret.targetedEntity());
            if (target != null) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.ZN.rotationDegrees(pitch));
                poseStack.translate(-0.6F, 0.28F, 0.0F);
                final Vec3 to = target.getPosition(partialTick).add(0.0D, target.getEyeHeight(), 0.0D).subtract(turret.barrels());
                final float dist = (float) to.length() - 0.8F;
                final float shot = (float) (Math.pow((5.0F - turret.timeSinceShot) / 5.0F + 1.0F, 2.5F) * 0.5F);
                final float shotNext = (float) (Math.pow((5.0F - (turret.timeSinceShot + 1)) / 5.0F + 1.0F, 2.5F) * 0.5F);
                final float fade = shot + (shotNext - shot) * partialTick;
                final VertexConsumer beam = buffers.getBuffer(RenderType.debugQuads());
                final PoseStack.Pose pose = poseStack.last();
                for (float size : new float[]{0.101F, 0.15F}) {
                    final float max = size * fade;
                    final float min = -max;
                    quad(beam, pose, dist, max, max, min, min);
                    quad(beam, pose, dist, max, min, min, max);
                }
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    /** One of 1.12.2's beam quads: a narrowing strip from the muzzle out to the target, half see-through green. */
    private static void quad(VertexConsumer beam, PoseStack.Pose pose, float dist, float y0, float z0, float y1, float z1) {
        beam.addVertex(pose, 0.0F, y0, z0).setColor(0.0F, 1.0F, 0.0F, 0.5F);
        beam.addVertex(pose, 0.0F, y1, z1).setColor(0.0F, 1.0F, 0.0F, 0.5F);
        beam.addVertex(pose, -dist, y1 * 0.4F, z1 * 0.4F).setColor(0.0F, 1.0F, 0.0F, 0.5F);
        beam.addVertex(pose, -dist, y0 * 0.4F, z0 * 0.4F).setColor(0.0F, 1.0F, 0.0F, 0.5F);
    }

    @Override
    public AABB getRenderBoundingBox(LaserTurretBlockEntity turret) {
        // the beam reaches out to 15 blocks (90 for a meteor), so the box is widened while one is drawn
        return turret.timeSinceShot > 0 && turret.timeSinceShot < 5 ? turret.renderBox().inflate(LaserTurretBlockEntity.METEOR_RANGE) : turret.renderBox();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
