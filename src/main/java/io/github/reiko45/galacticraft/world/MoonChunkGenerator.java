/*
 * Terrain algorithm derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.ChunkProviderMoon
 * and EnumCraterSize). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.world.noise.GradientNoise;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * The 1.12.2 Moon terrain: a height map of two blended 2D noise layers around y=63, with bowl-shaped
 * craters cut into it, then one block of turf over 2-3 blocks of moon dirt over moon rock, and a
 * ragged bedrock floor.
 *
 * <p>The maths matches the original so the landscape looks the same, including some of its quirks
 * (marked below). Differences from 1.12.2:
 * <ul>
 *     <li>noise generators are seeded from the world seed through {@link RandomState}, and are immutable
 *     so chunks can generate in parallel;</li>
 *     <li>caves, moon villages and the boss dungeon are not generated yet (later porting phases);</li>
 *     <li>ores are not hard-coded here but come from the biome's placed features (see datagen).</li>
 * </ul>
 */
public final class MoonChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MoonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            BlockState.CODEC.fieldOf("top_block").forGetter(gen -> gen.topBlock),
            BlockState.CODEC.fieldOf("filler_block").forGetter(gen -> gen.fillerBlock),
            BlockState.CODEC.fieldOf("stone_block").forGetter(gen -> gen.stoneBlock)
    ).apply(instance, MoonChunkGenerator::new));

    private static final int MID_HEIGHT = 63;
    /** 1.12.2 only generated terrain in the bottom 128 blocks; the dimension itself is 256 tall. */
    private static final int TERRAIN_TOP = 128;
    private static final int GEN_DEPTH = 256;
    private static final int CRATER_PROB = 300;
    /** Crater size classes {min radius, max radius}, weighted 14:8:2:1 like EnumCraterSize. */
    private static final int[][] CRATER_SIZES = buildCraterSizes();

    private final BlockState topBlock;
    private final BlockState fillerBlock;
    private final BlockState stoneBlock;
    private final Map<RandomState, Noises> noiseCache = Collections.synchronizedMap(new WeakHashMap<>());

    public MoonChunkGenerator(BiomeSource biomeSource, BlockState topBlock, BlockState fillerBlock, BlockState stoneBlock) {
        super(biomeSource);
        this.topBlock = topBlock;
        this.fillerBlock = fillerBlock;
        this.stoneBlock = stoneBlock;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ---------------------------------------------------------------- noise

    private record Noises(GradientNoise low, GradientNoise high, GradientNoise blend, GradientNoise detail, long surfaceSeed) {
    }

    private Noises noises(RandomState randomState) {
        return this.noiseCache.computeIfAbsent(randomState, state -> {
            final long seed = state.getOrCreateRandomFactory(GalacticraftCore.id("moon_terrain")).fromHashOf("terrain").nextLong();
            final Random seeder = new Random(seed);
            return new Noises(
                    new GradientNoise(seeder.nextLong(), 4, 0.25D, 0.012500000186264515D),
                    new GradientNoise(seeder.nextLong(), 4, 0.25D, 0.014999999664723873D),
                    // Frequency 0 in 1.12.2 too, so this is a per-world constant that picks the blend of the two layers.
                    new GradientNoise(seeder.nextLong(), 1, 0.25D, 0.0D),
                    new GradientNoise(seeder.nextLong(), 1, 0.25D, 0.019999999552965164D),
                    seed);
        });
    }

    // ---------------------------------------------------------------- height map

    /** Number of solid blocks (from y=0) in each column of the chunk, index {@code x + z * 16}. */
    private int[] columnHeights(Noises noises, int chunkX, int chunkZ) {
        final int[] heights = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                heights[x + z * 16] = baseColumnHeight(noises, chunkX * 16 + x, chunkZ * 16 + z);
            }
        }
        carveCraters(noises, chunkX, chunkZ, heights);
        return heights;
    }

    private static int baseColumnHeight(Noises noises, int worldX, int worldZ) {
        final double low = noises.low.eval(worldX, worldZ) * 8;
        final double high = noises.high.eval(worldX, worldZ) * 24;
        final double blend = (noises.blend.eval(worldX, worldZ) - 0.1D) * 4;
        final double yDev;
        if (blend < 0.0D) {
            yDev = low;
        } else if (blend > 1.0D) {
            yDev = high;
        } else {
            yDev = low + (high - low) * blend;
        }
        // 1.12.2 filled every y < MID_HEIGHT + yDev.
        return Math.clamp((int) Math.ceil(MID_HEIGHT + yDev), 1, TERRAIN_TOP);
    }

    private static void carveCraters(Noises noises, int chunkX, int chunkZ, int[] heights) {
        for (int cx = chunkX - 2; cx <= chunkX + 2; cx++) {
            for (int cz = chunkZ - 2; cz <= chunkZ + 2; cz++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        final int craterX = cx * 16 + x;
                        final int craterZ = cz * 16 + z;
                        // Quirk kept from 1.12.2: the noise is sampled at (17 * x, craterZ), not at the crater position.
                        final double threshold = noises.detail.eval(x * 16 + x, craterZ) / CRATER_PROB;
                        if (Math.abs(randFromPoint(craterX, craterZ * 1000)) < threshold) {
                            final Random random = new Random(craterX + craterZ * 5000);
                            final int[] size = CRATER_SIZES[random.nextInt(CRATER_SIZES.length)];
                            final int radius = random.nextInt(size[1] - size[0]) + size[0];
                            makeCrater(craterX, craterZ, chunkX * 16, chunkZ * 16, radius, heights);
                        }
                    }
                }
            }
        }
    }

    private static void makeCrater(int craterX, int craterZ, int chunkMinX, int chunkMinZ, int radius, int[] heights) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double xDev = craterX - (chunkMinX + x);
                double zDev = craterZ - (chunkMinZ + z);
                if (xDev * xDev + zDev * zDev < radius * radius) {
                    xDev /= radius;
                    zDev /= radius;
                    final double distSq = xDev * xDev + zDev * zDev;
                    final double depth = 5 - distSq * distSq * 6;
                    // The 1.12.2 loop removed blocks while (removed <= depth), i.e. floor(depth) + 1 of them.
                    if (depth >= 0) {
                        final int index = x + z * 16;
                        heights[index] = Math.max(1, heights[index] - ((int) Math.floor(depth) + 1));
                    }
                }
            }
        }
    }

    /** Integer hash noise in [-1, 1] from 1.12.2, including its int overflow behaviour. */
    private static double randFromPoint(int x, int z) {
        int n = x + z * 57;
        n = n << 13 ^ n;
        return 1.0D - (n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff) / 1073741824.0D;
    }

    private static int[][] buildCraterSizes() {
        final int[][] classes = {{8, 12, 14}, {13, 17, 8}, {18, 25, 2}, {26, 30, 1}};
        int total = 0;
        for (int[] c : classes) {
            total += c[2];
        }
        final int[][] weighted = new int[total][];
        int i = 0;
        for (int[] c : classes) {
            for (int n = 0; n < c[2]; n++) {
                weighted[i++] = new int[]{c[0], c[1]};
            }
        }
        return weighted;
    }

    // ---------------------------------------------------------------- generation

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        final Noises noises = noises(randomState);
        final int chunkX = chunk.getPos().x;
        final int chunkZ = chunk.getPos().z;
        final int[] heights = columnHeights(noises, chunkX, chunkZ);
        final int minY = chunk.getMinBuildHeight();
        final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        final Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Same per-chunk seed formula as 1.12.2, mixed with the world seed.
        final Random rand = new Random((chunkX * 341873128712L + chunkZ * 132897987541L) ^ noises.surfaceSeed);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int height = heights[x + z * 16];
                final int topY = height - 1;
                final int fillerDepth = (int) (noises.detail.eval(x + chunkX * 16, z + chunkZ * 16) / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
                for (int y = TERRAIN_TOP - 1; y >= Math.max(minY, 0); y--) {
                    // 1.12.2 rolled the bedrock check for every y, which also advances the RNG; keep that.
                    final boolean isBedrock = y <= rand.nextInt(5);
                    final BlockState state;
                    if (isBedrock) {
                        state = bedrock;
                    } else if (y > topY) {
                        continue;
                    } else if (y == topY) {
                        state = this.topBlock;
                    } else if (y >= topY - fillerDepth) {
                        state = this.fillerBlock;
                    } else {
                        state = this.stoneBlock;
                    }
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                }
                oceanFloor.update(x, topY, z, this.topBlock);
                worldSurface.update(x, topY, z, this.topBlock);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        final int[] heights = columnHeights(noises(randomState), x >> 4, z >> 4);
        return Math.max(heights[(x & 15) + (z & 15) * 16], level.getMinBuildHeight());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        final int height = getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        final int minY = level.getMinBuildHeight();
        final BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            final int y = minY + i;
            if (y <= 0) {
                states[i] = Blocks.BEDROCK.defaultBlockState();
            } else if (y < height - 4) {
                states[i] = this.stoneBlock;
            } else if (y < height - 1) {
                states[i] = this.fillerBlock;
            } else if (y == height - 1) {
                states[i] = this.topBlock;
            } else {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // TODO(phase 2): port MapGenCavesMoon.
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // Surface blocks are placed in fillFromNoise, exactly like ChunkProviderMoon#replaceBlocksForBiome.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Galacticraft Moon terrain");
    }
}
