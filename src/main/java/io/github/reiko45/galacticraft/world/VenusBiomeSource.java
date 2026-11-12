/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.world.gen.BiomeProviderVenus and
 * world.gen.layer.GenLayerVenus / GenLayerVenusBiomes). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Venus's three biomes (1.12.2 BiomeProviderVenus): every 64 x 64 block cell is Venus Flat, Venus Mountain or Venus
 * Valley at random, and four rounds of 1.12.2's GenLayerZoom (each halving the cells, and settling each new cell from
 * its neighbours) break them up into ragged regions at 4-block resolution, the resolution 1.21 stores biomes at; 1.21's
 * biome zoom then blurs their edges per block as 1.12.2's GenLayerVoronoiZoom did.
 *
 * <p>The layers are seeded from the world seed exactly as 1.12.2's were. 1.21 biome sources are not told the seed, so
 * {@link VenusChunkGenerator} passes it in when its world is created ({@link #setSeed}).
 */
public final class VenusBiomeSource extends BiomeSource {
    public static final MapCodec<VenusBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("flat").forGetter(source -> source.flat),
            Biome.CODEC.fieldOf("mountain").forGetter(source -> source.mountain),
            Biome.CODEC.fieldOf("valley").forGetter(source -> source.valley)
    ).apply(instance, VenusBiomeSource::new));

    /** Biome ids as the layers produce them: 1.12.2 VenusModule#setBiomeInfo order. */
    public static final int FLAT = 0;
    public static final int MOUNTAIN = 1;
    public static final int VALLEY = 2;
    /** 1.12.2 GenLayerVenus#createWorld: the four GenLayerZoom seeds. */
    private static final long[] ZOOM_SEEDS = {1000L, 1001L, 1002L, 1003L};
    /** Cached regions of REGION x REGION quart cells. */
    private static final int REGION = 16;

    private final Holder<Biome> flat;
    private final Holder<Biome> mountain;
    private final Holder<Biome> valley;
    private volatile Layers layers = new Layers(0L);
    private final Map<Long, int[]> regions = new ConcurrentHashMap<>();

    public VenusBiomeSource(Holder<Biome> flat, Holder<Biome> mountain, Holder<Biome> valley) {
        this.flat = flat;
        this.mountain = mountain;
        this.valley = valley;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.flat, this.mountain, this.valley);
    }

    /** Seeds the layers from the world seed (and forgets any biomes worked out before). */
    public void setSeed(long seed) {
        if (this.layers.seed != seed) {
            this.layers = new Layers(seed);
            this.regions.clear();
        }
    }

    public long seed() {
        return this.layers.seed;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return switch (biomeAt(x, z)) {
            case MOUNTAIN -> this.mountain;
            case VALLEY -> this.valley;
            default -> this.flat;
        };
    }

    /** Which of the three biomes (FLAT, MOUNTAIN or VALLEY) a quart cell (4 x 4 blocks) is. */
    public int biomeAt(int quartX, int quartZ) {
        final int rx = Math.floorDiv(quartX, REGION);
        final int rz = Math.floorDiv(quartZ, REGION);
        final long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        int[] region = this.regions.get(key);
        if (region == null) {
            if (this.regions.size() > 4096) {
                this.regions.clear();
            }
            region = this.layers.biomes(rx * REGION, rz * REGION, REGION, REGION);
            this.regions.put(key, region);
        }
        return region[(quartX - rx * REGION) + (quartZ - rz * REGION) * REGION];
    }

    /**
     * 1.12.2's GenLayer stack for Venus: GenLayerVenusBiomes, then GenLayerZoom 1000 to 1003, each with the world seed
     * mixed into its own (GenLayer#initWorldGenSeed).
     */
    private static final class Layers {
        final long seed;
        final long baseSeed;
        final long[] zoomSeeds = new long[ZOOM_SEEDS.length];

        Layers(long seed) {
            this.seed = seed;
            this.baseSeed = worldGenSeed(seed, seed);
            for (int i = 0; i < ZOOM_SEEDS.length; i++) {
                this.zoomSeeds[i] = worldGenSeed(seed, ZOOM_SEEDS[i]);
            }
        }

        /** The output of the last zoom over an area, indexed {@code x + z * width}. */
        int[] biomes(int x, int z, int width, int depth) {
            return zoom(ZOOM_SEEDS.length - 1, x, z, width, depth);
        }

        /** GenLayerVenusBiomes: one of the three at random per cell. */
        private int[] base(int x, int z, int width, int depth) {
            final int[] out = new int[width * depth];
            final Random random = new Random(this.baseSeed);
            for (int k = 0; k < depth; k++) {
                for (int i = 0; i < width; i++) {
                    random.at(x + i, z + k);
                    out[i + k * width] = random.nextInt(3);
                }
            }
            return out;
        }

        /**
         * GenLayerZoom: each parent cell becomes four; the corner keeps it, the cells beside it take it or its east /
         * south neighbour at random, and the far cell takes the most common of the four (or one at random).
         */
        private int[] zoom(int level, int x, int z, int width, int depth) {
            final int parentX = x >> 1;
            final int parentZ = z >> 1;
            final int parentWidth = (width >> 1) + 2;
            final int parentDepth = (depth >> 1) + 2;
            final int[] parent = level == 0 ? base(parentX, parentZ, parentWidth, parentDepth)
                    : zoom(level - 1, parentX, parentZ, parentWidth, parentDepth);
            final int zoomedWidth = (parentWidth - 1) << 1;
            final int zoomedDepth = (parentDepth - 1) << 1;
            final int[] zoomed = new int[zoomedWidth * zoomedDepth];
            final Random random = new Random(this.zoomSeeds[level]);
            for (int k = 0; k < parentDepth - 1; k++) {
                int index = (k << 1) * zoomedWidth;
                int corner = parent[k * parentWidth];
                int south = parent[(k + 1) * parentWidth];
                for (int i = 0; i < parentWidth - 1; i++) {
                    random.at((i + parentX) << 1, (k + parentZ) << 1);
                    final int east = parent[i + 1 + k * parentWidth];
                    final int southEast = parent[i + 1 + (k + 1) * parentWidth];
                    zoomed[index] = corner;
                    zoomed[index++ + zoomedWidth] = random.pick(corner, south);
                    zoomed[index] = random.pick(corner, east);
                    zoomed[index++ + zoomedWidth] = random.modeOrRandom(corner, east, south, southEast);
                    corner = east;
                    south = southEast;
                }
            }
            final int[] out = new int[width * depth];
            for (int k = 0; k < depth; k++) {
                System.arraycopy(zoomed, (k + (z & 1)) * zoomedWidth + (x & 1), out, k * width, width);
            }
            return out;
        }

        /** GenLayer: the layer's own seed scrambled three times, then the world seed mixed in three times. */
        private static long worldGenSeed(long worldSeed, long layerSeed) {
            long base = layerSeed;
            for (int i = 0; i < 3; i++) {
                base = LinearCongruentialGenerator.next(base, layerSeed);
            }
            long seed = worldSeed;
            for (int i = 0; i < 3; i++) {
                seed = LinearCongruentialGenerator.next(seed, base);
            }
            return seed;
        }
    }

    /** GenLayer's per-cell random (initChunkSeed and nextInt). */
    private static final class Random {
        private final long worldGenSeed;
        private long chunkSeed;

        Random(long worldGenSeed) {
            this.worldGenSeed = worldGenSeed;
        }

        void at(long x, long z) {
            long seed = this.worldGenSeed;
            seed = LinearCongruentialGenerator.next(seed, x);
            seed = LinearCongruentialGenerator.next(seed, z);
            seed = LinearCongruentialGenerator.next(seed, x);
            this.chunkSeed = LinearCongruentialGenerator.next(seed, z);
        }

        int nextInt(int bound) {
            int value = (int) ((this.chunkSeed >> 24) % bound);
            if (value < 0) {
                value += bound;
            }
            this.chunkSeed = LinearCongruentialGenerator.next(this.chunkSeed, this.worldGenSeed);
            return value;
        }

        int pick(int... values) {
            return values[nextInt(values.length)];
        }

        /** GenLayer#selectModeOrRandom. */
        int modeOrRandom(int a, int b, int c, int d) {
            if (b == c && c == d) {
                return b;
            } else if (a == b && a == c) {
                return a;
            } else if (a == b && a == d) {
                return a;
            } else if (a == c && a == d) {
                return a;
            } else if (a == b && c != d) {
                return a;
            } else if (a == c && b != d) {
                return a;
            } else if (a == d && b != c) {
                return a;
            } else if (b == c && a != d) {
                return b;
            } else if (b == d && a != c) {
                return b;
            } else {
                return c == d && a != b ? c : pick(a, b, c, d);
            }
        }
    }
}
