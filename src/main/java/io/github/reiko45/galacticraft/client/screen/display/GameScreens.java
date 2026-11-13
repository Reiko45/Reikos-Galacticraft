/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.screen.GameScreenBasic,
 * GameScreenCelestial and client.render.RenderPlanet). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen.display;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.DisplayScreenBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * The Display Screen's pictures (1.12.2 IGameScreen types), drawn in the screen's own plane: x across, y down, from
 * (0, 0) to (sizeX, sizeY), inside a frame of {@link #FRAME}; nearer the viewer is towards -z.
 * <ul>
 * <li>0 (GameScreenBasic): blank, a very dark grey.</li>
 * <li>1 (GameScreenBasic): the Earth. 1.12.2 drew a map of the Overworld around the screen when it had made one, and
 * the Earth's icon until then; the map (1.12.2 MapUtil) isn't ported, so it is always the icon.</li>
 * <li>2 (GameScreenCelestial): the solar system of the world the screen is in, the planets going round their orbits.</li>
 * <li>3: the Earth with the Moon going round it.</li>
 * <li>4: 1.12.2's planet render test: Europa, Ganymede, Io, Jupiter and Saturn in turn, ten seconds each, turning.</li>
 * <li>5 ({@link GameScreenText}): what the nearest Telemetry Unit sees, or the time.</li>
 * </ul>
 * 1.12.2 cut the pictures off at the frame with clip planes; here bodies outside the frame are left out (as 1.12.2 did
 * too) and orbit lines are drawn only where they are inside it.
 */
public final class GameScreens {
    /** 1.12.2 TileEntityScreen.FRAMEBORDER. */
    public static final float FRAME = 0.098F;
    private static final int SEGMENTS = 90;

    private GameScreens() {
    }

    private static ResourceLocation icon(String name) {
        return GalacticraftCore.id("textures/gui/celestialbodies/" + name + ".png");
    }

    /** A body for types 2 and 3, with 1.12.2's unscaled distance, orbit time and phase. */
    private record Body(String icon, float distance, float orbitTime, float phase, boolean planet) {
    }

    /** 1.12.2 GalacticraftCore, MarsModule, AsteroidsModule, VenusModule: the bodies of Sol this port has. */
    private static final Body EARTH = new Body("earth", 1.0F, 1.0F, 0.0F, true);
    private static final Body VENUS = new Body("venus", 0.75F, 0.61527929901423877327491785323111F, 2.0F, true);
    private static final Body MARS = new Body("mars", 1.25F, 1.8811610076670317634173055859803F, 0.1667F, true);
    private static final Body ASTEROIDS = new Body("asteroid", 1.375F, 45.0F, 4.0F, true);
    private static final Body MOON = new Body("moon", 13.0F, 100.0F, 0.0F, false);
    private static final Body[] PLANETS = {EARTH, VENUS, MARS, ASTEROIDS};
    private static final ResourceLocation[] TEST_PLANETS = {GalacticraftCore.id("textures/misc/planets/europa.png"),
            GalacticraftCore.id("textures/misc/planets/ganymede.png"), GalacticraftCore.id("textures/misc/planets/io.png"),
            GalacticraftCore.id("textures/misc/planets/jupiter_inner.png"), GalacticraftCore.id("textures/misc/planets/saturn.png")};
    private static final ResourceLocation JUPITER_UPPER = GalacticraftCore.id("textures/misc/planets/jupiter_upper.png");

    public static void render(DisplayScreenBlockEntity driver, int type, float ticks, float sizeX, float sizeY, PoseStack poseStack, MultiBufferSource buffers,
                              float partialTick) {
        final float frameBx = sizeX - FRAME;
        final float frameBy = sizeY - FRAME;
        switch (type) {
            case 0 -> background(poseStack, buffers, FRAME, FRAME, frameBx, frameBy, 0.09F);
            case 1 -> earth(poseStack, buffers, sizeX, sizeY, frameBx, frameBy);
            case 2, 3, 4 -> {
                background(poseStack, buffers, FRAME, FRAME, frameBx, frameBy, 0.0F);
                final Celestial celestial = new Celestial(poseStack, buffers, sizeX, sizeY, frameBx, frameBy);
                if (type == 2) {
                    celestial.solarSystem(driver.getLevel() == null ? Level.OVERWORLD : driver.getLevel().dimension(), ticks);
                } else if (type == 3) {
                    celestial.earthAndMoon(ticks);
                } else {
                    celestial.planetTest(ticks);
                }
            }
            default -> GameScreenText.render(driver, ticks, sizeX, sizeY, poseStack, buffers, partialTick);
        }
    }

    /** 1.12.2 drawBlackBackground: a grey quad just behind the picture. */
    static void background(PoseStack poseStack, MultiBufferSource buffers, float x0, float y0, float x1, float y1, float grey) {
        final VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        final Matrix4f pose = poseStack.last().pose();
        consumer.addVertex(pose, x0, y1, 0.005F).setColor(grey, grey, grey, 1.0F);
        consumer.addVertex(pose, x1, y1, 0.005F).setColor(grey, grey, grey, 1.0F);
        consumer.addVertex(pose, x1, y0, 0.005F).setColor(grey, grey, grey, 1.0F);
        consumer.addVertex(pose, x0, y0, 0.005F).setColor(grey, grey, grey, 1.0F);
    }

    /** A textured quad, full bright, (u0, v0) at the top left. */
    static void texture(PoseStack poseStack, MultiBufferSource buffers, ResourceLocation texture, float x0, float y0, float x1, float y1, float z,
                        float u0, float v0, float u1, float v1) {
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        final PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, x0, y1, z, u0, v1);
        vertex(consumer, pose, x1, y1, z, u1, v1);
        vertex(consumer, pose, x1, y0, z, u1, v0);
        vertex(consumer, pose, x0, y0, z, u0, v0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    /** 1.12.2 GameScreenBasic type 1 without a map: the Earth's icon, cut to the screen's shape. */
    private static void earth(PoseStack poseStack, MultiBufferSource buffers, float sizeX, float sizeY, float frameBx, float frameBy) {
        float u0 = 0.0F;
        float v0 = 0.0F;
        float u1 = 1.0F;
        float v1 = 1.0F;
        if (sizeX < sizeY) {
            u0 = (1.0F - sizeX / sizeY) / 2.0F;
            u1 = 1.0F - u0;
        } else if (sizeY < sizeX) {
            v0 = (1.0F - sizeY / sizeX) / 2.0F;
            v1 = 1.0F - v0;
        }
        texture(poseStack, buffers, icon("earth"), FRAME, FRAME, frameBx, frameBy, 0.0F, u0, v0, u1, v1);
    }

    /** 1.12.2 GameScreenCelestial. */
    private static final class Celestial {
        private final PoseStack poseStack;
        private final MultiBufferSource buffers;
        private final float centreX;
        private final float centreY;
        private final float frameBx;
        private final float frameBy;
        private final float scale;

        Celestial(PoseStack poseStack, MultiBufferSource buffers, float sizeX, float sizeY, float frameBx, float frameBy) {
            this.poseStack = poseStack;
            this.buffers = buffers;
            this.centreX = sizeX / 2;
            this.centreY = sizeY / 2;
            this.frameBx = frameBx;
            this.frameBy = frameBy;
            this.scale = Math.max(sizeX, sizeY) - 0.2F;
        }

        /**
         * Type 2 (drawCelestialBodies): the Sun in the middle, the planets round it. 1.12.2 chose the solar system of the
         * screen's world; every world here is in Sol.
         */
        void solarSystem(ResourceKey<Level> dimension, float ticks) {
            body(icon("sun"), 0.0F, 0.0F, 6.0F);
            for (Body planet : PLANETS) {
                final float[] pos = position(planet, ticks);
                circle(planet);
                body(icon(planet.icon()), pos[0], pos[1], planet.distance() < 1.5F ? 2.0F : 2.8F);
            }
        }

        /** Type 3 (drawCelestialBodiesZ): the Earth and the Moon (1.12.2 also showed the stations it knew of). */
        void earthAndMoon(float ticks) {
            body(icon("earth"), 0.0F, 0.0F, 11.0F);
            final float[] pos = position(MOON, ticks);
            circle(MOON);
            body(icon(MOON.icon()), pos[0], pos[1], 4.0F);
        }

        /** Type 4 (drawPlanetsTest, RenderPlanet.renderID). */
        void planetTest(float ticks) {
            this.poseStack.pushPose();
            this.poseStack.translate(this.centreX, this.centreY, 0.0F);
            final int id = (int) (ticks / 600.0F) % 5;
            if (id == 3) {
                float size = 48.0F / 70.0F * this.scale;
                turning(TEST_PLANETS[3], -size / 2, -size / 2, size, size, ticks, 0.0F);
                size *= 1.001F;
                turning(JUPITER_UPPER, -size / 2, -size / 2, size, size, ticks * 0.85F, -0.001F);
            } else {
                final float size = 8.0F / 70.0F * this.scale;
                turning(TEST_PLANETS[id], -size / 2, -size / 2, size, size, System.nanoTime() / 50_000_000.0F, 0.0F);
            }
            this.poseStack.popPose();
        }

        private void body(ResourceLocation texture, float x, float y, float relSize) {
            if (x + this.centreX > this.frameBx || x + this.centreX < FRAME || y + this.centreY > this.frameBy || y + this.centreY < FRAME) {
                return;
            }
            final float size = relSize / 70.0F * this.scale;
            final float cx = x + this.centreX;
            final float cy = y + this.centreY;
            texture(this.poseStack, this.buffers, texture, cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        }

        /** 1.12.2 getScale. */
        private static float distance(Body body) {
            float distance = body.distance();
            if (distance >= 1.375F) {
                distance = distance >= 1.5F ? distance * 1.15F : distance + 0.075F;
            }
            return 1.0F / 140.0F * distance * (body.planet() ? 25.0F : 3.5F);
        }

        private float[] position(Body body, float ticks) {
            final float timeScale = body.planet() ? 200.0F : 2.0F;
            final float r = distance(body) * this.scale;
            final double angle = ticks / (timeScale * body.orbitTime()) + body.phase();
            return new float[] {(float) Math.sin(angle) * r, (float) Math.cos(angle) * r};
        }

        /** 1.12.2 drawCircle: the orbit, grey (lighter close in), as a thin line of quads. */
        private void circle(Body body) {
            final float r = distance(body) * this.scale;
            final float grey = 0.1F + 0.65F * Math.max(0.0F, 0.5F - distance(body));
            final float half = 0.004F;
            final VertexConsumer consumer = this.buffers.getBuffer(RenderType.debugQuads());
            final Matrix4f pose = this.poseStack.last().pose();
            for (int i = 0; i < SEGMENTS; i++) {
                final double a0 = Mth.TWO_PI * i / SEGMENTS;
                final double a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
                final float x0 = this.centreX + (float) Math.cos(a0) * r;
                final float y0 = this.centreY + (float) Math.sin(a0) * r;
                final float x1 = this.centreX + (float) Math.cos(a1) * r;
                final float y1 = this.centreY + (float) Math.sin(a1) * r;
                if (!inside(x0, y0) || !inside(x1, y1)) {
                    continue;
                }
                final float nx = (float) Math.cos(a0) * half;
                final float ny = (float) Math.sin(a0) * half;
                consumer.addVertex(pose, x0 - nx, y0 - ny, 0.002F).setColor(grey, grey, grey, 1.0F);
                consumer.addVertex(pose, x0 + nx, y0 + ny, 0.002F).setColor(grey, grey, grey, 1.0F);
                consumer.addVertex(pose, x1 + nx, y1 + ny, 0.002F).setColor(grey, grey, grey, 1.0F);
                consumer.addVertex(pose, x1 - nx, y1 - ny, 0.002F).setColor(grey, grey, grey, 1.0F);
            }
        }

        private boolean inside(float x, float y) {
            return x >= FRAME && x <= this.frameBx && y >= FRAME && y <= this.frameBy;
        }

        /** 1.12.2 RenderPlanet.drawTexturedRectUV: six bands, each sliding at its own speed, so the planet seems to turn. */
        private void turning(ResourceLocation texture, float x, float y, float width, float height, float ticks, float z) {
            for (int band = 0; band < 6; band++) {
                final float factor = 1.0F + Mth.cos((7.5F + 10.0F * band) / 62.0F);
                bandPair(texture, x, y, width, height, ticks / 1100.0F % 1.0F - (1.0F - factor) * 0.15F, band / 6.0F, 0.16F * factor, z);
            }
        }

        /** 1.12.2 drawTexturedRectUVSixth: one band at the top and its twin at the bottom, wrapping round the texture. */
        private void bandPair(ResourceLocation texture, float x, float y, float width, float height, float prog, float y0, float span, float z) {
            y0 /= 2;
            if (prog < 0.0F) {
                prog += 1.0F;
            }
            prog = 1.0F - prog;
            final float y1 = y0 + 1 / 12.0F;
            final float y2 = 1.0F - y1;
            final float y3 = 1.0F - y0;
            final float yaa = y + height * y0;
            final float yab = y + height * y1;
            final float yba = y + height * y2;
            final float ybb = y + height * y3;
            if (prog <= 1.0F - span) {
                texture(this.poseStack, this.buffers, texture, x, yaa, x + width, yab, z, prog, y0, prog + span, y1);
                texture(this.poseStack, this.buffers, texture, x, yba, x + width, ybb, z, prog, y2, prog + span, y3);
            } else {
                final float xp = x + width * (1.0F - prog) / span;
                texture(this.poseStack, this.buffers, texture, x, yaa, xp, yab, z, prog, y0, 1.0F, y1);
                texture(this.poseStack, this.buffers, texture, x, yba, xp, ybb, z, prog, y2, 1.0F, y3);
                texture(this.poseStack, this.buffers, texture, xp, yaa, x + width, yab, z, 0.0F, y0, prog - 1.0F + span, y1);
                texture(this.poseStack, this.buffers, texture, xp, yba, x + width, ybb, z, 0.0F, y2, prog - 1.0F + span, y3);
            }
        }
    }
}
