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
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The Moon sky (1.12.2 SkyProviderMoon): 6,000 stars that never fully fade (1.12.2 WorldProviderMoon
 * #getStarBrightness: 0.3 at noon to 0.8 at midnight), the airless "orbital" sun with a black patch behind it so no
 * stars shine through, and the Earth, which stays put in the sky and only shifts as you travel north or south. No
 * clouds, sunrise colours or sky dome.
 */
public final class MoonSkyRenderer {
    private static final ResourceLocation SUN = GalacticraftCore.id("textures/gui/planets/orbitalsun.png");
    private static final ResourceLocation EARTH = GalacticraftCore.id("textures/gui/celestialbodies/earth.png");
    private static final int STAR_COUNT = 6000;

    private VertexBuffer stars;

    /** @return true: the vanilla sky is not drawn */
    public boolean render(ClientLevel level, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection, Runnable setupFog) {
        if (this.stars == null) {
            this.stars = buildStars();
        }
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        final float time = level.getTimeOfDay(partialTick);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        final float brightness = starBrightness(time);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(time * 360.0F));
        pose.mulPose(Axis.YP.rotationDegrees(-19.0F));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, brightness);
        RenderSystem.disableCull();
        FogRenderer.setupNoFog();
        this.stars.bind();
        this.stars.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        setupFog.run();
        RenderSystem.enableCull();
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(time * 360.0F));
        Matrix4f matrix = pose.last().pose();
        // The black disc hides the stars behind the sun
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        float size = 20.0F / 3.5F;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(matrix, -size, 99.9F, -size);
        builder.addVertex(matrix, size, 99.9F, -size);
        builder.addVertex(matrix, size, 99.9F, size);
        builder.addVertex(matrix, -size, 99.9F, size);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        size = 20.0F;
        builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -size, 100.0F, -size).setUv(0.0F, 0.0F);
        builder.addVertex(matrix, size, 100.0F, -size).setUv(1.0F, 0.0F);
        builder.addVertex(matrix, size, 100.0F, size).setUv(1.0F, 1.0F);
        builder.addVertex(matrix, -size, 100.0F, size).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        pose.popPose();

        // Home: the Earth, tilted by how far north or south of spawn the player is
        pose.pushPose();
        RenderSystem.disableBlend();
        final float earthRotation = (float) (level.getSharedSpawnPos().getZ() - camera.getPosition().z) * 0.01F;
        pose.scale(0.6F, 0.6F, 0.6F);
        pose.mulPose(Axis.XP.rotationDegrees(earthRotation));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        matrix = pose.last().pose();
        RenderSystem.setShaderTexture(0, EARTH);
        size = 10.0F;
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

    /** 1.12.2 WorldProviderMoon#getStarBrightness. */
    public static float starBrightness(float timeOfDay) {
        final float value = Mth.clamp(1.0F - (Mth.cos(timeOfDay * Mth.TWO_PI) * 2.0F + 0.25F), 0.0F, 1.0F);
        return value * value * 0.5F + 0.3F;
    }

    /** 1.12.2 SkyProviderMoon#renderStars (without the "more stars" option). */
    private static VertexBuffer buildStars() {
        final Random random = new Random(10842L);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < STAR_COUNT; ++i) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            final double size = 0.15F + random.nextFloat() * 0.1F;
            double length = x * x + y * y + z * z;
            if (length < 1.0D && length > 0.01D) {
                length = 1.0D / Math.sqrt(length);
                x *= length;
                y *= length;
                z *= length;
                final double px = x * 100.0D;
                final double py = y * 100.0D;
                final double pz = z * 100.0D;
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
