/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.WeatherRendererVenus,
 * TickHandlerClientVenus, FakeLightningBoltRenderer, dimension.WorldProviderVenus#weatherSounds and
 * core.TransformerHooks#addRainParticles). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCParticles;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.VenusWeather;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Venus: its sky ({@link PlanetSkyRenderer#VENUS}), no clouds, and its acid rain. The rain (1.12.2 WeatherRendererVenus)
 * is a curtain of rising streaks that stands on the ground (lower the heavier it falls) and reaches a little above the
 * viewer, fading out towards its edge; where drops land they hiss into acid vapour, and over lava and magma into smoke,
 * with the sound of lava being put out (WorldProviderVenus#weatherSounds). Far away lightning flickers (1.12.2
 * TickHandlerClientVenus): a bolt somewhere 180 blocks off now and then, with thunder, lasting a second.
 */
public final class VenusEffects extends DimensionSpecialEffects {
    private static final ResourceLocation RAIN = GalacticraftCore.id("textures/misc/rain_venus.png");
    private static final Map<BlockPos, Integer> LIGHTNING = new java.util.concurrent.ConcurrentHashMap<>();

    private final PlanetSkyRenderer sky = new PlanetSkyRenderer(PlanetSkyRenderer.VENUS);
    private final float[] rainX = new float[1024];
    private final float[] rainZ = new float[1024];
    private final Random random = new Random();
    private int rainSoundCounter;

    public VenusEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
        for (int i = 0; i < 32; ++i) {
            final float dz = i - 16;
            for (int j = 0; j < 32; ++j) {
                final float dx = j - 16;
                final float length = Mth.sqrt(dx * dx + dz * dz);
                this.rainX[i << 5 | j] = -dz / length;
                this.rainZ[i << 5 | j] = dx / length;
            }
        }
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        return this.sky.render(level, partialTick, modelViewMatrix, camera, projectionMatrix, setupFog);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true; // 1.12.2 gave Venus an empty CloudRenderer
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null ? fogColor : PlanetSkyRenderer.VENUS.fogColor(level.getTimeOfDay(1.0F));
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null; // 1.12.2 WorldProviderVenus#hasSunset
    }

    /** 1.12.2 WeatherRendererVenus#render. */
    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        final float strength = level.getRainLevel(partialTick);
        if (strength <= 0.0F) {
            return true;
        }
        lightTexture.turnOnLightLayer();
        final int px = Mth.floor(camX);
        final int py = Mth.floor(camY);
        final int pz = Mth.floor(camZ);
        final int r = Minecraft.useFancyGraphics() ? 8 : 4;
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, RAIN);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        boolean any = false;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = pz - r; z <= pz + r; ++z) {
            final int indexZ = (z - pz + 16) * 32;
            for (int x = px - r; x <= px + r; ++x) {
                final int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 4 - (int) (4.8F * strength);
                final int y = Math.max(py - r, ground);
                final int yMax = Math.max(py + r, ground);
                final int yBase = Math.max(ground, py);
                if (y == yMax) {
                    continue;
                }
                final int hash = x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
                this.random.setSeed(hash);
                final int index = indexZ + x - px + 16;
                final double dx = this.rainX[index] * 0.5D;
                final double dz = this.rainZ[index] * 0.5D;
                final double scroll = -((double) (ticks + x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 31) + partialTick) / 80.0D
                        * (3.0D + this.random.nextDouble());
                final double lift = this.random.nextDouble() / 1.8D;
                final double ex = x + 0.5D - camX;
                final double ez = z + 0.5D - camZ;
                final float rr = (float) Math.sqrt(ex * ex + ez * ez) / r;
                final float alpha = ((1.0F - rr * rr) * 0.5F + 0.5F) * strength / VenusWeather.MAX_STRENGTH;
                final int light = LevelRenderer.getLightColor(level, pos.set(x, yBase, z));
                final float cx = (float) (x + 0.5D - camX);
                final float cz = (float) (z + 0.5D - camZ);
                final float top = (float) (yMax - lift - camY);
                final float bottom = (float) (y - lift - camY);
                builder.addVertex(cx - (float) dx, top, cz - (float) dz).setUv(0.0F, (float) (y * 0.25D + scroll)).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
                builder.addVertex(cx + (float) dx, top, cz + (float) dz).setUv(1.0F, (float) (y * 0.25D + scroll)).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
                builder.addVertex(cx + (float) dx, bottom, cz + (float) dz).setUv(1.0F, (float) (yMax * 0.25D + scroll)).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
                builder.addVertex(cx - (float) dx, bottom, cz - (float) dz).setUv(0.0F, (float) (yMax * 0.25D + scroll)).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
                any = true;
            }
        }
        if (any) {
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } else {
            builder.build();
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        lightTexture.turnOffLightLayer();
        return true;
    }

    /**
     * 1.12.2 TransformerHooks#addRainParticles for Venus: up to 100 x strength^2 drops a tick within 10 blocks land as
     * acid vapour (or smoke on lava and magma), with WorldProviderVenus's hissing every so often.
     */
    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        final float strength = level.getRainLevel(1.0F);
        final Minecraft minecraft = Minecraft.getInstance();
        int drops = (int) (100.0F * strength * strength);
        final ParticleStatus particles = minecraft.options.particles().get();
        if (particles == ParticleStatus.DECREASED) {
            drops >>= 1;
        } else if (particles == ParticleStatus.MINIMAL) {
            drops = 0;
        }
        if (strength <= 0.0F) {
            return true;
        }
        final RandomSource rand = RandomSource.create(ticks * 312987231L);
        final BlockPos origin = BlockPos.containing(camera.getPosition());
        int landed = 0;
        double soundX = 0.0D;
        double soundY = 0.0D;
        double soundZ = 0.0D;
        for (int i = 0; i < drops; ++i) {
            final BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    origin.offset(rand.nextInt(10) - rand.nextInt(10), 0, rand.nextInt(10) - rand.nextInt(10)));
            if (top.getY() > origin.getY() + 10 || top.getY() < origin.getY() - 10) {
                continue;
            }
            final double fx = rand.nextDouble();
            final double fz = rand.nextDouble();
            final BlockPos below = top.below();
            final BlockState state = level.getBlockState(below);
            if (!state.getFluidState().is(FluidTags.LAVA) && !state.is(Blocks.MAGMA_BLOCK)) {
                if (!state.isAir()) {
                    ++landed;
                    final double height = state.getCollisionShape(level, below).max(Direction.Axis.Y, fx, fz);
                    final double x = below.getX() + fx;
                    final double y = below.getY() + 0.1D + Math.max(0.0D, height);
                    final double z = below.getZ() + fz;
                    if (rand.nextInt(landed) == 0) {
                        soundX = x;
                        soundY = y - 1.0D;
                        soundZ = z;
                    }
                    level.addParticle(GCParticles.ACID_VAPOR.get(), x, y, z, 0.0D, 0.0D, 0.0D);
                }
            } else {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, top.getX() + fx, top.getY() + 0.1D, top.getZ() + fz, 0.0D, 0.0D, 0.0D);
            }
        }
        // WorldProviderVenus#getSoundInterval: 80 - 88 x strength ticks between hisses (every tick in heavy rain)
        final int interval = Math.max(0, 80 - (int) (strength * 88.0F));
        if (landed > 0 && (interval > 0 ? rand.nextInt(interval) < this.rainSoundCounter++ : 0 < this.rainSoundCounter++)) {
            this.rainSoundCounter = 0;
            final RandomSource sound = level.getRandom();
            if ((int) soundY >= origin.getY() + 1 && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin).getY() > origin.getY()) {
                level.playLocalSound(soundX, soundY, soundZ, SoundEvents.LAVA_EXTINGUISH, SoundSource.WEATHER, 0.025F,
                        0.6F + sound.nextFloat() * 0.2F, false);
            } else {
                level.playLocalSound(soundX, soundY, soundZ, SoundEvents.LAVA_EXTINGUISH, SoundSource.WEATHER, 0.04F,
                        0.8F + sound.nextFloat() * 0.06F + sound.nextFloat() * 0.06F, false);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- distant lightning

    /**
     * TickHandlerClientVenus#onPlayerTick: each bolt lasts 20 ticks; a new one strikes at y 70, 180 blocks from the
     * player in a random direction, 1 tick in (300 + 800 x rain strength), with thunder heard from far away.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused() || !minecraft.level.dimension().equals(GCDimensions.VENUS)) {
            if (minecraft.level == null || !minecraft.level.dimension().equals(GCDimensions.VENUS)) {
                LIGHTNING.clear();
            }
            return;
        }
        final Iterator<Map.Entry<BlockPos, Integer>> it = LIGHTNING.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<BlockPos, Integer> entry = it.next();
            if (entry.getValue() - 1 <= 0) {
                it.remove();
            } else {
                entry.setValue(entry.getValue() - 1);
            }
        }
        final RandomSource random = player.getRandom();
        if (random.nextInt(300 + (int) (800.0F * minecraft.level.getRainLevel(1.0F))) == 0) {
            strike(player.getX(), player.getZ(), random);
        }
    }

    /** A distant bolt and its thunder. */
    public static void strike(double fromX, double fromZ, RandomSource random) {
        final double angle = random.nextDouble() * Math.PI * 2.0D;
        strikeAt(fromX + 180.0D * Math.cos(angle), fromZ + 180.0D * Math.sin(angle), random);
    }

    /** A bolt at y 70 here, lasting a second. */
    public static void strikeAt(double x, double z, RandomSource random) {
        strikeAt(x, z, random, 20);
    }

    /** A bolt at y 70 here, lasting this many ticks (the screenshot tour holds one longer). */
    public static void strikeAt(double x, double z, RandomSource random, int ticks) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            minecraft.level.playLocalSound(x, 70.0D, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 500.0F + random.nextFloat() * 500.0F,
                    1.0F + random.nextFloat() * 0.2F, false);
        }
        LIGHTNING.put(BlockPos.containing(x, 70.0D, z), ticks);
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || LIGHTNING.isEmpty()) {
            return;
        }
        final Vec3 cam = event.getCamera().getPosition();
        // The model-view matrix already holds the camera rotation during this pass; positions are camera-relative.
        final com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (Map.Entry<BlockPos, Integer> entry : LIGHTNING.entrySet()) {
            final BlockPos at = entry.getKey();
            final long seed = entry.getValue() / 10 + at.getX() + at.getZ();
            renderBolt(pose.last().pose(), seed, at.getX() - cam.x, at.getY() - cam.y, at.getZ() - cam.z);
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * 1.12.2 FakeLightningBoltRenderer#renderBolt: vanilla's lightning bolt (eight 16-block segments jittered by the
     * seed, with two forks), four nested layers of faint blue-grey.
     */
    private static void renderBolt(Matrix4f matrix, long seed, double x, double y, double z) {
        final double[] offsetX = new double[8];
        final double[] offsetZ = new double[8];
        double endX = 0.0D;
        double endZ = 0.0D;
        final Random random = new Random(seed);
        for (int i = 7; i >= 0; --i) {
            offsetX[i] = endX;
            offsetZ[i] = endZ;
            endX += random.nextInt(11) - 5;
            endZ += random.nextInt(11) - 5;
        }
        for (int layer = 0; layer < 4; ++layer) {
            final Random branch = new Random(seed);
            for (int j = 0; j < 3; ++j) {
                int k = 7;
                int l = 0;
                if (j > 0) {
                    k = 7 - j;
                    l = k - 2;
                }
                double d2 = offsetX[k] - endX;
                double d3 = offsetZ[k] - endZ;
                for (int i1 = k; i1 >= l; --i1) {
                    final double d4 = d2;
                    final double d5 = d3;
                    if (j == 0) {
                        d2 += branch.nextInt(11) - 5;
                        d3 += branch.nextInt(11) - 5;
                    } else {
                        d2 += branch.nextInt(31) - 15;
                        d3 += branch.nextInt(31) - 15;
                    }
                    double d6 = 0.1D + layer * 0.2D;
                    if (j == 0) {
                        d6 *= i1 * 0.1D + 1.0D;
                    }
                    double d7 = 0.1D + layer * 0.2D;
                    if (j == 0) {
                        d7 *= (i1 - 1) * 0.1D + 1.0D;
                    }
                    final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                    for (int j1 = 0; j1 < 5; ++j1) {
                        double d8 = x + 0.5D - d6;
                        double d9 = z + 0.5D - d6;
                        if (j1 == 1 || j1 == 2) {
                            d8 += d6 * 2.0D;
                        }
                        if (j1 == 2 || j1 == 3) {
                            d9 += d6 * 2.0D;
                        }
                        double d10 = x + 0.5D - d7;
                        double d11 = z + 0.5D - d7;
                        if (j1 == 1 || j1 == 2) {
                            d10 += d7 * 2.0D;
                        }
                        if (j1 == 2 || j1 == 3) {
                            d11 += d7 * 2.0D;
                        }
                        builder.addVertex(matrix, (float) (d10 + d2), (float) (y + i1 * 16), (float) (d11 + d3)).setColor(0.45F, 0.45F, 0.5F, 0.3F);
                        builder.addVertex(matrix, (float) (d8 + d4), (float) (y + (i1 + 1) * 16), (float) (d9 + d5)).setColor(0.45F, 0.45F, 0.5F, 0.3F);
                    }
                    BufferUploader.drawWithShader(builder.buildOrThrow());
                }
            }
        }
    }
}
