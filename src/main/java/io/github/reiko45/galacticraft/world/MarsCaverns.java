/*
 * Cavern algorithm derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.world.gen.MapGenCavernMars
 * and micdoodle8.mods.galacticraft.api.prefab.world.gen.MapGenBaseMeta). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import io.github.reiko45.galacticraft.block.CavernousVinesBlock;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Mars caverns (1.12.2 MapGenCavernMars): one chunk in a hundred holds the centre of a cavern at y=25, an ellipsoid 80
 * blocks across and 16 tall whose lower part is cut off flat at y=19. One cell in 200 of its ceiling grows a strand of
 * cavernous vines down to the floor, and one floor cell in 200 holds bacterial sludge.
 *
 * <p>1.12.2 reused its worm-cave walker for this but never moved the node, so it carved the same ellipsoid on every one
 * of ~50 steps and each carve also wiped the vines and sludge of the step before; only the last step's decoration
 * survived. Carving and decorating once, as here, gives the same caverns.
 */
public final class MarsCaverns {
    private static final int RANGE = 8;
    private static final double CAVE_WIDTH = 40.0D;
    private static final double HEIGHT_MULTIPLIER = 0.2D;
    private static final double CENTRE_Y = 25.0D;

    private final MarsChunkGenerator generator;
    private final Random rand = new Random();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private ChunkAccess chunk;

    MarsCaverns(MarsChunkGenerator generator) {
        this.generator = generator;
    }

    /** Where caverns centre (at y 25) within {@code radius} chunks of a chunk, by the same seeding as generation. */
    public static java.util.List<BlockPos> centres(long worldSeed, int chunkX, int chunkZ, int radius) {
        final Random random = new Random(worldSeed);
        final long r0 = random.nextLong();
        final long r1 = random.nextLong();
        final java.util.List<BlockPos> centres = new java.util.ArrayList<>();
        for (int x0 = chunkX - radius; x0 <= chunkX + radius; ++x0) {
            for (int z0 = chunkZ - radius; z0 <= chunkZ + radius; ++z0) {
                random.setSeed(x0 * r0 ^ z0 * r1 ^ worldSeed);
                if (random.nextInt(100) == 0) {
                    final int x = x0 * 16 + random.nextInt(16);
                    centres.add(new BlockPos(x, (int) CENTRE_Y, z0 * 16 + random.nextInt(16)));
                }
            }
        }
        return centres;
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
                if (this.rand.nextInt(100) == 0) {
                    final double x = x0 * 16 + this.rand.nextInt(16);
                    final double z = z0 * 16 + this.rand.nextInt(16);
                    final long seed = this.rand.nextLong();
                    final float width = 1.0F + this.rand.nextFloat() * 6.0F;
                    cavern(seed, chunkX, chunkZ, x, z, width);
                }
            }
        }
        this.chunk = null;
    }

    private void cavern(long seed, int originX, int originZ, double x, double z, float width) {
        final Random random = new Random(seed);
        final int max = RANGE * 16 - 16;
        final int steps = max - random.nextInt(max / 4);
        final int step = steps / 2;
        // generateCaveNode returned at once unless this chunk is near enough the cavern for its first step.
        final double dx = x - (originX * 16 + 8);
        final double dz = z - (originZ * 16 + 8);
        final double remaining = steps - step;
        final double reach = width + 2.0F + 16.0F;
        if (dx * dx + dz * dz - remaining * remaining > reach * reach) {
            return;
        }
        if (Math.abs(dx) > 16.0D + CAVE_WIDTH * 2.0D || Math.abs(dz) > 16.0D + CAVE_WIDTH * 2.0D) {
            return;
        }
        final double caveHeight = CAVE_WIDTH * HEIGHT_MULTIPLIER;
        final int minX = Math.max(Mth.floor(x - CAVE_WIDTH) - originX * 16 - 1, 0);
        final int maxX = Math.min(Mth.floor(x + CAVE_WIDTH) - originX * 16 + 1, 16);
        final int minY = Math.max(Mth.floor(CENTRE_Y - caveHeight) - 1, 1);
        final int maxY = Math.min(Mth.floor(CENTRE_Y + caveHeight) + 1, 65);
        final int minZ = Math.max(Mth.floor(z - CAVE_WIDTH) - originZ * 16 - 1, 0);
        final int maxZ = Math.min(Mth.floor(z + CAVE_WIDTH) - originZ * 16 + 1, 16);

        // Carve: Mars rock (and earlier vines and sludge) inside the ellipsoid above its flat floor.
        for (int localX = minX; localX < maxX; ++localX) {
            final double fx = (localX + originX * 16 + 0.5D - x) / CAVE_WIDTH;
            for (int localZ = minZ; localZ < maxZ; ++localZ) {
                final double fz = (localZ + originZ * 16 + 0.5D - z) / CAVE_WIDTH;
                if (fx * fx + fz * fz >= 1.0D) {
                    continue;
                }
                for (int y = maxY - 1; y >= minY; --y) {
                    final double fy = (y + 0.5D - CENTRE_Y) / caveHeight;
                    if (fx * fx + fy * fy + fz * fz < 1.0D && fy > -0.7D) {
                        final BlockState state = get(localX, y, localZ);
                        if (this.generator.isRock(state) || state.is(GCBlocks.CAVERNOUS_VINES.get()) || state.is(GCFluids.BACTERIAL_SLUDGE_BLOCK.get())) {
                            set(localX, y, localZ, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Decorate (with the MapGenBase random, as 1.12.2 did): vines down from the ceiling, sludge on the floor.
        for (int localX = minX; localX < maxX; ++localX) {
            final double fx = (localX + originX * 16 + 0.5D - x) / CAVE_WIDTH;
            for (int localZ = minZ; localZ < maxZ; ++localZ) {
                final double fz = (localZ + originZ * 16 + 0.5D - z) / CAVE_WIDTH;
                if (fx * fx + fz * fz >= 1.0D) {
                    continue;
                }
                for (int y = maxY - 1; y >= minY; --y) {
                    final double fy = (y + 0.5D - CENTRE_Y) / caveHeight;
                    if (fx * fx + fy * fy + fz * fz >= 1.0D || fy <= -0.7D || !get(localX, y, localZ).isAir()) {
                        continue;
                    }
                    if (this.generator.isRock(get(localX, y + 1, localZ)) && this.rand.nextInt(200) == 0) {
                        int length = 0;
                        while (y - length > 0 && get(localX, y - length, localZ).isAir()) {
                            length++;
                        }
                        final CavernousVinesBlock vines = GCBlocks.CAVERNOUS_VINES.get();
                        for (int depth = 0; depth < length; depth++) {
                            set(localX, y - depth, localZ, vines.vine(depth, length - depth));
                        }
                    } else if (this.generator.isRock(get(localX, y - 1, localZ)) && this.rand.nextInt(200) == 0) {
                        set(localX, y, localZ, GCFluids.BACTERIAL_SLUDGE_BLOCK.get().defaultBlockState());
                        // Let the pool settle once the chunk is in the world (1.12.2 sludge flowed on its random ticks).
                        this.chunk.markPosForPostprocessing(this.pos);
                    }
                }
            }
        }
    }

    /**
     * Recount every strand once all carving is done: a later cavern can carve away the lower part of an earlier one's
     * vines (1.12.2 counted strands on every light query, so it never had stale counts).
     */
    static void settleVines(ChunkAccess chunk) {
        final CavernousVinesBlock vines = GCBlocks.CAVERNOUS_VINES.get();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int below = 0;
                for (int y = 1; y < 66; y++) {
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    final BlockState state = chunk.getBlockState(pos);
                    if (!state.is(vines)) {
                        below = 0;
                        continue;
                    }
                    below = Math.min(below + 1, 5);
                    if (state.getValue(CavernousVinesBlock.HANGING) != below) {
                        chunk.setBlockState(pos, state.setValue(CavernousVinesBlock.HANGING, below), false);
                    }
                }
            }
        }
    }

    private BlockState get(int localX, int y, int localZ) {
        return this.chunk.getBlockState(at(localX, y, localZ));
    }

    private void set(int localX, int y, int localZ, BlockState state) {
        this.chunk.setBlockState(at(localX, y, localZ), state, false);
    }

    private BlockPos at(int localX, int y, int localZ) {
        return this.pos.set(this.chunk.getPos().getMinBlockX() + localX, y, this.chunk.getPos().getMinBlockZ() + localZ);
    }
}
