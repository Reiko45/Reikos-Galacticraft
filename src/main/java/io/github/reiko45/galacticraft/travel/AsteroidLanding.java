/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.dimension.TeleportTypeAsteroids and
 * WorldProviderAsteroids#getClosestAsteroidXZ). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.travel;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Where an Entry Pod comes down (1.12.2 TeleportTypeAsteroids#getPlayerSpawnLocation): over the nearest asteroid not
 * landed on before, if one is within 160 blocks, with a 2x2 shaft cut through any asteroid rock above its top so the pod
 * falls onto it; up to five asteroids are tried. Failing that, a small platform of asteroid rock is made at the take-off
 * point. The pod starts at y 310.
 */
public final class AsteroidLanding {
    public static final double ARRIVAL_Y = 310.0D;
    private static final int MAX_DISTANCE_SQ = 160 * 160;

    private AsteroidLanding() {
    }

    /** The block corner the pod is centred on (it covers the four columns around it), at y 310. */
    public static BlockPos find(ServerLevel level, int x, int z) {
        if (level.getChunkSource().getGenerator() instanceof AsteroidsChunkGenerator generator) {
            final List<AsteroidsChunkGenerator.Asteroid> asteroids = generator.asteroidsNear(level.getChunkSource().randomState(), x, z, 160);
            final Landings landings = Landings.get(level);
            for (int attempt = 0; attempt < 5; attempt++) {
                final AsteroidsChunkGenerator.Asteroid closest = closest(asteroids, landings, x, z);
                if (closest == null) {
                    break;
                }
                final long dx = closest.x() - x;
                final long dy = closest.y() - 128;
                final long dz = closest.z() - z;
                // 1.12.2 BlockVec3#distanceSquared from (x, 128, z)
                if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQ) {
                    break;
                }
                landings.mark(closest);
                final int[][] tries = {{0, 0}, {2, 2}, {2, -2}, {-2, -2}, {-2, 2}};
                for (int[] offset : tries) {
                    if (goodEntry(level, closest.x() + offset[0], closest.y(), closest.z() + offset[1])) {
                        return new BlockPos(closest.x() + offset[0], (int) ARRIVAL_Y, closest.z() + offset[1]);
                    }
                }
            }
        }
        GalacticraftCore.LOGGER.info("Failed to find a good large asteroid landing spot, making a small one");
        makeSmallLandingSpot(level, x, z);
        return new BlockPos(x, (int) ARRIVAL_Y, z);
    }

    /** 1.12.2 getClosestAsteroidXZ with mark = true: the nearest in x and z that nobody has landed on. */
    private static AsteroidsChunkGenerator.Asteroid closest(List<AsteroidsChunkGenerator.Asteroid> asteroids, Landings landings, int x, int z) {
        AsteroidsChunkGenerator.Asteroid result = null;
        long lowest = Long.MAX_VALUE;
        for (AsteroidsChunkGenerator.Asteroid asteroid : asteroids) {
            if (landings.landed(asteroid)) {
                continue;
            }
            final long dx = x - asteroid.x();
            final long dz = z - asteroid.z();
            if (dx * dx + dz * dz < lowest) {
                lowest = dx * dx + dz * dz;
                result = asteroid;
            }
        }
        return result;
    }

    /**
     * 1.12.2 goodAsteroidEntry: the column has rock within 20 blocks of the asteroid's centre height; asteroid rock above
     * it, in the 2x2 the pod fills, is cleared.
     */
    private static boolean goodEntry(ServerLevel level, int x, int centreY, int z) {
        for (int k = 208; k > 48; k--) {
            if (level.getBlockState(new BlockPos(x, k, z)).isAir() || Math.abs(k - centreY) > 20) {
                continue;
            }
            for (int y = k + 2; y < 256; y++) {
                for (BlockPos pos : new BlockPos[]{new BlockPos(x, y, z), new BlockPos(x - 1, y, z), new BlockPos(x, y, z - 1), new BlockPos(x - 1, y, z - 1)}) {
                    if (isAsteroidBlock(level.getBlockState(pos))) {
                        level.removeBlock(pos, false);
                    }
                }
            }
            return true;
        }
        return false;
    }

    /** 1.12.2 AsteroidBlocks.blockBasic: the rocks, their ores, the dark decoration and titanium blocks. */
    private static boolean isAsteroidBlock(BlockState state) {
        return state.is(GCBlocks.ASTEROID_ROCK_0.get()) || state.is(GCBlocks.ASTEROID_ROCK_1.get()) || state.is(GCBlocks.ASTEROID_ROCK_2.get())
                || state.is(GCBlocks.ASTEROID_ALUMINUM_ORE.get()) || state.is(GCBlocks.ILMENITE_ORE.get()) || state.is(GCBlocks.ASTEROID_IRON_ORE.get())
                || state.is(GCBlocks.DARK_DECORATION_BLOCK.get()) || state.is(GCBlocks.TITANIUM_BLOCK.get());
    }

    /** 1.12.2 makeSmallLandingSpot: under the first block found coming down, or at a random height in open space. */
    private static void makeSmallLandingSpot(ServerLevel level, int x, int z) {
        for (int k = 255; k > 48; k--) {
            final int[][] columns = {{0, 0}, {-1, 0}, {-1, -1}, {0, -1}};
            for (int[] column : columns) {
                if (!level.getBlockState(new BlockPos(x + column[0], k, z + column[1])).isAir()) {
                    makePlatform(level, x + column[0], k - 1, z + column[1]);
                    return;
                }
            }
        }
        makePlatform(level, x, 48 + level.random.nextInt(128), z);
    }

    /** 1.12.2 makePlatform: a rounded 6x6 layer, a 4x4 under it and a 2x2 under that. */
    private static void makePlatform(ServerLevel level, int x, int y, int z) {
        for (int xx = -3; xx < 3; xx++) {
            for (int zz = -3; zz < 3; zz++) {
                if ((xx == -3 || xx == 2) && (zz == -3 || zz == 2)) {
                    continue;
                }
                platformBlock(level, x + xx, y, z + zz);
            }
        }
        for (int xx = -2; xx < 2; xx++) {
            for (int zz = -2; zz < 2; zz++) {
                platformBlock(level, x + xx, y - 1, z + zz);
            }
        }
        platformBlock(level, x - 1, y - 2, z - 1);
        platformBlock(level, x - 1, y - 2, z);
        platformBlock(level, x, y - 2, z);
        platformBlock(level, x, y - 2, z - 1);
    }

    private static void platformBlock(ServerLevel level, int x, int y, int z) {
        final BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir()) {
            // 1.12.2 meta (int) (rand * 1.5): rock 0 two times in three, rock 1 otherwise
            final Block rock = (int) (level.random.nextFloat() * 1.5F) == 0 ? GCBlocks.ASTEROID_ROCK_0.get() : GCBlocks.ASTEROID_ROCK_1.get();
            level.setBlock(pos, rock.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** 1.12.2 AsteroidData's landed flag: asteroids already landed on, saved with the asteroid world. */
    public static final class Landings extends SavedData {
        private static final String NAME = GalacticraftCore.MOD_ID + "_asteroid_landings";
        private final Set<Long> landed = new HashSet<>();

        static Landings get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Landings::new, Landings::load), NAME);
        }

        private static Landings load(CompoundTag tag, HolderLookup.Provider registries) {
            final Landings landings = new Landings();
            for (long pos : tag.getLongArray("Landed")) {
                landings.landed.add(pos);
            }
            return landings;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("Landed", new LongArrayTag(this.landed.stream().mapToLong(Long::longValue).toArray()));
            return tag;
        }

        boolean landed(AsteroidsChunkGenerator.Asteroid asteroid) {
            return this.landed.contains(BlockPos.asLong(asteroid.x(), asteroid.y(), asteroid.z()));
        }

        void mark(AsteroidsChunkGenerator.Asteroid asteroid) {
            this.landed.add(BlockPos.asLong(asteroid.x(), asteroid.y(), asteroid.z()));
            setDirty();
        }
    }
}
