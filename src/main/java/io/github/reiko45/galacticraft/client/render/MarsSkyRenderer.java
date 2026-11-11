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
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Mars sky (1.12.2 SkyProviderMars and WorldProviderMars): a butterscotch sky that only dims to a quarter at night,
 * when up to three quarters of the stars come out; a smaller sun (17.5 x 1/1.25 AU) inside a pale pink haze; and the
 * Earth as a faint dot. No clouds.
 */
public final class MarsSkyRenderer {
    private static final ResourceLocation SUN = GalacticraftCore.id("textures/gui/planets/atmosphericsun.png");
    private static final ResourceLocation EARTH = GalacticraftCore.id("textures/gui/celestialbodies/earth.png");
    /** SkyProviderMars: 17.5 x WorldProviderSpace#getSolarSize (1 / 1.25). */
    private static final float SUN_SIZE = 17.5F / 1.25F;
    /** The haze around the sun: 1.12.2 colour (255, 194, 180), alpha 0.3. */
    private static final float AURA_R = 1.0F;
    private static final float AURA_G = 194.0F / 255.0F;
    private static final float AURA_B = 180.0F / 255.0F;
    private static final float AURA_A = 0.3F;

    private VertexBuffer stars;
    private VertexBuffer skyDisc;

    /** 1.12.2 WorldProviderMars#getStarBrightness: 0 by day, 0.75 at midnight. */
    public static float starBrightness(float timeOfDay) {
        final float value = Mth.clamp(1.0F - (Mth.cos(timeOfDay * Mth.TWO_PI) * 2.0F + 0.25F), 0.0F, 1.0F);
        return value * value * 0.75F;
    }

    /** 1.12.2 WorldProviderMars#getSkyColor: (154, 114, 66), dimmed by the star brightness. */
    public static Vec3 skyColor(float timeOfDay) {
        final float f = 1.0F - starBrightness(timeOfDay);
        return new Vec3(154.0F / 255.0F * f, 114.0F / 255.0F * f, 66.0F / 255.0F * f);
    }

    /** 1.12.2 WorldProviderMars#getFogColor: (210, 120, 59), dimmed the same way. */
    public static Vec3 fogColor(float timeOfDay) {
        final float f = 1.0F - starBrightness(timeOfDay);
        return new Vec3(210.0F / 255.0F * f, 120.0F / 255.0F * f, 59.0F / 255.0F * f);
    }

    /** @return true: the vanilla sky is not drawn */
    public boolean render(ClientLevel level, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection, Runnable setupFog) {
        if (this.stars == null) {
            this.stars = MoonSkyRenderer.buildStars();
            this.skyDisc = buildSkyDisc();
        }
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        final float time = level.getTimeOfDay(partialTick);

        // The sky dome, fogged like vanilla's, in the 1.12.2 Mars sky colour (ClientLevelMixin)
        final Vec3 sky = level.getSkyColor(camera.getPosition(), partialTick);
        setupFog.run();
        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor((float) sky.x, (float) sky.y, (float) sky.z, 1.0F);
        this.skyDisc.bind();
        this.skyDisc.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
        VertexBuffer.unbind();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        final float stars = starBrightness(time);
        if (stars > 0.0F) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(time * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(-19.0F));
            RenderSystem.setShaderColor(stars, stars, stars, stars);
            FogRenderer.setupNoFog();
            this.stars.bind();
            this.stars.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            setupFog.run();
            pose.popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(time * 360.0F));
        Matrix4f matrix = pose.last().pose();
        // Two soft fans of haze around the sun, the inner one stronger (its alpha grows as the day fades, like 1.12.2)
        final float day = 1.0F - stars;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        aura(matrix, 20.0F, Math.min(1.0F, AURA_A * 2.0F / day), day);
        aura(matrix, 40.0F, AURA_A * day, day);

        // 1.12.2 drew a black square behind the sun here, but with additive blending, so it hid nothing; it is left out.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        float size = SUN_SIZE;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -size, 100.0F, -size).setUv(0.0F, 0.0F);
        builder.addVertex(matrix, size, 100.0F, -size).setUv(1.0F, 0.0F);
        builder.addVertex(matrix, size, 100.0F, size).setUv(1.0F, 1.0F);
        builder.addVertex(matrix, -size, 100.0F, size).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        // The Earth: a tiny bright dot, fixed among the stars
        pose.scale(0.6F, 0.6F, 0.6F);
        pose.mulPose(Axis.ZP.rotationDegrees(40.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        matrix = pose.last().pose();
        RenderSystem.setShaderTexture(0, EARTH);
        size = 0.5F;
        builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -size, -100.0F, size).setUv(0.0F, 1.0F);
        builder.addVertex(matrix, size, -100.0F, size).setUv(1.0F, 1.0F);
        builder.addVertex(matrix, size, -100.0F, -size).setUv(1.0F, 0.0F);
        builder.addVertex(matrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        pose.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    /** One 1.12.2 haze fan: the centre in the haze colour, fading to transparent at an eight-pointed rim. */
    private static void aura(Matrix4f matrix, float radius, float centreAlpha, float day) {
        final float r = AURA_R * day;
        final float g = AURA_G * day;
        final float b = AURA_B * day;
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, 0.0F, 100.0F, 0.0F).setColor(r, g, b, centreAlpha);
        final float wide = radius * 1.5F;
        final float[][] rim = {{-radius, -radius}, {0, -wide}, {radius, -radius}, {wide, 0}, {radius, radius}, {0, wide},
                {-radius, radius}, {-wide, 0}, {-radius, -radius}};
        for (float[] point : rim) {
            builder.addVertex(matrix, point[0], 100.0F, point[1]).setColor(r, g, b, 0.0F);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /** Vanilla's sky disc at y=16 (1.12.2 drew a grid of quads at the same height). */
    private static VertexBuffer buildSkyDisc() {
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        final float y = 16.0F;
        builder.addVertex(0.0F, y, 0.0F);
        for (int angle = -180; angle <= 180; angle += 45) {
            builder.addVertex(512.0F * Mth.cos(angle * Mth.DEG_TO_RAD), y, 512.0F * Mth.sin(angle * Mth.DEG_TO_RAD));
        }
        final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }

    /** Whether a level is Mars (for the sky colour hook). */
    public static boolean isMars(ClientLevel level) {
        return CelestialPhysics.of(level.dimension()) == CelestialPhysics.MARS;
    }
}
