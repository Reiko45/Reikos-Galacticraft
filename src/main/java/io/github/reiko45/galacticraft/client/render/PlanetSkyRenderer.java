/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.SkyProviderMars,
 * planets.venus.client.SkyProviderVenus and their world providers' sky and fog colours). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
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
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The skies of the planets with air (1.12.2 SkyProviderMars, SkyProviderVenus): a sky dome in the planet's colour that
 * shifts towards its night colour as up to three quarters of the stars come out (1.12.2 getStarBrightness), and two
 * soft fans of haze around the sun. No clouds.
 *
 * <ul>
 *   <li>Mars: a butterscotch sky that only dims to a quarter at night, a smaller sun (17.5 x 1/1.25 AU) inside a pale
 *   pink haze, and the Earth as a faint dot.</li>
 *   <li>Venus: a sulphur-yellow sky turning dull brown at night; the clouds hide the sun, leaving only its haze, smaller
 *   and paler at the rim; and the sky turns the other way (Venus spins backwards).</li>
 * </ul>
 */
public final class PlanetSkyRenderer {
    private static final ResourceLocation SUN = GalacticraftCore.id("textures/gui/planets/atmosphericsun.png");
    private static final ResourceLocation EARTH = GalacticraftCore.id("textures/gui/celestialbodies/earth.png");
    /** The haze around the sun: 1.12.2 colour (255, 194, 180), alpha 0.3, on both planets. */
    private static final float AURA_R = 1.0F;
    private static final float AURA_G = 194.0F / 255.0F;
    private static final float AURA_B = 180.0F / 255.0F;
    private static final float AURA_A = 0.3F;

    /**
     * One planet's sky: its day and night sky and fog colours (0-255), the sun's size (0 for none), whether the Earth
     * shows, which way the sky turns, the outer haze's radius and how blue the inner haze's rim is.
     */
    public record Style(int[] daySky, int[] nightSky, int[] dayFog, int[] nightFog, float sunSize, boolean earth, float turn,
                        float outerAura, float innerRimBlue) {
        /** 1.12.2 getStarBrightness (WorldProviderMars, WorldProviderVenus): 0 by day, 0.75 at midnight. */
        public float starBrightness(float timeOfDay) {
            final float value = Mth.clamp(1.0F - (Mth.cos(timeOfDay * Mth.TWO_PI) * 2.0F + 0.25F), 0.0F, 1.0F);
            return value * value * 0.75F;
        }

        public Vec3 skyColor(float timeOfDay) {
            return blend(this.daySky, this.nightSky, starBrightness(timeOfDay));
        }

        public Vec3 fogColor(float timeOfDay) {
            return blend(this.dayFog, this.nightFog, starBrightness(timeOfDay));
        }

        private static Vec3 blend(int[] day, int[] night, float nightness) {
            final float dayness = 1.0F - nightness;
            return new Vec3((day[0] * dayness + night[0] * nightness) / 255.0F, (day[1] * dayness + night[1] * nightness) / 255.0F,
                    (day[2] * dayness + night[2] * nightness) / 255.0F);
        }
    }

    /** WorldProviderMars sky (154, 114, 66) and fog (210, 120, 59), both just dimmed at night. */
    public static final Style MARS = new Style(new int[]{154, 114, 66}, new int[]{0, 0, 0}, new int[]{210, 120, 59}, new int[]{0, 0, 0},
            17.5F / 1.25F, true, 1.0F, 40.0F, 1.0F);
    /**
     * WorldProviderVenus sky (255, 207, 81) by day, (118, 89, 21) by night; fog (203, 147, 0) and (131, 108, 46); no sun
     * drawn and the inner haze's rim a twentieth as blue (SkyProviderVenus).
     */
    public static final Style VENUS = new Style(new int[]{255, 207, 81}, new int[]{118, 89, 21}, new int[]{203, 147, 0}, new int[]{131, 108, 46},
            0.0F, false, -1.0F, 20.0F, 1.0F / 20.0F);

    private final Style style;
    private final StarField stars = new StarField(StarField.PLANET);
    private VertexBuffer skyDisc;

    public PlanetSkyRenderer(Style style) {
        this.style = style;
    }

    /** The sky style of a level, or null if it has none of its own. */
    @Nullable
    public static Style styleOf(ClientLevel level) {
        final CelestialPhysics physics = CelestialPhysics.of(level.dimension());
        if (physics == CelestialPhysics.MARS) {
            return MARS;
        }
        return physics == CelestialPhysics.VENUS ? VENUS : null;
    }

    /** @return true: the vanilla sky is not drawn */
    public boolean render(ClientLevel level, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection, Runnable setupFog) {
        if (this.skyDisc == null) {
            this.skyDisc = buildSkyDisc();
        }
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        final float time = level.getTimeOfDay(partialTick);
        final float turn = this.style.turn() * time * 360.0F;

        // The sky dome, fogged like vanilla's, in the planet's sky colour (ClientLevelMixin)
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
        final float stars = this.style.starBrightness(time);
        if (stars > 0.0F) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(turn));
            pose.mulPose(Axis.YP.rotationDegrees(-19.0F));
            RenderSystem.setShaderColor(stars, stars, stars, stars);
            FogRenderer.setupNoFog();
            final VertexBuffer starBuffer = this.stars.get();
            starBuffer.bind();
            starBuffer.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            setupFog.run();
            pose.popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(turn));
        Matrix4f matrix = pose.last().pose();
        // Two soft fans of haze around the sun, the inner one stronger (its alpha grows as the day fades, like 1.12.2)
        final float day = 1.0F - stars;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        aura(matrix, 20.0F, Math.min(1.0F, AURA_A * 2.0F / day), day, this.style.innerRimBlue());
        aura(matrix, this.style.outerAura(), AURA_A * day, day, 1.0F);

        // 1.12.2 drew a black square behind the sun here, but with additive blending, so it hid nothing; it is left out.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        if (this.style.sunSize() > 0.0F) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, SUN);
            final float size = this.style.sunSize();
            final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(matrix, -size, 100.0F, -size).setUv(0.0F, 0.0F);
            builder.addVertex(matrix, size, 100.0F, -size).setUv(1.0F, 0.0F);
            builder.addVertex(matrix, size, 100.0F, size).setUv(1.0F, 1.0F);
            builder.addVertex(matrix, -size, 100.0F, size).setUv(0.0F, 1.0F);
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }

        if (this.style.earth()) {
            // The Earth: a tiny bright dot, fixed among the stars
            pose.scale(0.6F, 0.6F, 0.6F);
            pose.mulPose(Axis.ZP.rotationDegrees(40.0F));
            pose.mulPose(Axis.XP.rotationDegrees(200.0F));
            matrix = pose.last().pose();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, EARTH);
            final float size = 0.5F;
            final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(matrix, -size, -100.0F, size).setUv(0.0F, 1.0F);
            builder.addVertex(matrix, size, -100.0F, size).setUv(1.0F, 1.0F);
            builder.addVertex(matrix, size, -100.0F, -size).setUv(1.0F, 0.0F);
            builder.addVertex(matrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }
        pose.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    /**
     * One 1.12.2 haze fan: the centre in the haze colour, fading to transparent at an eight-pointed rim (whose blue
     * SkyProviderVenus cut for its inner fan).
     */
    private static void aura(Matrix4f matrix, float radius, float centreAlpha, float day, float rimBlue) {
        final float r = AURA_R * day;
        final float g = AURA_G * day;
        final float b = AURA_B * day;
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, 0.0F, 100.0F, 0.0F).setColor(r, g, b, centreAlpha);
        final float wide = radius * 1.5F;
        final float[][] rim = {{-radius, -radius}, {0, -wide}, {radius, -radius}, {wide, 0}, {radius, radius}, {0, wide},
                {-radius, radius}, {-wide, 0}, {-radius, -radius}};
        for (float[] point : rim) {
            builder.addVertex(matrix, point[0], 100.0F, point[1]).setColor(r, g, b * rimBlue, 0.0F);
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
}
