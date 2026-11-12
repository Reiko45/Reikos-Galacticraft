/*
 * Cave algorithm derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.MapGenCavesMoon,
 * micdoodle8.mods.galacticraft.planets.mars.world.gen.MapGenCaveMars and
 * micdoodle8.mods.galacticraft.api.prefab.world.gen.MapGenBaseMeta). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Moon, Mars and Venus caves (1.12.2 MapGenCavesMoon, MapGenCaveMars, MapGenCaveVenus): the classic pre-1.13 worm
 * carver. Every chunk within 8 chunks may start a few tunnels (1 in 15 chunks has any), which wander and branch; where
 * one passes through the chunk being generated it carves away the planet's own rock (and nothing else, so ores survive
 * as in 1.12.2). Only y 1-120 is carved. The planets differ only in what they carve, see {@link Carvable}.
 *
 * <p>Venus's lava tubes (1.12.2 MapGenLavaVenus) are the same carver in a thinner {@link Shape}: single unbranching
 * tunnels from 1 chunk in 25, filled rather than emptied ({@link Replacer}).
 */
final class WormCaves {
    /** Whether a block in a tunnel is removed. Mars rolls the tunnel's own random for its surface blocks. */
    @FunctionalInterface
    interface Carvable {
        boolean carve(BlockState state, Random tunnelRandom);
    }

    /** What a tunnel leaves in place of a block in it, or null to keep the block. */
    @FunctionalInterface
    interface Replacer {
        @Nullable
        BlockState replace(ChunkAccess chunk, BlockPos pos, BlockState state, Random tunnelRandom);
    }

    /**
     * How tunnels are laid out: the radius every tunnel has at its ends, 1 chunk in {@code chance} starting any, whether
     * some start with a wide room and several branches, whether every tunnel has width 1, and the highest y carved.
     */
    record Shape(double baseRadius, int chance, boolean rooms, boolean fixedWidth, int maxY) {
    }

    /** MapGenCavesMoon, MapGenCaveMars, MapGenCaveVenus. */
    static final Shape TUNNELS = new Shape(1.5D, 15, true, false, 120);
    /** MapGenLavaVenus: no rooms, width 1 (radius 0.5 to 1.5), and no height limit. */
    static final Shape LAVA_TUBES = new Shape(0.5D, 25, false, true, 256);

    private static final int RANGE = 8;
    private static final float HALF_PI = (float) (Math.PI / 2.0D);
    private static final float TWO_PI = (float) (Math.PI * 2.0D);

    private final Replacer replacer;
    private final Shape shape;
    private final Random rand = new Random();
    private ChunkAccess chunk;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    WormCaves(Carvable carvable) {
        this((chunk, pos, state, random) -> carvable.carve(state, random) ? Blocks.AIR.defaultBlockState() : null, TUNNELS);
    }

    WormCaves(Replacer replacer, Shape shape) {
        this.replacer = replacer;
        this.shape = shape;
    }

    /** 1.12.2 MapGenBaseMeta#generate. */
    void generate(long worldSeed, ChunkAccess chunk) {
        this.chunk = chunk;
        final int chunkX = chunk.getPos().x;
        final int chunkZ = chunk.getPos().z;
        this.rand.setSeed(worldSeed);
        final long r0 = this.rand.nextLong();
        final long r1 = this.rand.nextLong();
        for (int x0 = chunkX - RANGE; x0 <= chunkX + RANGE; ++x0) {
            for (int z0 = chunkZ - RANGE; z0 <= chunkZ + RANGE; ++z0) {
                this.rand.setSeed(x0 * r0 ^ z0 * r1 ^ worldSeed);
                recursiveGenerate(x0, z0, chunkX, chunkZ);
            }
        }
        this.chunk = null;
    }

    private void recursiveGenerate(int x, int z, int originX, int originZ) {
        int count = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(40) + 1) + 1);
        if (this.rand.nextInt(this.shape.chance()) != 0) {
            count = 0;
        }
        for (int i = 0; i < count; ++i) {
            final double startX = x * 16 + this.rand.nextInt(16);
            final double startY = this.rand.nextInt(this.rand.nextInt(120) + 8);
            final double startZ = z * 16 + this.rand.nextInt(16);
            int tunnels = 1;
            if (this.shape.rooms() && this.rand.nextInt(4) == 0) {
                generateLargeCaveNode(this.rand.nextLong(), originX, originZ, startX, startY, startZ);
                tunnels += this.rand.nextInt(4);
            }
            for (int j = 0; j < tunnels; ++j) {
                final float yaw = this.rand.nextFloat() * TWO_PI;
                final float pitch = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                if (this.shape.fixedWidth()) {
                    generateCaveNode(this.rand.nextLong(), originX, originZ, startX, startY, startZ, 1.0F, yaw, pitch, 0, 0, 1.0D);
                    continue;
                }
                float width = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();
                if (this.rand.nextInt(10) == 0) {
                    width *= this.rand.nextFloat() * this.rand.nextFloat() * 3.0F + 1.0F;
                }
                generateCaveNode(this.rand.nextLong(), originX, originZ, startX, startY, startZ, width, yaw, pitch, 0, 0, 1.0D);
            }
        }
    }

    private void generateLargeCaveNode(long seed, int originX, int originZ, double x, double y, double z) {
        generateCaveNode(seed, originX, originZ, x, y, z, 1.0F + this.rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    private void generateCaveNode(long seed, int originX, int originZ, double x, double y, double z, float width, float yaw, float pitch,
                                  int step, int steps, double heightScale) {
        final double centreX = originX * 16 + 8;
        final double centreZ = originZ * 16 + 8;
        float yawChange = 0.0F;
        float pitchChange = 0.0F;
        final Random random = new Random(seed);
        if (steps <= 0) {
            final int max = RANGE * 16 - 16;
            steps = max - random.nextInt(max / 4);
        }
        boolean room = false;
        if (step == -1) {
            step = steps / 2;
            room = true;
        }
        final int branchAt = random.nextInt(steps / 2) + steps / 4;
        for (final boolean steep = random.nextInt(6) == 0; step < steps; ++step) {
            final double radius = this.shape.baseRadius() + Mth.sin(step * (float) Math.PI / steps) * width;
            final double radiusY = radius * heightScale;
            final float cosPitch = Mth.cos(pitch);
            final float sinPitch = Mth.sin(pitch);
            x += Mth.cos(yaw) * cosPitch;
            y += sinPitch;
            z += Mth.sin(yaw) * cosPitch;
            pitch *= steep ? 0.92F : 0.7F;
            pitch += pitchChange * 0.1F;
            yaw += yawChange * 0.1F;
            pitchChange *= 0.9F;
            yawChange *= 0.75F;
            pitchChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!room && step == branchAt && width > 1.0F && steps > 0) {
                generateCaveNode(random.nextLong(), originX, originZ, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - HALF_PI, pitch / 3.0F, step, steps, 1.0D);
                generateCaveNode(random.nextLong(), originX, originZ, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + HALF_PI, pitch / 3.0F, step, steps, 1.0D);
                return;
            }
            if (!room && random.nextInt(4) == 0) {
                continue;
            }
            final double dx = x - centreX;
            final double dz = z - centreZ;
            final double remaining = steps - step;
            final double reach = width + 2.0F + 16.0F;
            if (dx * dx + dz * dz - remaining * remaining > reach * reach) {
                return;
            }
            if (x < centreX - 16.0D - radius * 2.0D || z < centreZ - 16.0D - radius * 2.0D
                    || x > centreX + 16.0D + radius * 2.0D || z > centreZ + 16.0D + radius * 2.0D) {
                continue;
            }
            final int minX = Math.max(Mth.floor(x - radius) - originX * 16 - 1, 0);
            final int maxX = Math.min(Mth.floor(x + radius) - originX * 16 + 1, 16);
            final int minY = Math.max(Mth.floor(y - radiusY) - 1, 1);
            final int maxY = Math.min(Mth.floor(y + radiusY) + 1, this.shape.maxY());
            final int minZ = Math.max(Mth.floor(z - radius) - originZ * 16 - 1, 0);
            final int maxZ = Math.min(Mth.floor(z + radius) - originZ * 16 + 1, 16);
            for (int localY = minY; localY < maxY; localY++) {
                final double fy = (localY + 0.5D - y) / radiusY;
                for (int localX = minX; localX < maxX; localX++) {
                    final double fx = (localX + originX * 16 + 0.5D - x) / radius;
                    for (int localZ = minZ; localZ < maxZ; localZ++) {
                        final double fz = (localZ + originZ * 16 + 0.5D - z) / radius;
                        if (fx * fx + fz * fz < 1.0D && fy > -0.7D && fx * fx + fy * fy + fz * fz < 1.0D) {
                            this.pos.set(this.chunk.getPos().getMinBlockX() + localX, localY, this.chunk.getPos().getMinBlockZ() + localZ);
                            final BlockState replacement = this.replacer.replace(this.chunk, this.pos, this.chunk.getBlockState(this.pos), random);
                            if (replacement != null) {
                                this.chunk.setBlockState(this.pos, replacement, false);
                            }
                        }
                    }
                }
            }
            if (room) {
                break;
            }
        }
    }
}
