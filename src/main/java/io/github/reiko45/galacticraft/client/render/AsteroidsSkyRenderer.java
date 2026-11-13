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
 * The asteroid belt's sky (1.12.2 SkyProviderAsteroids): black, with 6,000 small grey stars (35,000 with moreStars) fixed in place, the sun on
 * the horizon (the celestial angle never moves from 0.25), smaller than from Earth (17.5 x 1 / 1.375 AU), and the Earth
 * a speck far away.
 */
public final class AsteroidsSkyRenderer {
    private static final ResourceLocation SUN = GalacticraftCore.id("textures/gui/planets/orbitalsun.png");
    private static final ResourceLocation EARTH = GalacticraftCore.id("textures/gui/celestialbodies/earth.png");
    private static final int STAR_COUNT = 6000;
    /** 1.12.2 17.5 x WorldProviderSpace#getSolarSize (1 / 1.375). */
    private static final float SUN_SIZE = 17.5F / 1.375F;

    private final StarField stars = new StarField(StarField.ASTEROIDS);

    /** @return true: the vanilla sky is not drawn */
    public boolean render(ClientLevel level, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection, Runnable setupFog) {
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 0.7F);
        RenderSystem.disableCull();
        FogRenderer.setupNoFog();
        final VertexBuffer starBuffer = this.stars.get();
        starBuffer.bind();
        starBuffer.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
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

}
