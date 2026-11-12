/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.SkyProviderAsteroids).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The asteroid belt's sky (1.12.2 SkyProviderAsteroids): black, with 6,000 small grey stars fixed in place, the sun on
 * the horizon (the celestial angle never moves from 0.25), smaller than from Earth (17.5 x 1 / 1.375 AU), and the Earth
 * a speck far away.
 */
public final class AsteroidsSkyRenderer {
    private static final ResourceLocation SUN = GalacticraftCore.id("textures/gui/planets/orbitalsun.png");
    private static final ResourceLocation EARTH = GalacticraftCore.id("textures/gui/celestialbodies/earth.png");
    private static final int STAR_COUNT = 6000;
    /** 1.12.2 17.5 x WorldProviderSpace#getSolarSize (1 / 1.375). */
    private static final float SUN_SIZE = 17.5F / 1.375F;

    private VertexBuffer stars;

    /** @return true: the vanilla sky is not drawn */
    public boolean render(ClientLevel level, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection, Runnable setupFog) {
        if (this.stars == null) {
            this.stars = buildStars();
        }
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 0.7F);
        RenderSystem.disableCull();
        FogRenderer.setupNoFog();
        this.stars.bind();
        this.stars.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        setupFog.run();
        RenderSystem.enableCull();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f matrix = pose.last().pose();
        // The black square keeps the stars from shining through the sun
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        float size = SUN_SIZE / 4.2F;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(matrix, -size, 90.0F, -size);
        builder.addVertex(matrix, size, 90.0F, -size);
        builder.addVertex(matrix, size, 90.0F, size);
        builder.addVertex(matrix, -size, 90.0F, size);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        size = SUN_SIZE / 1.2F;
        builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -size, 90.0F, -size).setUv(0.0F, 0.0F);
        builder.addVertex(matrix, size, 90.0F, -size).setUv(1.0F, 0.0F);
        builder.addVertex(matrix, size, 90.0F, size).setUv(1.0F, 1.0F);
        builder.addVertex(matrix, -size, 90.0F, size).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        pose.popPose();

        // Home, far away
        pose.pushPose();
        pose.scale(0.6F, 0.6F, 0.6F);
        pose.mulPose(Axis.ZP.rotationDegrees(40.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        matrix = pose.last().pose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, EARTH);
        size = 0.5F;
        builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -size, -100.0F, size).setUv(0.0F, 1.0F);
        builder.addVertex(matrix, size, -100.0F, size).setUv(1.0F, 1.0F);
        builder.addVertex(matrix, size, -100.0F, -size).setUv(1.0F, 0.0F);
        builder.addVertex(matrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        pose.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    /** 1.12.2 SkyProviderAsteroids#renderStars: smaller stars than the Moon's, 80 blocks out. */
    private static VertexBuffer buildStars() {
        final Random random = new Random(10842L);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < STAR_COUNT; ++i) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            final double size = 0.08F + random.nextFloat() * 0.07F;
            double length = x * x + y * y + z * z;
            if (length < 1.0D && length > 0.01D) {
                length = 1.0D / Math.sqrt(length);
                x *= length;
                y *= length;
                z *= length;
                final double px = x * 80.0D;
                final double py = y * 80.0D;
                final double pz = z * 80.0D;
                final double yaw = Math.atan2(x, z);
                final double sinYaw = Math.sin(yaw);
                final double cosYaw = Math.cos(yaw);
                final double pitch = Math.atan2(Math.sqrt(x * x + z * z), y);
                final double sinPitch = Math.sin(pitch);
                final double cosPitch = Math.cos(pitch);
                final double spin = random.nextDouble() * Math.PI * 2.0D;
                final double sinSpin = Math.sin(spin);
                final double cosSpin = Math.cos(spin);
                for (int corner = 0; corner < 4; ++corner) {
                    final double a = ((corner & 2) - 1) * size;
                    final double b = ((corner + 1 & 2) - 1) * size;
                    final double c = a * cosSpin - b * sinSpin;
                    final double d = b * cosSpin + a * sinSpin;
                    final double e = c * sinPitch;
                    final double f = -c * cosPitch;
                    final double dx = f * sinYaw - d * cosYaw;
                    final double dz = d * sinYaw + f * cosYaw;
                    builder.addVertex((float) (px + dx), (float) (py + e), (float) (pz + dz));
                }
            }
        }
        final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }
}
