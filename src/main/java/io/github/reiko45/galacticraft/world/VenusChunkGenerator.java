/*
 * Terrain algorithm derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.world.gen.ChunkProviderVenus,
 * BiomeVenus, BiomeGenVenusValley, MapGenCaveVenus and MapGenLavaVenus). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.world.noise.GradientNoise;
import io.github.reiko45.galacticraft.world.noise.LegacyOctaves;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

/**
 * The 1.12.2 Venus terrain (ChunkProviderVenus): a 3D density field like the pre-1.13 Overworld's, its height and
 * roughness blended from the biomes around each 4-block cell (flat plains around y 72, mountains rising past 100,
 * valleys sunk to the 50s), cut wherever it falls below a smooth 2D noise threshold, all of Venus Hard Rock. Worm caves
 * then run through it, thin lava tubes are filled with lava behind a skin of Venus Volcanic Rock, and each biome lays
 * its surface: on the flats and mountains a crust of hard rock over a few blocks of soft rock, in the valleys soft rock,
 * with pumice on the deep valley floors. Bedrock is ragged over y 0-4.
 *
 * <p>Differences from 1.12.2: noise is seeded from the world seed through {@link RandomState}, and the surface and
 * bedrock random mixes in the world seed too (1.12.2 gave every world the same); caves, lava tubes and the surface run in
 * {@link #buildSurface}, in the same order as 1.12.2 (before the surface, so the caves cut through it); see
 * {@link LegacyOctaves} for the noise.
 */
public final class VenusChunkGenerator extends ChunkGenerator {
    public static final MapCodec<VenusChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            VenusBiomeSource.CODEC.codec().fieldOf("biome_source").forGetter(gen -> gen.venusBiomes)
    ).apply(instance, VenusChunkGenerator::new));

    /** 1.12.2 World#getSeaLevel (Venus never changed it): where the surface rules switch. */
    private static final int SEA_LEVEL = 63;
    private static final int GEN_DEPTH = 256;
    /** 1.12.2 Biome base heights and height variations: Venus Flat, Mountain and Valley (BiomeVenus). */
    private static final float[] BASE_HEIGHT = {0.5F, 2.0F, -0.4F};
    private static final float[] HEIGHT_VARIATION = {0.4F, 1.0F, 0.2F};
    private static final float[] PARABOLIC_FIELD = new float[25];

    static {
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                PARABOLIC_FIELD[i + 2 + (j + 2) * 5] = 10.0F / (float) Math.sqrt(i * i + j * j + 0.2F);
            }
        }
    }

    private final VenusBiomeSource venusBiomes;
    private final Map<RandomState, Noises> noiseCache = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Long, int[]> heightCache = new ConcurrentHashMap<>();

    public VenusChunkGenerator(VenusBiomeSource biomeSource) {
        super(biomeSource);
        this.venusBiomes = biomeSource;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** The world's seed reaches the biome layers here, when the world is created (1.21 biome sources aren't told it). */
    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long seed) {
        this.venusBiomes.setSeed(seed);
        return super.createState(structureSets, randomState, seed);
    }

    public VenusBiomeSource venusBiomes() {
        return this.venusBiomes;
    }

    private static BlockState hardRock() {
        return GCBlocks.VENUS_HARD_ROCK.get().defaultBlockState();
    }

    // ---------------------------------------------------------------- noise

    /**
     * The ChunkProviderVenus noise that the terrain uses: noiseGen2 (the 3D density), noiseGen6 (the 2D height offset),
     * noiseGen4 (surface depth) and noiseGenSmooth1 (the solid threshold). Its noiseGen1 and noiseGen3 were sampled but
     * never read, and noiseGen5 and mobSpawnerNoise never sampled.
     */
    private record Noises(LegacyOctaves density, LegacyOctaves heightOffset, PerlinSimplexNoise surfaceDepth, GradientNoise threshold,
                          long surfaceSeed) {
    }

    private Noises noises(RandomState randomState) {
        return this.noiseCache.computeIfAbsent(randomState, state -> {
            final var factory = state.getOrCreateRandomFactory(GalacticraftCore.id("venus_terrain"));
            final RandomSource density = factory.fromHashOf("density");
            final RandomSource offset = factory.fromHashOf("height_offset");
            final RandomSource depth = factory.fromHashOf("surface_depth");
            final long threshold = factory.fromHashOf("threshold").nextLong();
            return new Noises(new LegacyOctaves(density, 16), new LegacyOctaves(offset, 16),
                    new PerlinSimplexNoise(depth, IntStream.rangeClosed(-3, 0).boxed().toList()),
                    new GradientNoise(threshold, 4, 0.25D, 0.014999999664723873D), factory.fromHashOf("surface").nextLong());
        });
    }

    // ---------------------------------------------------------------- terrain

    /**
     * ChunkProviderVenus#createLandPerBiome: the density at each corner of a 5 x 33 x 5 grid of 4 x 8 x 4 cells,
     * indexed {@code (x * 5 + z) * 33 + y}.
     */
    private double[] densities(Noises noises, int chunkX, int chunkZ) {
        final int cellX = chunkX * 4;
        final int cellZ = chunkZ * 4;
        final int[] biomes = new int[100];
        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 10; i++) {
                biomes[i + j * 10] = this.venusBiomes.biomeAt(cellX - 2 + i, cellZ - 2 + j);
            }
        }
        final double[] offsets = noises.heightOffset.sample2d(cellX, cellZ, 5, 5, 2000.0D, 2000.0D);
        final double[] density = noises.density.sample(cellX, 0, cellZ, 5, 33, 5, 684.412D, 684.412D, 684.412D);
        final double[] out = new double[825];
        int index = 0;
        int column = 0;
        for (int k = 0; k < 5; ++k) {
            for (int l = 0; l < 5; ++l) {
                float variation = 0.0F;
                float height = 0.0F;
                float total = 0.0F;
                final int centre = biomes[k + 2 + (l + 2) * 10];
                for (int j1 = -2; j1 <= 2; ++j1) {
                    for (int k1 = -2; k1 <= 2; ++k1) {
                        final int biome = biomes[k + j1 + 2 + (l + k1 + 2) * 10];
                        final float base = BASE_HEIGHT[biome];
                        float weight = PARABOLIC_FIELD[j1 + 2 + (k1 + 2) * 5] / (base + 2.0F);
                        if (base > BASE_HEIGHT[centre]) {
                            weight /= 2.0F;
                        }
                        variation += HEIGHT_VARIATION[biome] * weight;
                        height += base * weight;
                        total += weight;
                    }
                }
                variation = variation / total;
                height = height / total;
                variation = variation * 0.9F + 0.1F;
                height = (height * 4.0F - 1.0F) / 8.0F;
                double offset = offsets[column++] / 4000.0D;
                if (offset < 0.0D) {
                    offset = -offset * 0.3D;
                }
                offset = offset * 3.0D - 2.0D;
                if (offset < 0.0D) {
                    offset = offset / 2.0D;
                    if (offset < -1.0D) {
                        offset = -1.0D;
                    }
                    offset = offset / 1.4D / 2.0D;
                } else {
                    if (offset > 1.0D) {
                        offset = 1.0D;
                    }
                    offset = offset / 8.0D;
                }
                double baseCell = height + offset * 0.2D;
                baseCell = baseCell * 8.5D / 8.0D;
                final double level = 8.5D + baseCell * 4.0D;
                for (int y = 0; y < 33; ++y) {
                    double falloff = (y - level) * 12.0D * 128.0D / 256.0D / variation;
                    if (falloff < 0.0D) {
                        falloff *= 4.0D;
                    }
                    double value = density[index] / 1024.0D - falloff;
                    if (y > 29) {
                        final double top = (y - 29) / 3.0F;
                        value = value * (1.0D - top) + -10.0D * top;
                    }
                    out[index++] = value;
                }
            }
        }
        return out;
    }

    /**
     * ChunkProviderVenus#setBlocksInChunk: the corner densities interpolated through every block; a block is solid
     * where its density beats 20 x the smooth threshold noise of its column. Calls {@code solid} for every solid block.
     */
    private void fillTerrain(Noises noises, int chunkX, int chunkZ, SolidSink solid) {
        final double[] density = densities(noises, chunkX, chunkZ);
        final double[] threshold = new double[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                threshold[x + z * 16] = noises.threshold.eval(chunkX * 16 + x, chunkZ * 16 + z) * 20.0D;
            }
        }
        for (int i = 0; i < 4; ++i) {
            final int j = i * 5;
            final int k = (i + 1) * 5;
            for (int l = 0; l < 4; ++l) {
                final int i1 = (j + l) * 33;
                final int j1 = (j + l + 1) * 33;
                final int k1 = (k + l) * 33;
                final int l1 = (k + l + 1) * 33;
                for (int i2 = 0; i2 < 32; ++i2) {
                    double d1 = density[i1 + i2];
                    double d2 = density[j1 + i2];
                    double d3 = density[k1 + i2];
                    double d4 = density[l1 + i2];
                    final double d5 = (density[i1 + i2 + 1] - d1) * 0.125D;
                    final double d6 = (density[j1 + i2 + 1] - d2) * 0.125D;
                    final double d7 = (density[k1 + i2 + 1] - d3) * 0.125D;
                    final double d8 = (density[l1 + i2 + 1] - d4) * 0.125D;
                    for (int j2 = 0; j2 < 8; ++j2) {
                        double d10 = d1;
                        double d11 = d2;
                        final double d12 = (d3 - d1) * 0.25D;
                        final double d13 = (d4 - d2) * 0.25D;
                        for (int k2 = 0; k2 < 4; ++k2) {
                            final double step = (d11 - d10) * 0.25D;
                            double value = d10 - step;
                            for (int l2 = 0; l2 < 4; ++l2) {
                                value += step;
                                final int x = i * 4 + k2;
                                final int z = l * 4 + l2;
                                if (value > threshold[x + z * 16]) {
                                    solid.accept(x, i2 * 8 + j2, z);
                                }
                            }
                            d10 += d12;
                            d11 += d13;
                        }
                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface SolidSink {
        void accept(int x, int y, int z);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        final BlockState rock = hardRock();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final int minX = chunk.getPos().getMinBlockX();
        final int minZ = chunk.getPos().getMinBlockZ();
        fillTerrain(noises(randomState), chunk.getPos().x, chunk.getPos().z,
                (x, y, z) -> chunk.setBlockState(pos.set(minX + x, y, minZ + z), rock, false));
        return CompletableFuture.completedFuture(chunk);
    }

    /** The height of the terrain (before caves) in each column of a chunk, {@code x + z * 16}. */
    private int[] heights(RandomState randomState, int chunkX, int chunkZ) {
        final long key = ChunkPos.asLong(chunkX, chunkZ);
        int[] heights = this.heightCache.get(key);
        if (heights == null) {
            final int[] top = new int[256];
            fillTerrain(noises(randomState), chunkX, chunkZ, (x, y, z) -> top[x + z * 16] = Math.max(top[x + z * 16], y + 1));
            if (this.heightCache.size() > 1024) {
                this.heightCache.clear();
            }
            this.heightCache.put(key, top);
            heights = top;
        }
        return heights;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return Math.max(heights(randomState, x >> 4, z >> 4)[(x & 15) + (z & 15) * 16], level.getMinBuildHeight());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        final int height = getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        final int minY = level.getMinBuildHeight();
        final BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            final int y = minY + i;
            states[i] = y <= 0 ? Blocks.BEDROCK.defaultBlockState() : y < height ? hardRock() : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(minY, states);
    }

    // ---------------------------------------------------------------- caves and surface

    /**
     * ChunkProviderVenus#generateChunk after the terrain: the caves (MapGenCaveVenus, which only cut hard rock, all there
     * is yet), the lava tubes (MapGenLavaVenus), then each column's biome surface (replaceBlocksForBiome).
     */
    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        carveAndSurface(level.getSeed(), level.getBiomeManager(), randomState, chunk);
    }

    /** The caves, lava tubes and surface of a chunk of terrain, given the world seed and its biome zoom. */
    public void carveAndSurface(long seed, BiomeManager biomes, RandomState randomState, ChunkAccess chunk) {
        final BlockState hard = hardRock();
        new WormCaves((c, pos, state, random) -> state == hard ? Blocks.AIR.defaultBlockState() : null, WormCaves.TUNNELS).generate(seed, chunk);
        new WormCaves(VenusChunkGenerator::lavaTube, WormCaves.LAVA_TUBES).generate(seed, chunk);
        surface(noises(randomState), biomes, chunk);
    }

    /** The biome zoom a world with this seed uses over these biomes (for tests, which have no world region). */
    public BiomeManager biomeManager(long seed) {
        return new BiomeManager((x, y, z) -> this.venusBiomes.getNoiseBiome(x, y, z, null), BiomeManager.obfuscateSeed(seed));
    }

    /**
     * MapGenLavaVenus: soft or hard rock in a lava tube turns to lava, or to Venus Volcanic Rock where it touches air
     * beside or below it (or the edge of the chunk, which 1.12.2 counted as air), so the lava stays sealed in.
     */
    private static BlockState lavaTube(ChunkAccess chunk, BlockPos pos, BlockState state, Random random) {
        if (!state.is(GCBlocks.VENUS_HARD_ROCK.get()) && !state.is(GCBlocks.VENUS_SOFT_ROCK.get())) {
            return null;
        }
        final int x = pos.getX() & 15;
        final int z = pos.getZ() & 15;
        final boolean open = z == 0 || isAir(chunk, pos.north())
                || z == 15 || isAir(chunk, pos.south())
                || x == 15 || isAir(chunk, pos.east())
                || x == 0 || isAir(chunk, pos.west())
                || pos.getY() == 0 || isAir(chunk, pos.below());
        return open ? GCBlocks.VENUS_VOLCANIC_ROCK.get().defaultBlockState() : Blocks.LAVA.defaultBlockState();
    }

    private static boolean isAir(ChunkAccess chunk, BlockPos pos) {
        return chunk.getBlockState(pos).is(Blocks.AIR);
    }

    /**
     * BiomeVenus#generateBiomeTerrainVenus and BiomeGenVenusValley#genTerrainBlocks, column by column (z, then x, as
     * 1.12.2 walked them): from the top, the first rock under air is the surface. Near and above sea level it gets the
     * biome's top block over (surface depth) filler blocks; a little lower, filler only; deep down nothing, except in
     * valleys, whose deep floors are pumice. Bedrock is rolled at every height, as in 1.12.2.
     */
    private void surface(Noises noises, BiomeManager biomes, ChunkAccess chunk) {
        final int chunkX = chunk.getPos().x;
        final int chunkZ = chunk.getPos().z;
        final Random rand = new Random((chunkX * 341873128712L + chunkZ * 132897987541L) ^ noises.surfaceSeed);
        final BlockState hard = hardRock();
        final BlockState soft = GCBlocks.VENUS_SOFT_ROCK.get().defaultBlockState();
        final BlockState pumice = GCBlocks.PUMICE.get().defaultBlockState();
        final BlockState volcanic = GCBlocks.VENUS_VOLCANIC_ROCK.get().defaultBlockState();
        final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                final int worldX = chunkX * 16 + x;
                final int worldZ = chunkZ * 16 + z;
                final boolean valley = biomes.getBiome(pos.set(worldX, SEA_LEVEL, worldZ)).is(GCDimensions.VENUS_VALLEY);
                final BlockState biomeTop = valley ? soft : hard;
                final BlockState biomeFiller = soft;
                BlockState top = biomeTop;
                BlockState filler = biomeFiller;
                final double depthNoise = noises.surfaceDepth.getValue(worldX * 0.0625D, worldZ * 0.0625D, true) * 8.25D;
                final int depth = (int) (depthNoise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
                int remaining = -1;
                for (int y = GEN_DEPTH - 1; y >= 0; --y) {
                    pos.set(worldX, y, worldZ);
                    if (y <= rand.nextInt(5)) {
                        chunk.setBlockState(pos, bedrock, false);
                        continue;
                    }
                    final BlockState state = chunk.getBlockState(pos);
                    if (state.isAir()) {
                        remaining = -1;
                    } else if (state == hard || state == volcanic) {
                        if (remaining == -1) {
                            if (depth <= 0) {
                                top = null;
                                filler = hard;
                            } else if (y >= SEA_LEVEL - 4 && y <= SEA_LEVEL + 1) {
                                top = biomeTop;
                                filler = biomeFiller;
                            }
                            remaining = depth;
                            if (y >= SEA_LEVEL - 1) {
                                // 1.12.2 placed a null top block, which its chunk primer stored as air
                                chunk.setBlockState(pos, top == null ? air : top, false);
                            } else if (y < SEA_LEVEL - 7 - depth) {
                                top = null;
                                filler = hard;
                                if (valley) {
                                    chunk.setBlockState(pos, pumice, false);
                                }
                            } else {
                                chunk.setBlockState(pos, filler, false);
                            }
                        } else if (remaining > 0) {
                            --remaining;
                            chunk.setBlockState(pos, filler, false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Venus's caves and lava tubes are carved in buildSurface, before the surface, as in 1.12.2.
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
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Galacticraft Venus terrain");
    }
}
