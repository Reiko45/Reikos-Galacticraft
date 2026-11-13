/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.util.MapUtil, MapGen and ColorUtil#lighten).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.MapNetwork;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 MapUtil: pictures of the Overworld made from its own terrain generator, in the background. The Earth's picture
 * is a 192 x 48 strip round 0, 0, a pixel for every 8 blocks, recoloured with the Earth icon's own colours (the Moon sky,
 * the galaxy map and the Display Screens show it); a Display Screen in the Overworld also gets a map of the 352 blocks
 * round it, a block a pixel, in 1.12.2's biome colours. Each sample is a biome (1.12.2's numbering, which the colour
 * table follows) and a ground height.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class OverworldMap {
    /** 1.12.2 OVERWORLD_TEXTURE_WIDTH and _HEIGHT. */
    public static final int WIDTH = 192;
    public static final int HEIGHT = 48;
    /** Blocks a pixel of the strip covers (1.12.2: two pixels a chunk). */
    public static final int SCALE = 8;
    /** 1.12.2 SIZE_STD2: a Display Screen's local map, a block a pixel. */
    public static final int LOCAL = 352;
    /** The local map is sampled every 4 blocks (a biome cell) and its heights smoothed between. */
    public static final int LOCAL_STEP = 4;
    public static final int LOCAL_CELLS = LOCAL / LOCAL_STEP;
    /** Local maps are only made for screens this close to the player asking. */
    public static final int LOCAL_REACH = 128;
    private static final int LOCAL_CACHE = 16;
    /** How far above the quick estimate the ground search starts (crags rise up to some 30 blocks above it). */
    private static final int CLIMB = 32;
    private static final int GROUND_STEP = 4;
    private static final int OCEAN_HEIGHT = 63;
    private static final int DEEP_OCEAN = 56;

    /** 1.12.2 setupColours, by its biome ids: main colour, second colour, percent of pixels in the second. */
    private static final int[][] COLOURS = {
            {MapColor.WATER.col, 0, 0},                 // 0 ocean
            {0x497436, 0, 0},                           // 1 plains
            {0xd4cd98, MapColor.PLANT.col, 3},          // 2 desert (cactus)
            {0x4d654c, MapColor.STONE.col, 15},         // 3 extreme hills
            {0x3c7521, 0x295416, 45},                   // 4 forest
            {0x627e61, 0x172a17, 18},                   // 5 taiga
            {0x43541b, 0x111309, 25},                   // 6 swampland
            {0x497436, 0, 0},                           // 7 river
            {0, 0, 0},                                  // 8 hell
            {0, 0, 0},                                  // 9 the end
            {MapColor.ICE.col, 0, 0},                   // 10 frozen ocean
            {MapColor.ICE.col, 0, 0},                   // 11 frozen river
            {MapColor.SNOW.col, 0x497436, 3},           // 12 ice plains
            {MapColor.SNOW.col, MapColor.ICE.col, 5},   // 13 ice mountains
            {0x63565f, 0x7c1414, 10},                   // 14 mushroom island
            {0x6a6066, 0, 0},                           // 15 mushroom island shore
            {MapColor.SAND.col, 0, 0},                  // 16 beach
            {0xd4cd98, 0, 0},                           // 17 desert hills
            {0x3c7521, 0x295416, 35},                   // 18 forest hills
            {0x627e61, 0x172a17, 14},                   // 19 taiga hills
            {0x4d654c, 0x497436, 50},                   // 20 extreme hills edge
            {0x176c03, 0x0f4502, 25},                   // 21 jungle
            {0x176c03, 0x0f4502, 25},                   // 22 jungle hills
            {0x176c03, 0x0f4502, 25},                   // 23 jungle edge
            {0x2f2fd4, 0, 0},                           // 24 deep ocean
            {MapColor.STONE.col, 0, 0},                 // 25 stone beach
            {MapColor.SAND.col, MapColor.SNOW.col, 75}, // 26 cold beach
            {0x516b36, 0x497436, 65},                   // 27 birch forest
            {0x516b36, 0x497436, 55},                   // 28 birch forest hills
            {0x9c2424, 0x1e2e18, 98},                   // 29 roofed forest
            {MapColor.SNOW.col, 0x172a17, 12},          // 30 cold taiga
            {MapColor.SNOW.col, 0x172a17, 12},          // 31 cold taiga hills
            {0x172a17, 0x6e4e35, 12},                   // 32 mega taiga
            {0x172a17, 0x6e4e35, 12},                   // 33 mega taiga hills
            {0x7b7978, 0x497436, 10},                   // 34 extreme hills+
            {0x565529, 0x262304, 20},                   // 35 savanna
            {0x565529, 0x262304, 14},                   // 36 savanna plateau
            {0xa0521f, 0x712f23, 14},                   // 37 mesa
            {0xa0521f, 0x712f23, 17},                   // 38 mesa plateau F
            {0xa0521f, 0x712f23, 20}};                  // 39 mesa plateau

    /**
     * 1.12.2 mapped on a thread of its own (MapUtil threadCurrentMap): one low-priority thread, so mapping never holds up
     * the world's own chunk generation.
     */
    private static final ExecutorService MAPPER = Executors.newSingleThreadExecutor(task -> {
        final Thread thread = new Thread(task, "Galacticraft map");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    /** Made once per server, on the first request. */
    private static CompletableFuture<byte[]> strip;
    private static final Map<Long, CompletableFuture<byte[]>> LOCAL_MAPS = new LinkedHashMap<>(LOCAL_CACHE, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CompletableFuture<byte[]>> eldest) {
            return size() > LOCAL_CACHE;
        }
    };

    private OverworldMap() {
    }

    // ------------------------------------------------------------------ server

    /**
     * 1.12.2 S_REQUEST_OVERWORLD_IMAGE: the strip goes to the player now if made, else once it is. An empty strip means
     * there is none to make (an Overworld without a noise generator, as 1.12.2 skipped flat worlds).
     */
    public static void requestStrip(ServerPlayer player) {
        strip(player.server).thenAcceptAsync(data -> {
            if (!player.hasDisconnected()) {
                MapNetwork.send(player, new MapNetwork.MapImage(MapNetwork.STRIP, BlockPos.ZERO, data));
            }
        }, player.server);
    }

    /** The strip's samples, started on the first call (1.12.2 makeOverworldTexture). */
    public static CompletableFuture<byte[]> strip(MinecraftServer server) {
        if (strip == null) {
            strip = sampleAsync(server.overworld(), -WIDTH * SCALE / 2, -HEIGHT * SCALE / 2, WIDTH, HEIGHT, SCALE);
        }
        return strip;
    }

    /** 1.12.2 S_REQUEST_MAP_IMAGE: the local map round a Display Screen in the Overworld, centred on its chunk. */
    public static void requestLocal(ServerPlayer player, BlockPos screen) {
        if (!player.level().dimension().equals(Level.OVERWORLD) || !screen.closerToCenterThan(player.position(), LOCAL_REACH)) {
            return;
        }
        final ChunkPos chunk = new ChunkPos(screen);
        final BlockPos centre = new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
        final CompletableFuture<byte[]> map = LOCAL_MAPS.computeIfAbsent(chunk.toLong(), key -> sampleAsync(player.server.overworld(),
                centre.getX() - LOCAL / 2, centre.getZ() - LOCAL / 2, LOCAL_CELLS, LOCAL_CELLS, LOCAL_STEP));
        map.thenAcceptAsync(data -> {
            if (!player.hasDisconnected()) {
                MapNetwork.send(player, new MapNetwork.MapImage(MapNetwork.LOCAL, centre, data));
            }
        }, player.server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        override = null;
        strip = null;
        LOCAL_MAPS.clear();
    }

    /**
     * Map this generator instead of the Overworld's own (the screenshot tour's world is flat, which has no picture), or
     * null for the Overworld's again; forgets the pictures made so far.
     */
    public static void mapWith(@Nullable ChunkGenerator generator, @Nullable RandomState random) {
        override = generator == null || random == null ? null : new Generator(generator, random);
        strip = null;
        LOCAL_MAPS.clear();
    }

    private record Generator(ChunkGenerator generator, RandomState random) {
    }

    @Nullable
    private static Generator override;

    private static CompletableFuture<byte[]> sampleAsync(ServerLevel level, int x0, int z0, int width, int height, int step) {
        final Generator use = override != null ? override : new Generator(level.getChunkSource().getGenerator(), level.getChunkSource().randomState());
        if (!(use.generator() instanceof NoiseBasedChunkGenerator generator)) {
            return CompletableFuture.completedFuture(new byte[0]);
        }
        final RandomState random = use.random();
        return CompletableFuture.supplyAsync(() -> {
                    final long start = System.nanoTime();
                    final byte[] data = sample(generator, random, x0, z0, width, height, step);
                    GalacticraftCore.LOGGER.debug("Mapped {} x {} of the Overworld in {} ms", width, height, (System.nanoTime() - start) / 1_000_000L);
                    return data;
                }, MAPPER)
                .exceptionally(error -> {
                    GalacticraftCore.LOGGER.error("Galacticraft could not map the Overworld", error);
                    return new byte[0];
                });
    }

    /**
     * 1.12.2 MapGen: for each pixel (x-major, as 1.12.2 stored them) the biome and the ground height at its middle, from
     * the generator alone, so nothing needs to be loaded.
     */
    public static byte[] sample(NoiseBasedChunkGenerator generator, RandomState random, int x0, int z0, int width, int height, int step) {
        final byte[] data = new byte[width * height * 2];
        final BiomeSource biomes = generator.getBiomeSource();
        final Climate.Sampler climate = random.sampler();
        final int sea = generator.getSeaLevel();
        final NoiseSettings noise = generator.generatorSettings().value().noiseSettings();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                final int bx = x0 + x * step + step / 2;
                final int bz = z0 + z * step + step / 2;
                final int ground = ground(random, noise, bx, bz);
                final Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(bx), QuartPos.fromBlock(Math.max(ground, sea)),
                        QuartPos.fromBlock(bz), climate);
                final int index = (x * height + z) * 2;
                data[index] = (byte) legacyBiome(biome);
                data[index + 1] = (byte) Mth.clamp(ground, 0, 255);
            }
        }
        return data;
    }

    /**
     * The ground height: the top of the first solid block from the sky, as the generator's own OCEAN_FLOOR_WG height
     * (1.12.2 MapGen likewise worked heights out from the terrain noise, with nothing generated). The generator scans
     * down from the top of the world; this starts a little above its quick estimate (the terrain without crags or caves,
     * as NoiseChunk#computePreliminarySurfaceLevel finds it) and steps down the finished terrain's density 4 blocks at a
     * time, some six times quicker.
     */
    public static int ground(RandomState random, NoiseSettings noise, int x, int z) {
        final int step = noise.getCellHeight();
        final int top = noise.minY() + noise.height() - 1;
        final DensityFunction quick = random.router().initialDensityWithoutJaggedness();
        int estimate = noise.minY();
        for (int y = top; y >= noise.minY(); y -= step) {
            if (density(quick, x, y, z) > 0.0D) {
                estimate = y;
                break;
            }
        }
        final DensityFunction terrain = random.router().finalDensity();
        int y = Math.min(top, estimate + CLIMB);
        double above = density(terrain, x, y, z);
        while (above > 0.0D && y < top) {
            y = Math.min(top, y + CLIMB);
            above = density(terrain, x, y, z);
        }
        for (y -= GROUND_STEP; y >= noise.minY(); y -= GROUND_STEP) {
            final double value = density(terrain, x, y, z);
            if (value > 0.0D) {
                // solid here, air GROUND_STEP above: the ground's top is where the density crosses 0, and the height the
                // first block above it (as the generator's heights are)
                return y + 1 + (int) Math.floor(GROUND_STEP * value / (value - above));
            }
            above = value;
        }
        return noise.minY();
    }

    private static double density(DensityFunction function, int x, int y, int z) {
        return function.compute(new DensityFunction.SinglePointContext(x, y, z));
    }

    /** A biome as the 1.12.2 biome its colours follow; biomes 1.12.2 never had go by their nearest kin, or by their tags. */
    public static int legacyBiome(Holder<Biome> biome) {
        final String path = biome.unwrapKey().filter(key -> key.location().getNamespace().equals("minecraft"))
                .map(key -> key.location().getPath()).orElse("");
        return switch (path) {
            case "ocean", "lukewarm_ocean", "warm_ocean", "cold_ocean" -> 0;
            case "plains", "sunflower_plains", "meadow" -> 1;
            case "desert" -> 2;
            case "windswept_hills", "windswept_gravelly_hills", "stony_peaks" -> 3;
            case "forest", "flower_forest", "cherry_grove" -> 4;
            case "taiga" -> 5;
            case "swamp", "mangrove_swamp" -> 6;
            case "river" -> 7;
            case "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas" -> 8;
            case "the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens", "the_void" -> 9;
            case "frozen_ocean", "deep_frozen_ocean" -> 10;
            case "frozen_river" -> 11;
            case "snowy_plains", "ice_spikes" -> 12;
            case "snowy_slopes", "frozen_peaks", "jagged_peaks" -> 13;
            case "mushroom_fields" -> 14;
            case "beach" -> 16;
            case "jungle", "bamboo_jungle" -> 21;
            case "sparse_jungle" -> 23;
            case "deep_ocean", "deep_lukewarm_ocean", "deep_cold_ocean" -> 24;
            case "stony_shore" -> 25;
            case "snowy_beach" -> 26;
            case "birch_forest" -> 27;
            case "old_growth_birch_forest" -> 28;
            case "dark_forest" -> 29;
            case "snowy_taiga", "grove" -> 30;
            case "old_growth_pine_taiga", "old_growth_spruce_taiga" -> 32;
            case "windswept_forest" -> 34;
            case "savanna" -> 35;
            case "savanna_plateau", "windswept_savanna" -> 36;
            case "badlands", "eroded_badlands" -> 37;
            case "wooded_badlands" -> 38;
            default -> byTags(biome);
        };
    }

    private static int byTags(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) {
            return 24;
        }
        if (biome.is(BiomeTags.IS_OCEAN)) {
            return 0;
        }
        if (biome.is(BiomeTags.IS_RIVER)) {
            return 7;
        }
        if (biome.is(BiomeTags.IS_BEACH)) {
            return 16;
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return 37;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return 21;
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return 35;
        }
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return 5;
        }
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) {
            return 3;
        }
        return biome.is(BiomeTags.IS_FOREST) ? 4 : 1;
    }

    // ------------------------------------------------------------------ colours

    /**
     * 1.12.2 MapUtil: below sea level everything but deserts and frozen oceans is water (ponds, lakes and rivers too),
     * deep water below 56; then convertBiomeColour.
     */
    public static int colour(int biome, int height, RandomSource random) {
        if (height < OCEAN_HEIGHT && biome != 2 && biome != 10) {
            biome = 0;
        }
        if (height < DEEP_OCEAN && biome == 0) {
            biome = 24;
        }
        return biomeColour(biome, height, random);
    }

    /** 1.12.2 convertBiomeColour: the biome's colour (some pixels its second), snow on high hills, lighter the higher. */
    public static int biomeColour(int biome, int height, RandomSource random) {
        final int[] entry = COLOURS[Mth.clamp(biome, 0, COLOURS.length - 1)];
        int rv = entry[2] > 0 && random.nextInt(100) < entry[2] ? entry[1] : entry[0];
        if (rv == 0x9c2424 && random.nextInt(2) == 0) {
            rv = 0xbfa384;
        }
        if (height < OCEAN_HEIGHT) {
            return rv;
        }
        if (height > 92 && (biome == 3 || biome == 20 || biome == 31 || biome == 33 || biome == 34) && random.nextInt(8) > 98 - height) {
            rv = MapColor.SNOW.col;
        }
        return lighten(rv, (height - 68.0F) / 94.0F);
    }

    /** 1.12.2 ColorUtil#lighten (a negative factor darkens). */
    public static int lighten(int colour, float factor) {
        final int r = Math.min(255, (int) ((colour >> 16 & 255) * (1.0F + factor)));
        final int g = Math.min(255, (int) ((colour >> 8 & 255) * (1.0F + factor)));
        final int b = Math.min(255, (int) ((colour & 255) * (1.0F + factor)));
        return r << 16 | g << 8 | b;
    }

    /** The strip in plain map colours (RGB, row-major WIDTH x HEIGHT). */
    public static int[] stripColours(byte[] strip, RandomSource random) {
        final int[] image = new int[WIDTH * HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < HEIGHT; z++) {
                final int index = (x * HEIGHT + z) * 2;
                image[z * WIDTH + x] = colour(strip[index] & 255, strip[index + 1] & 255, random);
            }
        }
        return image;
    }

    /**
     * 1.12.2 convertTo12pxTexture: the strip cut into 4 x 4 tiles, ranked from bluest to greenest, and every four in turn
     * painted with the next of 144 colours picked from the Earth icon (palette: ARGB, row-major, at least 48 wide and
     * tall), ranked the same way: the world's own land and sea in the Earth icon's colours. RGB out, WIDTH x HEIGHT.
     */
    public static int[] earthPicture(int[] image, int[] palette, int paletteWidth) {
        final int[] result = new int[WIDTH * HEIGHT];
        final TreeMap<Integer, Integer> tiles = new TreeMap<>();
        final TreeMap<Integer, Integer> colours = new TreeMap<>();
        int count = 0;
        for (int x = 0; x < WIDTH; x += 4) {
            for (int z = 0; z < HEIGHT; z += 4) {
                int g = 0;
                int b = 0;
                for (int xx = 0; xx < 4; xx++) {
                    for (int zz = 0; zz < 4; zz++) {
                        final int col = image[(z + zz) * WIDTH + x + xx];
                        g += col >> 8 & 255;
                        b += col & 255;
                    }
                }
                while (tiles.containsKey(g - b)) {
                    g++;
                }
                tiles.put(g - b, count);
                if (x < HEIGHT) {
                    final int col = palette[(z + 1) * paletteWidth + x + 1];
                    g = col >> 8 & 255;
                    b = col & 255;
                    while (colours.containsKey(g - b)) {
                        g++;
                    }
                    colours.put(g - b, col & 0xFFFFFF);
                }
                count++;
            }
        }
        final Iterator<Integer> colour = colours.keySet().iterator();
        final Iterator<Integer> tile = tiles.keySet().iterator();
        final int rows = HEIGHT / 4;
        final int perColour = WIDTH / HEIGHT;
        count = 0;
        int painted = 0;
        for (int i = 0; i < WIDTH / 4 * rows; i++) {
            if (count % perColour == 0) {
                painted = colours.get(colour.next());
            }
            final int position = tiles.get(tile.next());
            final int tx = position / rows * 4;
            final int tz = position % rows * 4;
            for (int xx = 0; xx < 4; xx++) {
                for (int zz = 0; zz < 4; zz++) {
                    result[(tz + zz) * WIDTH + tx + xx] = painted;
                }
            }
            count++;
        }
        return result;
    }

    /**
     * 1.12.2 makeRGBimage: the local map, LOCAL x LOCAL RGB (row-major) from the LOCAL_CELLS x LOCAL_CELLS samples, each
     * block taking its cell's biome and a height smoothed between the cells round it.
     */
    public static int[] localPicture(byte[] samples, RandomSource random) {
        final int[] image = new int[LOCAL * LOCAL];
        for (int x = 0; x < LOCAL; x++) {
            final float fx = (x + 0.5F) / LOCAL_STEP - 0.5F;
            final int cx = Mth.clamp(Mth.floor(fx), 0, LOCAL_CELLS - 1);
            final int cx1 = Math.min(cx + 1, LOCAL_CELLS - 1);
            final float tx = Mth.clamp(fx - cx, 0.0F, 1.0F);
            for (int z = 0; z < LOCAL; z++) {
                final float fz = (z + 0.5F) / LOCAL_STEP - 0.5F;
                final int cz = Mth.clamp(Mth.floor(fz), 0, LOCAL_CELLS - 1);
                final int cz1 = Math.min(cz + 1, LOCAL_CELLS - 1);
                final float tz = Mth.clamp(fz - cz, 0.0F, 1.0F);
                final float top = Mth.lerp(tx, height(samples, cx, cz), height(samples, cx1, cz));
                final float bottom = Mth.lerp(tx, height(samples, cx, cz1), height(samples, cx1, cz1));
                final int biome = samples[(x / LOCAL_STEP * LOCAL_CELLS + z / LOCAL_STEP) * 2] & 255;
                image[z * LOCAL + x] = colour(biome, Math.round(Mth.lerp(tz, top, bottom)), random);
            }
        }
        return image;
    }

    private static int height(byte[] samples, int x, int z) {
        return samples[(x * LOCAL_CELLS + z) * 2 + 1] & 255;
    }
}
