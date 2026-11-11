/*
 * Terrain algorithm derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.api.prefab.world.gen.ChunkProviderSpace
 * and micdoodle8.mods.galacticraft.planets.mars.world.gen.ChunkProviderMars). Copyright (c) 2023 Team Galacticraft.
 * Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
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
 * The 1.12.2 Mars terrain (ChunkProviderSpace with ChunkProviderMars's settings): a height map around y=93 that seven
 * 2D noise layers blend between gentle plains, small hills, valleys and mountains up to ~190, a few large craters, then
 * fine regolith over ~3 blocks of regolith over Mars stone, on a ragged bedrock floor. Huge flat-floored caverns at
 * y 17-33 hold glowing cavernous vines and pools of bacterial sludge ({@link MarsCaverns}), and Moon-style worm caves
 * run through the rest ({@link WormCaves}).
 *
 * <p>The maths matches the original, including its quirks (marked below). Differences from 1.12.2: noise is seeded
 * from the world seed through {@link RandomState} and noise generators that 1.12.2 re-tuned between uses are split
 * into one fixed-frequency instance per use, so chunks can generate in parallel; ores come from the biome's placed
 * features; the Mars dungeon is not generated yet (a later porting phase).
 */
public final class MarsChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MarsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            BlockState.CODEC.fieldOf("top_block").forGetter(gen -> gen.topBlock),
            BlockState.CODEC.fieldOf("filler_block").forGetter(gen -> gen.fillerBlock),
            BlockState.CODEC.fieldOf("stone_block").forGetter(gen -> gen.stoneBlock)
    ).apply(instance, MarsChunkGenerator::new));

    /** ChunkProviderMars#getSeaLevel, the terrain's mid height. */
    private static final int MID_HEIGHT = 93;
    private static final double HEIGHT_MOD = 12;
    private static final double SMALL_FEATURE_HEIGHT_MOD = 26;
    private static final double MOUNTAIN_HEIGHT_MOD = 95;
    private static final double VALLEY_HEIGHT_MOD = 50;
    private static final int CRATER_PROB = 2000;
    private static final double MAIN_FEATURE_FILTER_MOD = 4;
    private static final double LARGE_FEATURE_FILTER_MOD = 8;
    private static final double SMALL_FEATURE_FILTER_MOD = 8;
    /** generateTerrain filled y 0-254. */
    private static final int TERRAIN_TOP = 255;
    /** makeCrater only dug from y=127 down: in taller columns a crater is a hidden pocket under y=128. */
    private static final int CRATER_TOP = 128;
    private static final int GEN_DEPTH = 256;

    private final BlockState topBlock;
    private final BlockState fillerBlock;
    private final BlockState stoneBlock;
    private final Map<RandomState, Noises> noiseCache = Collections.synchronizedMap(new WeakHashMap<>());

    public MarsChunkGenerator(BiomeSource biomeSource, BlockState topBlock, BlockState fillerBlock, BlockState stoneBlock) {
        super(biomeSource);
        this.topBlock = topBlock;
        this.fillerBlock = fillerBlock;
        this.stoneBlock = stoneBlock;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** Mars rock as the carvers see it (1.12.2 tested for the "mars" block, whose ores came later). */
    boolean isRock(BlockState state) {
        return state == this.topBlock || state == this.fillerBlock || state == this.stoneBlock;
    }

    BlockState topBlock() {
        return this.topBlock;
    }

    // ---------------------------------------------------------------- noise

    /**
     * ChunkProviderSpace's noiseGen1-7. 1.12.2 set noiseGen4 to 0.0625 for the filler depth and noiseGen5 to 0.015 for
     * craters between terrain passes; those are the {@code fillerDepth} and {@code crater} copies here (same seeds).
     */
    private record Noises(GradientNoise base, GradientNoise smallHill, GradientNoise mountain, GradientNoise valley,
                          GradientNoise featureFilter, GradientNoise largeFilter, GradientNoise smallFilter,
                          GradientNoise crater, GradientNoise fillerDepth, long surfaceSeed) {
    }

    private Noises noises(RandomState randomState) {
        return this.noiseCache.computeIfAbsent(randomState, state -> {
            final long seed = state.getOrCreateRandomFactory(GalacticraftCore.id("mars_terrain")).fromHashOf("terrain").nextLong();
            final Random seeder = new Random(seed);
            final long s1 = seeder.nextLong();
            final long s2 = seeder.nextLong();
            final long s3 = seeder.nextLong();
            final long s4 = seeder.nextLong();
            final long s5 = seeder.nextLong();
            final long s6 = seeder.nextLong();
            final long s7 = seeder.nextLong();
            return new Noises(
                    new GradientNoise(s1, 4, 0.25D, 0.014999999664723873D),
                    new GradientNoise(s2, 4, 0.25D, 0.0030000000474999913D),
                    new GradientNoise(s3, 4, 0.25D, 0.01D),
                    new GradientNoise(s4, 2, 0.25D, 0.01D),
                    new GradientNoise(s5, 1, 0.25D, 0.01D),
                    new GradientNoise(s6, 1, 0.25D, 0.0010000000474974513D),
                    new GradientNoise(s7, 1, 0.25D, 0.004999999888241291D),
                    new GradientNoise(s5, 1, 0.25D, 0.014999999664723873D),
                    new GradientNoise(s4, 2, 0.25D, 0.0625D),
                    seed);
        });
    }

    // ---------------------------------------------------------------- height map

    /**
     * Solid blocks from y=0 in each column of a chunk (index {@code x + z * 16}), plus how many blocks craters hollowed
     * out under y=128 in columns that are taller than that.
     */
    private record Columns(int[] heights, int[] pockets) {
        boolean solid(int index, int y) {
            if (y >= this.heights[index]) {
                return false;
            }
            final int pocket = this.pockets[index];
            return pocket == 0 || y < CRATER_TOP - pocket || y >= CRATER_TOP;
        }
    }

    private Columns columns(Noises noises, int chunkX, int chunkZ) {
        final int[] heights = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                heights[x + z * 16] = baseColumnHeight(noises, chunkX * 16 + x, chunkZ * 16 + z);
            }
        }
        final int[] pockets = new int[256];
        carveCraters(noises, chunkX, chunkZ, heights, pockets);
        return new Columns(heights, pockets);
    }

    private static int baseColumnHeight(Noises noises, int worldX, int worldZ) {
        final double baseHeight = noises.base.eval(worldX, worldZ) * HEIGHT_MOD;
        final double smallHillHeight = noises.smallHill.eval(worldX, worldZ) * SMALL_FEATURE_HEIGHT_MOD;
        double mountainHeight = Math.abs(noises.mountain.eval(worldX, worldZ));
        double valleyHeight = Math.abs(noises.valley.eval(worldX, worldZ));
        final double featureFilter = noises.featureFilter.eval(worldX, worldZ) * MAIN_FEATURE_FILTER_MOD;
        final double largeFilter = noises.largeFilter.eval(worldX, worldZ) * LARGE_FEATURE_FILTER_MOD;
        final double smallFilter = noises.smallFilter.eval(worldX, worldZ) * SMALL_FEATURE_FILTER_MOD - 0.5D;
        mountainHeight = lerp(smallHillHeight, mountainHeight * MOUNTAIN_HEIGHT_MOD, fade(clamp(mountainHeight * 2.0D)));
        // Quirk kept from 1.12.2: (valleyHeight + 2) * 4 is always above 1, so valleys always take the second branch.
        valleyHeight = lerp(smallHillHeight, valleyHeight * VALLEY_HEIGHT_MOD - VALLEY_HEIGHT_MOD + 9.0D, fade(clamp((valleyHeight + 2.0D) * 4.0D)));
        double yDev = lerp(valleyHeight, mountainHeight, fade(largeFilter));
        yDev = lerp(smallHillHeight, yDev, smallFilter);
        yDev = lerp(baseHeight, yDev, featureFilter);
        // 1.12.2 filled every y < MID_HEIGHT + yDev up to y=254.
        return Math.clamp((int) Math.ceil(MID_HEIGHT + yDev), 0, TERRAIN_TOP);
    }

    private static double lerp(double from, double to, double t) {
        if (t < 0.0D) {
            return from;
        }
        if (t > 1.0D) {
            return to;
        }
        return from + (to - from) * t;
    }

    private static double fade(double n) {
        return n * n * n * (n * (n * 6.0D - 15.0D) + 10.0D);
    }

    private static double clamp(double x) {
        return x < 0.0D ? 0.0D : Math.min(x, 1.0D);
    }

    private static void carveCraters(Noises noises, int chunkX, int chunkZ, int[] heights, int[] pockets) {
        for (int cx = chunkX - 2; cx <= chunkX + 2; cx++) {
            for (int cz = chunkZ - 2; cz <= chunkZ + 2; cz++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        final int craterX = cx * 16 + x;
                        final int craterZ = cz * 16 + z;
                        if (Math.abs(MoonChunkGenerator.randFromPoint(craterX, craterZ * 1000)) < noises.crater.eval(craterX, craterZ) / CRATER_PROB) {
                            final Random random = new Random(craterX + craterZ * 5000);
                            final int[] size = MoonChunkGenerator.CRATER_SIZES[random.nextInt(MoonChunkGenerator.CRATER_SIZES.length)];
                            final int radius = random.nextInt(size[1] - size[0]) + size[0] + 15;
                            makeCrater(craterX, craterZ, chunkX * 16, chunkZ * 16, radius, heights, pockets);
                        }
                    }
                }
            }
        }
    }

    private static void makeCrater(int craterX, int craterZ, int chunkMinX, int chunkMinZ, int radius, int[] heights, int[] pockets) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double xDev = craterX - (chunkMinX + x);
                double zDev = craterZ - (chunkMinZ + z);
                if (xDev * xDev + zDev * zDev < radius * radius) {
                    xDev /= radius;
                    zDev /= radius;
                    final double distSq = xDev * xDev + zDev * zDev;
                    final double depth = 5 - distSq * distSq * 6;
                    if (depth < 0) {
                        continue;
                    }
                    // 1.12.2 removed solid blocks from y=127 down while (removed <= depth): floor(depth) + 1 of them.
                    final int removed = (int) Math.floor(depth) + 1;
                    final int index = x + z * 16;
                    if (heights[index] <= CRATER_TOP) {
                        heights[index] = Math.max(1, heights[index] - removed);
                    } else {
                        pockets[index] = Math.min(pockets[index] + removed, CRATER_TOP - 1);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- generation

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        final Noises noises = noises(randomState);
        final int chunkX = chunk.getPos().x;
        final int chunkZ = chunk.getPos().z;
        final Columns columns = columns(noises, chunkX, chunkZ);
        final int minY = Math.max(chunk.getMinBuildHeight(), 0);
        final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        final Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Same per-chunk seed formula as 1.12.2, mixed with the world seed.
        final Random rand = new Random((chunkX * 341873128712L + chunkZ * 132897987541L) ^ noises.surfaceSeed);

        // ChunkProviderSpace#replaceBiomeBlocks: scanning down, the first rock under air becomes the top block and the
        // next fillerDepth blocks filler (the floor of a crater pocket gets a surface of its own, as in 1.12.2).
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int index = x + z * 16;
                final int fillerDepth = (int) (noises.fillerDepth.eval(chunkX * 16 + x, chunkZ * 16 + z) / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
                int remaining = -1;
                for (int y = TERRAIN_TOP - 1; y >= minY; y--) {
                    // 1.12.2 rolled the bedrock check for every y, which also advances the RNG; keep that.
                    if (y <= rand.nextInt(5)) {
                        chunk.setBlockState(pos.set(x, y, z), bedrock, false);
                        continue;
                    }
                    if (!columns.solid(index, y)) {
                        remaining = -1;
                        continue;
                    }
                    final BlockState state;
                    if (remaining == -1) {
                        remaining = fillerDepth;
                        // Quirk kept from 1.12.2: below y=19 the surface block is filler (never reached on Mars).
                        state = y >= 19 ? this.topBlock : this.fillerBlock;
                    } else if (remaining > 0) {
                        remaining--;
                        state = this.fillerBlock;
                    } else {
                        state = this.stoneBlock;
                    }
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                }
                final int topY = columns.heights[index] - 1;
                oceanFloor.update(x, topY, z, this.topBlock);
                worldSurface.update(x, topY, z, this.topBlock);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        final Columns columns = columns(noises(randomState), x >> 4, z >> 4);
        return Math.max(columns.heights[(x & 15) + (z & 15) * 16], level.getMinBuildHeight());
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

    /**
     * ChunkProviderMars#getWorldGenerators: the caverns first, then the worm caves, which carve regolith and stone and
     * break through the fine regolith only 1 time in 25 (MapGenCaveMars#BREAK_THROUGH_CHANCE).
     */
    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        if (step == GenerationStep.Carving.AIR) {
            new MarsCaverns(this).generate(seed, chunk);
            new WormCaves((state, tunnelRandom) -> {
                if (state == this.fillerBlock || state == this.stoneBlock) {
                    return true;
                }
                return state == this.topBlock && tunnelRandom.nextInt(25) == 0;
            }).generate(seed, chunk);
            MarsCaverns.settleVines(chunk);
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // Surface blocks are placed in fillFromNoise, exactly like ChunkProviderSpace#replaceBiomeBlocks.
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
        info.add("Galacticraft Mars terrain");
    }
}
