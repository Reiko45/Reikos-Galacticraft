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
 * The sky in Earth orbit (1.12.2 SkyProviderOrbit): 6,000 close stars (20,000 with moreStars), the orbital sun and the Moon on the celestial
 * sphere, and the Earth filling the view below, smaller the higher you are. Stars and Earth turn with the station's
 * spin ({@link #spinDegreesPerTick}, set by the Spin Thruster sync).
 */
public final class OrbitSkyRenderer {
    private static final ResourceLocation SUN = GalacticraftCore.id("textures/gui/planets/orbitalsun.png");
    private static final ResourceLocation MOON = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static final ResourceLocation EARTH = GalacticraftCore.id("textures/gui/celestialbodies/earth.png");
    private static final int STAR_COUNT = 6000;

    /** Degrees the sky turns per tick (1.12.2 spinDeltaPerTick) for the station the player is at. */
    public static float spinDegreesPerTick;
    private static float spinAngle;
    private static float prevPartialTick;
    private static long prevGameTime;

    private final StarField stars = new StarField(StarField.ORBIT);

    /** The sky's current spin, for anything that should turn with it. */
    public static float spinAngle() {
        return spinAngle;
    }

    /** @return true: the vanilla sky is not drawn */
    public boolean render(ClientLevel level, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection, Runnable setupFog) {
        advanceSpin(level, partialTick);
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        final float height = 400.0F + (float) camera.getPosition().y / 2.0F;
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(spinAngle));
        // Stars
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(0.8F, 0.8F, 0.8F, 0.8F);
        FogRenderer.setupNoFog();
        final VertexBuffer starBuffer = this.stars.get();
        starBuffer.bind();
        starBuffer.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        setupFog.run();

        // Sun and Moon on the turning celestial sphere, each with a black patch hiding the stars behind it
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f matrix = pose.last().pose();
        blackSquare(matrix, 8.0F, 99.9F);
        texturedSquare(matrix, SUN, 28.0F, 100.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        blackSquare(matrix, 11.3F, -99.9F);
        final int phase = level.getMoonPhase();
        final int u = phase % 4;
        final int v = phase / 4 % 2;
        texturedSquare(matrix, MOON, -40.0F, -100.0F, (u + 1) / 4.0F, (v + 1) / 2.0F, u / 4.0F, v / 2.0F);
        pose.popPose();

        // The Earth below (drawn without blending, so at half brightness as in 1.12.2)
        RenderSystem.disableBlend();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        final float scale = Math.max(100.0F * (0.3F - height / 10000.0F), 0.2F);
        matrix = pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, EARTH);
        RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F);
        final float y = -height / 10.0F;
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -scale, y, scale).setUv(0.0F, 1.0F);
        builder.addVertex(matrix, scale, y, scale).setUv(1.0F, 1.0F);
        builder.addVertex(matrix, scale, y, -scale).setUv(1.0F, 0.0F);
        builder.addVertex(matrix, -scale, y, -scale).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        pose.popPose();
        pose.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        return true;
    }

    /** 1.12.2: spinAngle -= spinDeltaPerTick for each tick (and part tick) since the last frame. */
    private static void advanceSpin(ClientLevel level, float partialTick) {
        float delta = partialTick - prevPartialTick;
        prevPartialTick = partialTick;
        final long now = level.getGameTime();
        final int ticks = (int) (now - prevGameTime);
        prevGameTime = now;
        if (ticks > 0 && ticks < 20) {
            delta += ticks;
        }
        spinAngle -= spinDegreesPerTick * delta;
        while (spinAngle < -180.0F) {
            spinAngle += 360.0F;
        }
        while (spinAngle > 180.0F) {
            spinAngle -= 360.0F;
        }
    }

    private static void blackSquare(Matrix4f matrix, float size, float y) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(matrix, -size, y, -size);
        builder.addVertex(matrix, size, y, -size);
        builder.addVertex(matrix, size, y, size);
        builder.addVertex(matrix, -size, y, size);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /** A square of {@code size} (negative: mirrored, as 1.12.2 drew the Moon from below) at height {@code y}. */
    private static void texturedSquare(Matrix4f matrix, ResourceLocation texture, float size, float y, float u0, float v0, float u1, float v1) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -size, y, -size).setUv(u0, v0);
        builder.addVertex(matrix, size, y, -size).setUv(u1, v0);
        builder.addVertex(matrix, size, y, size).setUv(u1, v1);
        builder.addVertex(matrix, -size, y, size).setUv(u0, v1);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

}
