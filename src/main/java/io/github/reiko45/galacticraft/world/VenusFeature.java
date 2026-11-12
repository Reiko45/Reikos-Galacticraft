/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.world.gen.ChunkProviderVenus#populate,
 * WorldGenLakesVenus, WorldGenVaporPool and WorldGenCrashedProbe). Copyright (c) 2023 Team Galacticraft. Licensed under
 * the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.Codec;
import io.github.reiko45.galacticraft.block.entity.CrashedProbeBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * What 1.12.2 ChunkProviderVenus#populate added to every chunk before its ores: a lava lake 1 chunk in 10 (1 in 3 in
 * valleys), rimmed with Venus Volcanic Rock; and in valleys, 1 chunk in 5, a sulphuric acid pool sealed in soft rock 10-14
 * blocks under the ground with a Vapor Spout on the surface above it, or failing that, 1 in 190, a Crashed Probe at the
 * bottom of the crater it made, its rim scorched, holding salvage and its Radioisotope Core.
 */
public class VenusFeature extends Feature<NoneFeatureConfiguration> {
    public VenusFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final RandomSource rand = context.random();
        final int minX = context.origin().getX() & ~15;
        final int minZ = context.origin().getZ() & ~15;
        final boolean valley = level.getBiome(new BlockPos(minX + 8, 64, minZ + 8)).is(GCDimensions.VENUS_VALLEY);
        boolean placed = false;
        if (rand.nextInt(valley ? 3 : 10) == 0) {
            final int x = rand.nextInt(16);
            final int y = rand.nextInt(rand.nextInt(248) + 8);
            final int z = rand.nextInt(16);
            placed = lake(level, rand, new BlockPos(minX + x, y, minZ + z));
        }
        if (valley) {
            if (rand.nextInt(5) == 0) {
                final int x = minX + rand.nextInt(16) + 8;
                final int z = minZ + rand.nextInt(16) + 8;
                final int y = top(level, x, z) - 10 - rand.nextInt(5);
                placed |= vaporPool(level, rand, new BlockPos(x, y, z));
            } else if (rand.nextInt(190) == 0) {
                final int x = rand.nextInt(16) + 8;
                final int z = rand.nextInt(16) + 8;
                final int y = top(level, minX + x, minZ + z);
                placed |= crashedProbe(level, rand, new BlockPos(minX + x - 8, y, minZ + z - 8));
            }
        }
        return placed;
    }

    /** 1.12.2 getTopSolidOrLiquidBlock: just above the highest block that stops movement. */
    private static int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    /** 1.12.2 BlockBasicVenus: every block of the planet's rock, ores and bricks (what the probe's crater scorches). */
    private static boolean isVenusRock(BlockState state) {
        final Block block = state.getBlock();
        return Set.of(GCBlocks.VENUS_SOFT_ROCK.get(), GCBlocks.VENUS_HARD_ROCK.get(), GCBlocks.VENUS_VOLCANIC_ROCK.get(), GCBlocks.PUMICE.get(),
                GCBlocks.VENUS_DUNGEON_BRICK_1.get(), GCBlocks.VENUS_DUNGEON_BRICK_2.get(), GCBlocks.VENUS_ALUMINUM_ORE.get(),
                GCBlocks.VENUS_COPPER_ORE.get(), GCBlocks.GALENA_ORE.get(), GCBlocks.VENUS_QUARTZ_ORE.get(), GCBlocks.VENUS_SILICON_ORE.get(),
                GCBlocks.VENUS_TIN_ORE.get(), GCBlocks.LEAD_BLOCK.get(), GCBlocks.SOLAR_ORE.get()).contains(block);
    }

    /**
     * WorldGenLakesVenus (vanilla's lake, with lava and a volcanic rim): from the corner of a 16 x 8 x 16 box, down to the
     * first solid block, 4 to 7 overlapping blobs; abandoned if the lower half would leak or the upper half meets a
     * liquid. The lower half fills with lava, the upper half is cleared, and the walls (and half the roof) turn to
     * Venus Volcanic Rock.
     */
    public static boolean lake(WorldGenLevel level, RandomSource rand, BlockPos start) {
        BlockPos position = start;
        while (position.getY() > level.getMinBuildHeight() + 5 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        if (position.getY() <= level.getMinBuildHeight() + 4) {
            return false;
        }
        position = position.below(4);
        final boolean[] shape = new boolean[2048];
        final int blobs = rand.nextInt(4) + 4;
        for (int j = 0; j < blobs; ++j) {
            final double sizeX = rand.nextDouble() * 6.0D + 3.0D;
            final double sizeY = rand.nextDouble() * 4.0D + 2.0D;
            final double sizeZ = rand.nextDouble() * 6.0D + 3.0D;
            final double centreX = rand.nextDouble() * (16.0D - sizeX - 2.0D) + 1.0D + sizeX / 2.0D;
            final double centreY = rand.nextDouble() * (8.0D - sizeY - 4.0D) + 2.0D + sizeY / 2.0D;
            final double centreZ = rand.nextDouble() * (16.0D - sizeZ - 2.0D) + 1.0D + sizeZ / 2.0D;
            for (int x = 1; x < 15; ++x) {
                for (int z = 1; z < 15; ++z) {
                    for (int y = 1; y < 7; ++y) {
                        final double dx = (x - centreX) / (sizeX / 2.0D);
                        final double dy = (y - centreY) / (sizeY / 2.0D);
                        final double dz = (z - centreZ) / (sizeZ / 2.0D);
                        if (dx * dx + dy * dy + dz * dz < 1.0D) {
                            shape[(x * 16 + z) * 8 + y] = true;
                        }
                    }
                }
            }
        }
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (isEdge(shape, x, y, z)) {
                        final BlockState state = level.getBlockState(position.offset(x, y, z));
                        if (y >= 4 && state.liquid()) {
                            return false;
                        }
                        if (y < 4 && !state.isSolid() && !state.is(Blocks.LAVA)) {
                            return false;
                        }
                    }
                }
            }
        }
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (shape[(x * 16 + z) * 8 + y]) {
                        level.setBlock(position.offset(x, y, z), y >= 4 ? Blocks.AIR.defaultBlockState() : Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // (1.12.2 then grassed any dirt under open sky around the lake, which Venus has none of.)
        final BlockState rim = GCBlocks.VENUS_VOLCANIC_ROCK.get().defaultBlockState();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    final BlockPos pos = position.offset(x, y, z);
                    if (isEdge(shape, x, y, z) && (y < 4 || rand.nextInt(2) != 0) && level.getBlockState(pos).isSolid()) {
                        level.setBlock(pos, rim, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }

    private static boolean isEdge(boolean[] shape, int x, int y, int z) {
        return !shape[(x * 16 + z) * 8 + y]
                && (x < 15 && shape[((x + 1) * 16 + z) * 8 + y] || x > 0 && shape[((x - 1) * 16 + z) * 8 + y]
                || z < 15 && shape[(x * 16 + z + 1) * 8 + y] || z > 0 && shape[(x * 16 + (z - 1)) * 8 + y]
                || y < 7 && shape[(x * 16 + z) * 8 + y + 1] || y > 0 && shape[(x * 16 + z) * 8 + (y - 1)]);
    }

    /**
     * WorldGenVaporPool: a ball of radius 5-8 around the centre, its outer shell (the last 16 of radius squared) Venus
     * Soft Rock, the lower half inside sulphuric acid and the upper half air; then straight up from the ball, the
     * highest solid block becomes a Vapor Spout and everything between it and the ball is cleared.
     */
    public static boolean vaporPool(WorldGenLevel level, RandomSource rand, BlockPos centre) {
        if (centre.getY() <= level.getMinBuildHeight() + 4) {
            return false;
        }
        final BlockState soft = GCBlocks.VENUS_SOFT_ROCK.get().defaultBlockState();
        final BlockState acid = GCFluids.SULPHURIC_ACID_BLOCK.get().defaultBlockState();
        final int radius = 5 + rand.nextInt(4);
        final int radiusSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    final int distance = x * x + y * y + z * z;
                    if (distance <= radiusSq) {
                        final BlockState state = distance >= radiusSq - 16 ? soft : y <= 0 ? acid : Blocks.AIR.defaultBlockState();
                        level.setBlock(centre.offset(x, y, z), state, distance == radiusSq ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        boolean spout = false;
        for (int y = level.getMaxBuildHeight() - 1; y >= centre.getY() + 1; --y) {
            final BlockPos pos = new BlockPos(centre.getX(), y, centre.getZ());
            if (!level.getBlockState(pos).isAir()) {
                if (!spout) {
                    level.setBlock(pos, GCBlocks.VAPOR_SPOUT.get().defaultBlockState(), Block.UPDATE_ALL);
                    spout = true;
                } else {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /**
     * WorldGenCrashedProbe: down to the ground, then (unless a mountain stands over the spot) a crater of radius 5-8,
     * a bowl below and an open shaft to the sky above, its rock walls scorched 1 block in 5; the probe lies at the bottom,
     * its loot not yet rolled.
     */
    public static boolean crashedProbe(WorldGenLevel level, RandomSource rand, BlockPos start) {
        BlockPos position = start;
        while (position.getY() > level.getMinBuildHeight() + 5 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        if (position.getY() <= level.getMinBuildHeight() + 4) {
            return false;
        }
        final int radius = 5 + rand.nextInt(4);
        final int radiusSq = radius * radius;
        final int top = level.getMaxBuildHeight();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= Math.min(top - position.getY(), 50); y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (y > 15 && !level.getBlockState(position.offset(x, y, z)).isAir()) {
                        return false;
                    }
                }
            }
        }
        final BlockState scorched = GCBlocks.SCORCHED_VENUS_ROCK.get().defaultBlockState();
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int y = -radius - 1; y < top - position.getY(); y++) {
                for (int z = -radius - 1; z <= radius + 1; z++) {
                    final int below = Math.min(0, y);
                    final int distance = x * x + below * below + z * z;
                    final BlockPos pos = position.offset(x, y, z);
                    if (distance <= radiusSq) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else if (y < 0 && isVenusRock(level.getBlockState(pos)) && rand.nextInt(5) == 0) {
                        level.setBlock(pos, scorched, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        final BlockPos probePos = position.offset(0, -radius + 1, 0);
        level.setBlock(probePos, GCBlocks.CRASHED_PROBE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(probePos) instanceof CrashedProbeBlockEntity probe) {
            probe.clearContent();
            probe.setLootTable(CrashedProbeBlockEntity.LOOT, rand.nextLong());
            probe.setDropCore();
        }
        return true;
    }
}
