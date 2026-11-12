/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.ChunkProviderAsteroids,
 * SpecialAsteroidBlock and SpecialAsteroidBlockHandler). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.BrightAirBlock;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.world.noise.NoiseModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

/**
 * The asteroid belt (1.12.2 ChunkProviderAsteroids): empty space between y 48 and 208 full of asteroids. About one in
 * 800 two-block grid points (more where a billowed density noise is high) is the centre of a 5-25 block ellipsoid,
 * skewed by three noise fields and roughened by a turbulence noise: asteroid rock with a core of rock, ore (aluminium,
 * ilmenite, iron, silicon, diamond) or solid meteoric iron, and half the time a shell of another rock or Dense Ice. One
 * in ten big ones is hollow, with grass, dirt, glowstone, trees, flowers and pools inside, behind a shell of Dense Ice.
 * Single rock and ore blocks float between them. There is no sky light: the sunward (west) faces of the asteroids are
 * lit by Bright Air, brighter where more space is open in front of them.
 *
 * <p>The maths follows 1.12.2, quirks included (noted below). The noises are seeded from the world seed through
 * {@link RandomState}; 1.12.2's position hashes (which asteroid centres there are, their height and size, their core)
 * did not use the seed and still don't.
 */
public final class AsteroidsChunkGenerator extends ChunkGenerator {
    public static final MapCodec<AsteroidsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, AsteroidsChunkGenerator::new));

    private static final int CHUNK_SIZE_Y = 256;
    private static final int MAX_ASTEROID_RADIUS = 25;
    private static final int MIN_ASTEROID_RADIUS = 5;
    private static final int MAX_ASTEROID_SKEW = 8;
    public static final int MIN_ASTEROID_Y = 48;
    public static final int MAX_ASTEROID_Y = CHUNK_SIZE_Y - 48;
    private static final int ASTEROID_CHANCE = 800;
    private static final int ASTEROID_SHELL_CHANCE = 2;
    private static final int MIN_BLOCKS_PER_CHUNK = 50;
    private static final int MAX_BLOCKS_PER_CHUNK = 200;
    private static final int ILMENITE_CHANCE = 400;
    private static final int IRON_CHANCE = 300;
    private static final int ALUMINUM_CHANCE = 250;
    private static final int NOISE_OFFSET_SIZE = 256;
    private static final float MIN_HOLLOW_SIZE = .6F;
    private static final float MAX_HOLLOW_SIZE = .8F;
    private static final int HOLLOW_CHANCE = 10;
    private static final int MIN_RADIUS_FOR_HOLLOW = 15;
    private static final int TREE_CHANCE = 2;
    private static final int TALL_GRASS_CHANCE = 2;
    private static final int FLOWER_CHANCE = 2;
    private static final int WATER_CHANCE = 2;
    private static final int LAVA_CHANCE = 2;
    private static final int GLOWSTONE_CHANCE = 20;

    /** 1.12.2 SpecialAsteroidBlock: a block, its weight and how thick a layer it makes (0-1). */
    private record Special(BlockState state, int probability, double thickness) {
    }

    /** The asteroids that reach into one chunk, as 1.12.2 kept them (for lighting and decoration). */
    private record AsteroidData(boolean hollow, float[] sizeYArray, int xMin, int zMin, int xMax, int zMax, int zSize, int size, int asteroidX,
                                int asteroidY, int asteroidZ) {
    }

    private record Noises(NoiseModule density, NoiseModule turbulence, NoiseModule skewX, NoiseModule skewY, NoiseModule skewZ) {
    }

    /** 1.12.2 WorldGenTrees(false, 2, oak, oak leaves, no vines): oaks 2 to 4 logs tall. */
    private static final TreeConfiguration SMALL_OAK = new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(Blocks.OAK_LOG),
            new StraightTrunkPlacer(2, 2, 0), BlockStateProvider.simple(Blocks.OAK_LEAVES), new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
            new TwoLayersFeatureSize(1, 0, 1)).ignoreVines().build();

    private final Map<RandomState, Noises> noiseCache = Collections.synchronizedMap(new WeakHashMap<>());
    private List<Special> coreBlocks;
    private List<Special> shellBlocks;

    public AsteroidsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private Noises noises(RandomState state) {
        return this.noiseCache.computeIfAbsent(state, s -> {
            final long seed = s.getOrCreateRandomFactory(GalacticraftCore.id("asteroids_terrain")).fromHashOf("terrain").nextLong();
            final Random rand = new Random(seed);
            final NoiseModule density = NoiseModule.billowed(rand.nextLong(), 2, .25F, .009F, .6F);
            final NoiseModule turbulence = NoiseModule.gradient(rand.nextLong(), 1, .2F, .08F, .08F, .08F, .5F);
            // 1.12.2 set one frequency of each skew noise to 0.005 and the others to 1 / size for every asteroid
            final NoiseModule skewX = NoiseModule.gradient(rand.nextLong(), 1, 1, 1, 1, 1, MAX_ASTEROID_SKEW);
            final NoiseModule skewY = NoiseModule.gradient(rand.nextLong(), 1, 1, 1, 1, 1, MAX_ASTEROID_SKEW);
            final NoiseModule skewZ = NoiseModule.gradient(rand.nextLong(), 1, 1, 1, 1, 1, MAX_ASTEROID_SKEW);
            return new Noises(density, turbulence, skewX, skewY, skewZ);
        });
    }

    /** 1.12.2 coreHandler (each block entered "probability" times) and shellHandler. */
    private void initBlocks() {
        if (this.coreBlocks != null) {
            return;
        }
        final List<Special> core = new ArrayList<>();
        add(core, new Special(GCBlocks.ASTEROID_ROCK_2.get().defaultBlockState(), 5, .3));
        add(core, new Special(GCBlocks.ASTEROID_ROCK_1.get().defaultBlockState(), 7, .3));
        add(core, new Special(GCBlocks.ASTEROID_ROCK_0.get().defaultBlockState(), 11, .25));
        add(core, new Special(GCBlocks.ASTEROID_ALUMINUM_ORE.get().defaultBlockState(), 5, .2));
        add(core, new Special(GCBlocks.ILMENITE_ORE.get().defaultBlockState(), 4, .15));
        add(core, new Special(GCBlocks.ASTEROID_IRON_ORE.get().defaultBlockState(), 3, .2));
        add(core, new Special(GCBlocks.SILICON_ORE.get().defaultBlockState(), 2, .15));
        add(core, new Special(GCBlocks.METEORIC_IRON_BLOCK.get().defaultBlockState(), 2, .13));
        add(core, new Special(Blocks.DIAMOND_ORE.defaultBlockState(), 1, .1));
        final List<Special> shell = new ArrayList<>();
        add(shell, new Special(GCBlocks.ASTEROID_ROCK_0.get().defaultBlockState(), 1, .15));
        add(shell, new Special(GCBlocks.ASTEROID_ROCK_1.get().defaultBlockState(), 3, .15));
        add(shell, new Special(GCBlocks.ASTEROID_ROCK_2.get().defaultBlockState(), 1, .15));
        add(shell, new Special(GCBlocks.DENSE_ICE.get().defaultBlockState(), 1, .15));
        this.shellBlocks = shell;
        this.coreBlocks = core;
    }

    private static void add(List<Special> list, Special block) {
        for (int i = 0; i < block.probability(); i++) {
            list.add(block);
        }
    }

    /** 1.12.2 SpecialAsteroidBlockHandler#getBlock: bigger asteroids lean towards the end of the list (ores, meteoric iron, diamond). */
    private static Special pick(List<Special> list, Random rand, int size) {
        final int s = list.size();
        if (s < 10) {
            return list.get(rand.nextInt(s));
        }
        final double r = rand.nextDouble();
        return list.get((int) (s * Math.pow(r, (size + 5) * 0.05D)));
    }

    // ---------------------------------------------------------------- position hashes (no seed, as in 1.12.2)

    private static float randFromPoint(int x, int y, int z) {
        int n = x + z * 57 + y * 571;
        n ^= n << 13;
        n = n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff;
        return 1.0F - n / 1073741824.0F;
    }

    private static float randFromPoint(int x, int z) {
        int n = x + z * 57;
        n ^= n << 13;
        n = n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff;
        return 1.0F - n / 1073741824.0F;
    }

    private static float randFromPointPos(int x, int z) {
        int n = x + z * 57;
        n ^= n << 13;
        n = n * (n * n * 15731 + 789221) + 1376312589 & 0x3fffffff;
        return 1.0F - n / 1073741824.0F;
    }

    private static int clamp(int x, int min, int max) {
        return x < min ? min : Math.min(x, max);
    }

    // ---------------------------------------------------------------- terrain

    /** 1.12.2 generateTerrain: every asteroid centre within three chunks back and two ahead that reaches into this chunk. */
    private List<AsteroidData> generateTerrain(Noises noises, int chunkX, int chunkZ, ChunkAccess chunk, boolean dataOnly, Random chunkRand) {
        initBlocks();
        final List<AsteroidData> asteroids = new ArrayList<>();
        final Random random = new Random();
        final int rangeY = MAX_ASTEROID_Y - MIN_ASTEROID_Y;
        final int rangeSize = MAX_ASTEROID_RADIUS - MIN_ASTEROID_RADIUS;
        for (int i = chunkX - 3; i < chunkX + 3; i++) {
            final int minX = i * 16;
            for (int k = chunkZ - 3; k < chunkZ + 3; k++) {
                final int minZ = k * 16;
                for (int x = minX; x < minX + 16; x += 2) {
                    for (int z = minZ; z < minZ + 16; z += 2) {
                        if (randFromPointPos(x, z) < (noises.density.get(x, z) + .4) / ASTEROID_CHANCE) {
                            random.setSeed(x + z * 3067L);
                            final int y = random.nextInt(rangeY) + MIN_ASTEROID_Y;
                            final int size = random.nextInt(rangeSize) + MIN_ASTEROID_RADIUS;
                            generateAsteroid(noises, random, x, y, z, chunkX << 4, chunkZ << 4, size, chunk, dataOnly, chunkRand, asteroids);
                        }
                    }
                }
            }
        }
        return asteroids;
    }

    private void generateAsteroid(Noises noises, Random rand, int asteroidX, int asteroidY, int asteroidZ, int chunkX, int chunkZ, int size,
                                  ChunkAccess chunk, boolean dataOnly, Random chunkRand, List<AsteroidData> asteroids) {
        final Special core = pick(this.coreBlocks, rand, size);
        Special shell = null;
        if (rand.nextInt(ASTEROID_SHELL_CHANCE) == 0) {
            shell = pick(this.shellBlocks, rand, size);
        }
        boolean hollow = false;
        final float hollowSize = rand.nextFloat() * (MAX_HOLLOW_SIZE - MIN_HOLLOW_SIZE) + MIN_HOLLOW_SIZE;
        if (rand.nextInt(HOLLOW_CHANCE) == 0 && size >= MIN_RADIUS_FOR_HOLLOW) {
            hollow = true;
            shell = new Special(GCBlocks.DENSE_ICE.get().defaultBlockState(), 1, .15);
        }
        final int reach = size + MAX_ASTEROID_SKEW + 2;
        final int xMin = clamp(Math.max(chunkX, asteroidX - reach) - chunkX, 0, 16);
        final int zMin = clamp(Math.max(chunkZ, asteroidZ - reach) - chunkZ, 0, 16);
        final int yMin = asteroidY - reach;
        final int yMax = asteroidY + reach;
        final int xMax = clamp(Math.min(chunkX + 16, asteroidX + reach) - chunkX, 0, 16);
        final int zMax = clamp(Math.min(chunkZ + 16, asteroidZ + reach) - chunkZ, 0, 16);
        final int xSize = xMax - xMin;
        final int ySize = yMax - yMin;
        final int zSize = zMax - zMin;
        if (xSize <= 0 || ySize <= 0 || zSize <= 0) {
            return;
        }
        // Quirk kept from 1.12.2: the noise offsets add the chunk's corner but the arrays start at xMin / zMin.
        final float noiseOffsetX = randFromPoint(asteroidX, asteroidY, asteroidZ) * NOISE_OFFSET_SIZE + chunkX;
        final float noiseOffsetY = randFromPoint(asteroidX * 7, asteroidY * 11, asteroidZ * 13) * NOISE_OFFSET_SIZE;
        final float noiseOffsetZ = randFromPoint(asteroidX * 17, asteroidY * 23, asteroidZ * 29) * NOISE_OFFSET_SIZE + chunkZ;
        final float other = 1F / (size * 2F / 2F);
        final float[] sizeXArray = new float[ySize * zSize];
        final float[] sizeZArray = new float[xSize * ySize];
        final float[] sizeYArray = new float[xSize * zSize];
        for (int x = 0; x < xSize; x++) {
            final int xx = x * zSize;
            final float xxx = x + noiseOffsetX;
            for (int z = 0; z < zSize; z++) {
                sizeYArray[xx + z] = noises.skewY.get(xxx, z + noiseOffsetZ, other, 0.005F);
            }
        }
        asteroids.add(new AsteroidData(hollow, sizeYArray, xMin, zMin, xMax, zMax, zSize, size, asteroidX, asteroidY, asteroidZ));
        if (dataOnly) {
            return;
        }
        for (int y = 0; y < ySize; y++) {
            final int yy = y * zSize;
            final float yyy = y + noiseOffsetY;
            for (int z = 0; z < zSize; z++) {
                sizeXArray[yy + z] = noises.skewX.get(yyy, z + noiseOffsetZ, 0.005F, other);
            }
        }
        for (int x = 0; x < xSize; x++) {
            final int xx = x * ySize;
            final float xxx = x + noiseOffsetX;
            for (int y = 0; y < ySize; y++) {
                sizeZArray[xx + y] = noises.skewZ.get(xxx, y + noiseOffsetY, other, other);
            }
        }
        double shellThickness = 0;
        int terrainY = 0;
        int terrainYY = 0;
        BlockState shellState = null;
        if (shell != null) {
            shellState = shell.state();
            shellThickness = 1.0 - shell.thickness();
        }
        final BlockState coreState = core.state();
        final BlockState rock0 = GCBlocks.ASTEROID_ROCK_0.get().defaultBlockState();
        final BlockState rock1 = GCBlocks.ASTEROID_ROCK_1.get().defaultBlockState();
        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState dirt = Blocks.DIRT.defaultBlockState();
        final BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        final BlockState light = Blocks.GLOWSTONE.defaultBlockState();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = xMax - 1; x >= xMin; x--) {
            final int indexXY = (x - xMin) * ySize - yMin;
            final int indexXZ = (x - xMin) * zSize - zMin;
            final int distanceX = asteroidX - (x + chunkX);
            final float xx = x + chunkX;
            for (int z = zMin; z < zMax; z++) {
                if (hollow) {
                    final float sizeModY = sizeYArray[indexXZ + z];
                    terrainY = terrainHeightFor(sizeModY, asteroidY, size);
                    terrainYY = terrainHeightFor(sizeModY, asteroidY - 1, size);
                }
                float sizeY = size + sizeYArray[indexXZ + z];
                sizeY *= sizeY;
                final int distanceZ = asteroidZ - (z + chunkZ);
                final float zz = z + chunkZ;
                for (int y = yMin; y < yMax; y++) {
                    float dSizeX = distanceX / (size + sizeXArray[(y - yMin) * zSize + z - zMin]);
                    float dSizeZ = distanceZ / (size + sizeZArray[indexXY + y]);
                    dSizeX *= dSizeX;
                    dSizeZ *= dSizeZ;
                    int distanceY = asteroidY - y;
                    distanceY *= distanceY;
                    float distance = dSizeX + distanceY / sizeY + dSizeZ;
                    float distanceAbove = distance;
                    distance += noises.turbulence.get(xx, y, zz);
                    if (hollow && distance <= hollowSize) {
                        distanceAbove += noises.turbulence.get(xx, y + 1, zz);
                        if (distanceAbove <= 1 && (y - 1) == terrainYY) {
                            set(chunk, pos.set(x, y + 1, z), light);
                        }
                    }
                    if (distance <= 1) {
                        final BlockState state;
                        if (hollow && distance <= hollowSize) {
                            state = y == terrainY ? grass : y < terrainY ? dirt : air;
                        } else if (distance <= core.thickness()) {
                            state = rand.nextBoolean() ? coreState : rock0;
                        } else if (shellState != null && distance >= shellThickness) {
                            state = shellState;
                        } else {
                            state = rock1;
                        }
                        set(chunk, pos.set(x, y, z), state);
                    }
                }
            }
        }
        if (hollow) {
            // glowstone dotted over the inside's floor
            for (int x = xMin; x < xMax; x++) {
                final int indexXY = (x - xMin) * ySize - yMin;
                final int indexXZ = (x - xMin) * zSize - zMin;
                int distanceX = asteroidX - (x + chunkX);
                distanceX *= distanceX;
                for (int z = zMin; z < zMax; z++) {
                    float sizeY = size + sizeYArray[indexXZ + z];
                    sizeY *= sizeY;
                    int distanceZ = asteroidZ - (z + chunkZ);
                    distanceZ *= distanceZ;
                    for (int y = yMin; y < yMax; y++) {
                        float sizeX = size + sizeXArray[(y - yMin) * zSize + z - zMin];
                        float sizeZ = size + sizeZArray[indexXY + y];
                        sizeX *= sizeX;
                        sizeZ *= sizeZ;
                        int distanceY = asteroidY - y;
                        distanceY *= distanceY;
                        float distance = distanceX / sizeX + distanceY / sizeY + distanceZ / sizeZ;
                        distance += noises.turbulence.get(x + chunkX, y, z + chunkZ);
                        if (distance <= 1) {
                            final BlockState state = chunk.getBlockState(pos.set(x, y, z));
                            final BlockState above = chunk.getBlockState(pos.set(x, y + 1, z));
                            if (above.isAir() && (state.is(rock0.getBlock()) || state.is(rock1.getBlock()) || state.is(GCBlocks.ASTEROID_ROCK_2.get())
                                    || state.is(Blocks.GRASS_BLOCK)) && chunkRand.nextInt(GLOWSTONE_CHANCE) == 0) {
                                set(chunk, pos.set(x, y, z), light);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void set(ChunkAccess chunk, BlockPos pos, BlockState state) {
        if (pos.getY() >= chunk.getMinBuildHeight() && pos.getY() < chunk.getMaxBuildHeight()) {
            chunk.setBlockState(pos, state, false);
        }
    }

    private static int terrainHeightFor(float yMod, int asteroidY, int asteroidSize) {
        return (int) (asteroidY - asteroidSize / 4 + yMod * 1.5F);
    }

    /** 1.12.2 getTerrainHeightAt: 1 where the point is outside the chunk's part of the asteroid (a quirk kept). */
    private static int terrainHeightAt(int x, int z, AsteroidData data) {
        final int index = (x - data.xMin) * data.zSize - data.zMin;
        if (index < data.sizeYArray.length && index >= 0) {
            return terrainHeightFor(data.sizeYArray[index], data.asteroidY, data.size);
        }
        return 1;
    }

    /**
     * 1.12.2 generateSkylightMap: in front of every west-facing asteroid face, Bright Air glowing 2 to 14, more the more
     * open the four blocks in front are (counted inside this chunk only, as 1.12.2 did).
     */
    private static void lightSunwardFaces(ChunkAccess chunk, List<AsteroidData> asteroids) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (AsteroidData a : asteroids) {
            final int yMin = Math.max(a.asteroidY - a.size, 0);
            final int yMax = Math.min(a.asteroidY + a.size, 255);
            final int xMin = a.xMin == 0 ? 1 : a.xMin;
            for (int x = a.xMax - 1; x >= xMin; x--) {
                for (int z = a.zMin; z < a.zMax; z++) {
                    for (int y = yMin; y < yMax; y++) {
                        if (!air(chunk, pos, x - 1, y, z) || air(chunk, pos, x, y, z)) {
                            continue;
                        }
                        int count = 2;
                        if (x > 1 && air(chunk, pos, x - 2, y, z)) {
                            count += 2;
                        }
                        if (x > 2) {
                            if (air(chunk, pos, x - 3, y, z)) {
                                count += 2;
                            }
                            // 1.12.2 tested the same block above twice
                            if (air(chunk, pos, x - 3, y + 1, z)) {
                                count += 2;
                            }
                            if (z > 0 && air(chunk, pos, x - 3, y, z - 1)) {
                                count++;
                            }
                            if (z < 15 && air(chunk, pos, x - 3, y, z + 1)) {
                                count++;
                            }
                        }
                        if (x > 3) {
                            if (air(chunk, pos, x - 4, y, z)) {
                                count += 2;
                            }
                            if (air(chunk, pos, x - 4, y + 1, z)) {
                                count += 2;
                            }
                            // 1.12.2 counted these two when they were NOT air
                            if (z > 0 && !air(chunk, pos, x - 4, y, z - 1)) {
                                count++;
                            }
                            if (z < 15 && !air(chunk, pos, x - 4, y, z + 1)) {
                                count++;
                            }
                        }
                        count = Math.min(count, 12);
                        chunk.setBlockState(pos.set(x - 1, y, z), GCBlocks.BRIGHT_AIR.get().defaultBlockState().setValue(BrightAirBlock.LEVEL, count + 2), false);
                    }
                }
            }
        }
    }

    /** 1.12.2 tested "instanceof BlockAir"; Bright Air is air too. */
    private static boolean air(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        return chunk.getBlockState(pos.set(x, y, z)).isAir();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        final Noises noises = noises(randomState);
        final int chunkX = chunk.getPos().x;
        final int chunkZ = chunk.getPos().z;
        final Random chunkRand = new Random(chunkX * 341873128712L + chunkZ * 132897987541L);
        final List<AsteroidData> asteroids = generateTerrain(noises, chunkX, chunkZ, chunk, false, chunkRand);
        lightSunwardFaces(chunk, asteroids);
        Heightmap.primeHeightmaps(chunk, java.util.EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    // ---------------------------------------------------------------- decoration (1.12.2 populate)

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        final int chunkX = chunk.getPos().x;
        final int chunkZ = chunk.getPos().z;
        int x = chunkX << 4;
        int z = chunkZ << 4;
        final long worldSeed = level.getSeed();
        final Random rand = new Random(worldSeed);
        final long var7 = rand.nextLong() / 2L * 2L + 1L;
        final long var9 = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed(chunkX * var7 + chunkZ * var9 ^ worldSeed);
        final Noises noises = noises(level.getLevel().getChunkSource().randomState());
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Half the chunks get small floating blocks (in alternate 16-block layers)
        if (rand.nextBoolean()) {
            final double density = noises.density.get(chunkX * 16, chunkZ * 16) * 0.54;
            final double numOfBlocks = Math.max(0.4D, Math.min(1.0D, randFromPoint(chunkX, chunkZ))) * MAX_BLOCKS_PER_CHUNK * density + MIN_BLOCKS_PER_CHUNK;
            final int y0 = rand.nextInt(2);
            final int yRange = MAX_ASTEROID_Y - MIN_ASTEROID_Y;
            x += 4;
            z += 4;
            for (int i = 0; i < numOfBlocks; i++) {
                final int y = rand.nextInt(yRange) + MIN_ASTEROID_Y;
                if (y0 == (y / 16) % 2) {
                    final int px = x + rand.nextInt(16);
                    final int pz = z + rand.nextInt(16);
                    BlockState state = GCBlocks.ASTEROID_ROCK_1.get().defaultBlockState();
                    if (rand.nextInt(ILMENITE_CHANCE) == 0) {
                        state = GCBlocks.ILMENITE_ORE.get().defaultBlockState();
                    } else if (rand.nextInt(IRON_CHANCE) == 0) {
                        state = GCBlocks.ASTEROID_IRON_ORE.get().defaultBlockState();
                    } else if (rand.nextInt(ALUMINUM_CHANCE) == 0) {
                        state = GCBlocks.ASTEROID_ALUMINUM_ORE.get().defaultBlockState();
                    }
                    level.setBlock(pos.set(px, y, pz), state, 2);
                }
            }
            x -= 4;
            z -= 4;
        }
        // The insides of hollow asteroids: trees, grass, flowers, lava and water
        final List<AsteroidData> asteroids = generateTerrain(noises, chunkX, chunkZ, null, true, null);
        rand.setSeed(chunkX * var7 + chunkZ * var9 ^ worldSeed);
        final RandomSource random = RandomSource.create(chunkX * var7 + chunkZ * var9 ^ worldSeed);
        for (AsteroidData asteroid : asteroids) {
            if (!asteroid.hollow) {
                continue;
            }
            boolean treesDone = false;
            if (rand.nextInt(TREE_CHANCE) == 0) {
                rand.nextInt(3); // 1.12.2 picked a tree type and then always used oak
                for (int tries = 0; tries < 5; tries++) {
                    final int i = rand.nextInt(16) + x + 8;
                    final int k = rand.nextInt(16) + z + 8;
                    final BlockPos at = new BlockPos(i, terrainHeightAt(i - x, k - z, asteroid), k);
                    if (level.getBlockState(at.below()).is(BlockTags.DIRT) && Feature.TREE.place(SMALL_OAK, level, this, random, at)) {
                        break;
                    }
                }
                treesDone = true;
            }
            if (!treesDone || rand.nextInt(TALL_GRASS_CHANCE) == 0) {
                final int i = rand.nextInt(16) + x + 8;
                final int k = rand.nextInt(16) + z + 8;
                scatter(level, rand, new BlockPos(i, terrainHeightAt(i - x, k - z, asteroid), k), Blocks.SHORT_GRASS.defaultBlockState(), 128);
            }
            if (rand.nextInt(FLOWER_CHANCE) == 0) {
                final int i = rand.nextInt(16) + x + 8;
                final int k = rand.nextInt(16) + z + 8;
                // 1.12.2 red flowers 2, 4, 5 and 7
                final BlockState[] flowers = {Blocks.ALLIUM.defaultBlockState(), Blocks.RED_TULIP.defaultBlockState(), Blocks.ORANGE_TULIP.defaultBlockState(),
                        Blocks.PINK_TULIP.defaultBlockState()};
                scatter(level, rand, new BlockPos(i, terrainHeightAt(i - x, k - z, asteroid), k), flowers[rand.nextInt(flowers.length)], 64);
            }
            if (rand.nextInt(LAVA_CHANCE) == 0) {
                final int i = rand.nextInt(16) + x + 8;
                final int k = rand.nextInt(16) + z + 8;
                lake(level, random, new BlockPos(i, terrainHeightAt(i - x, k - z, asteroid), k), Blocks.LAVA.defaultBlockState());
            }
            if (rand.nextInt(WATER_CHANCE) == 0) {
                final int i = rand.nextInt(16) + x + 8;
                final int k = rand.nextInt(16) + z + 8;
                lake(level, random, new BlockPos(i, terrainHeightAt(i - x, k - z, asteroid), k), Blocks.WATER.defaultBlockState());
            }
        }
    }

    /** 1.12.2 WorldGenTallGrass / WorldGenFlowers: tries around the spot, keeping those that can grow there. */
    private static void scatter(WorldGenLevel level, Random rand, BlockPos origin, BlockState plant, int tries) {
        for (int i = 0; i < tries; i++) {
            final BlockPos at = origin.offset(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (level.ensureCanWrite(at) && level.isEmptyBlock(at) && plant.canSurvive(level, at)) {
                level.setBlock(at, plant, 2);
            }
        }
    }

    /** 1.12.2 WorldGenLakes, here vanilla's lake feature. */
    private void lake(WorldGenLevel level, RandomSource random, BlockPos at, BlockState fluid) {
        if (at.getY() < 5) {
            return;
        }
        // 1.12.2 lakes were centred on the spot, vanilla's start at it
        Feature.LAKE.place(new LakeFeature.Configuration(BlockStateProvider.simple(fluid), BlockStateProvider.simple(GCBlocks.ASTEROID_ROCK_1.get())),
                level, this, random, at.offset(-8, 0, -8));
    }

    // ---------------------------------------------------------------- where the asteroids are

    /** An asteroid's centre and radius. */
    public record Asteroid(int x, int y, int z, int size) {
    }

    /**
     * Every asteroid centred within {@code radius} blocks (in x and z) of a point. 1.12.2 WorldProviderAsteroids kept a
     * saved list, filled in as chunks generated; the centres come from position hashes and the density noise alone, so
     * here they are worked out directly, generated or not.
     */
    public List<Asteroid> asteroidsNear(RandomState state, int x, int z, int radius) {
        final Noises noises = noises(state);
        final List<Asteroid> found = new ArrayList<>();
        final Random random = new Random();
        final int rangeY = MAX_ASTEROID_Y - MIN_ASTEROID_Y;
        final int rangeSize = MAX_ASTEROID_RADIUS - MIN_ASTEROID_RADIUS;
        // the centres sit on even coordinates, like generateTerrain's 2-block steps from each chunk's corner
        for (int ax = (x - radius) & ~1; ax <= x + radius; ax += 2) {
            for (int az = (z - radius) & ~1; az <= z + radius; az += 2) {
                if (randFromPointPos(ax, az) < (noises.density.get(ax, az) + .4) / ASTEROID_CHANCE) {
                    random.setSeed(ax + az * 3067L);
                    final int y = random.nextInt(rangeY) + MIN_ASTEROID_Y;
                    found.add(new Asteroid(ax, y, az, random.nextInt(rangeSize) + MIN_ASTEROID_RADIUS));
                }
            }
        }
        return found;
    }

    // ---------------------------------------------------------------- the rest

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return CHUNK_SIZE_Y;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    /** 1.12.2 getAverageGroundLevel: 96. */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return 96;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        final BlockState[] states = new BlockState[level.getHeight()];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Galacticraft asteroid belt");
    }
}
