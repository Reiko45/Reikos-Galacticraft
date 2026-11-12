/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.entity.RenderGrapple).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.GrappleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 RenderGrapple: the grapple OBJ, turned along its flight and spinning (5 degrees a tick) while it flies, and
 * while it pulls, a pale rope sagging back to the shooter's feet.
 */
public class GrappleRenderer extends EntityRenderer<GrappleEntity> {
    public static final ModelResourceLocation MODEL = BakedModels.location("entity/grapple");

    public GrappleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(GrappleEntity grapple, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        final Player shooter = grapple.shooter();
        if (shooter != null && grapple.pulling()) {
            renderRope(grapple, shooter, partialTick, poseStack, buffers);
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, grapple.yRotO, grapple.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, grapple.xRotO, grapple.getXRot()) - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(grapple.roll(partialTick)));
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), MODEL, light);
        poseStack.popPose();
        super.render(grapple, entityYaw, partialTick, poseStack, buffers, light);
    }

    /** 1.12.2: 16 segments, y bending as (f^2 + f) / 2, in (203, 203, 192). */
    private static void renderRope(GrappleEntity grapple, Player shooter, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        final Vec3 end = shooter.getPosition(partialTick).add(0.0D, -0.2D, 0.0D);
        final Vec3 start = grapple.getPosition(partialTick).add(0.0D, 0.25D, 0.0D);
        final float dx = (float) (end.x - start.x);
        final float dy = (float) (end.y - start.y);
        final float dz = (float) (end.z - start.z);
        final VertexConsumer consumer = buffers.getBuffer(RenderType.lineStrip());
        final PoseStack.Pose pose = poseStack.last();
        final int segments = 16;
        for (int i = 0; i <= segments; i++) {
            final float f = (float) i / segments;
            final float x = dx * f;
            final float y = dy * (f * f + f) * 0.5F + 0.15F - 0.2F;
            final float z = dz * f;
            final float next = (float) Math.min(i + 1, segments) / segments;
            float nx = dx * next - x;
            float ny = dy * (next * next + next) * 0.5F + 0.15F - 0.2F - y;
            float nz = dz * next - z;
            final float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 1.0E-4F) {
                nx /= length;
                ny /= length;
                nz /= length;
            } else {
                ny = 1.0F;
            }
            consumer.addVertex(pose, x, y, z).setColor(203, 203, 192, 255).setNormal(pose, nx, ny, nz);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GrappleEntity grapple) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
