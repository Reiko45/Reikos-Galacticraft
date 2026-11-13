package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.world.OverworldMap;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 10 part 4: the Overworld pictures (1.12.2 MapUtil) made from the terrain generator. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OverworldMapTests {
    private static final String AIR = "air_room";

    private OverworldMapTests() {
    }

    private static Holder<Biome> biome(GameTestHelper helper, net.minecraft.resources.ResourceKey<Biome> key) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
    }

    /** Vanilla biomes go by their 1.12.2 counterparts; new ones by their nearest kin. */
    @GameTest(template = AIR)
    public static void biomesFollow1122Numbering(GameTestHelper helper) {
        final Object[][] cases = {{Biomes.OCEAN, 0}, {Biomes.PLAINS, 1}, {Biomes.DESERT, 2}, {Biomes.WINDSWEPT_HILLS, 3}, {Biomes.FOREST, 4},
                {Biomes.SWAMP, 6}, {Biomes.RIVER, 7}, {Biomes.FROZEN_OCEAN, 10}, {Biomes.SNOWY_PLAINS, 12}, {Biomes.MUSHROOM_FIELDS, 14},
                {Biomes.BEACH, 16}, {Biomes.JUNGLE, 21}, {Biomes.DEEP_OCEAN, 24}, {Biomes.DARK_FOREST, 29}, {Biomes.SAVANNA, 35},
                {Biomes.BADLANDS, 37}, {Biomes.CHERRY_GROVE, 4}, {Biomes.MANGROVE_SWAMP, 6}, {Biomes.MEADOW, 1}, {Biomes.GROVE, 30},
                {Biomes.JAGGED_PEAKS, 13}};
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked")
            final net.minecraft.resources.ResourceKey<Biome> key = (net.minecraft.resources.ResourceKey<Biome>) c[0];
            helper.assertValueEqual(OverworldMap.legacyBiome(biome(helper, key)), (int) c[1], key.location().toString());
        }
        helper.succeed();
    }

    /**
     * 1.12.2 convertBiomeColour: water below sea level whatever the biome (not deserts), deep water below 56, the biome's
     * colour lighter the higher it lies (unchanged at 68, doubled at 162).
     */
    @GameTest(template = AIR)
    public static void coloursFollow1122(GameTestHelper helper) {
        final RandomSource random = RandomSource.create(1L);
        helper.assertValueEqual(OverworldMap.colour(1, 60, random), MapColor.WATER.col, "a plains pond is water");
        helper.assertValueEqual(OverworldMap.colour(1, 50, random), 0x2f2fd4, "deeper than 56: deep water");
        helper.assertValueEqual(OverworldMap.colour(24, 40, random), 0x2f2fd4, "deep ocean");
        helper.assertValueEqual(OverworldMap.colour(7, 68, random), 0x497436, "a river bank at 68: unchanged");
        helper.assertValueEqual(OverworldMap.colour(1, 162, random), 0x92e86c, "plains at 162: twice as bright");
        helper.assertValueEqual(OverworldMap.lighten(0x808080, -0.5F), 0x404040, "darker below 68");
        final int desert = OverworldMap.colour(2, 60, random);
        helper.assertTrue(desert == 0xd4cd98 || desert == MapColor.PLANT.col, "a desert hollow stays desert: " + Integer.toHexString(desert));
        helper.succeed();
    }

    /**
     * 1.12.2 convertTo12pxTexture: tiles ranked bluest to greenest take the palette's colours ranked the same way, so the
     * sea gets the icon's blues and the land its greens; every pixel of the result is a palette colour.
     */
    @GameTest(template = AIR)
    public static void earthPictureRecoloursWithPalette(GameTestHelper helper) {
        final int[] image = new int[OverworldMap.WIDTH * OverworldMap.HEIGHT];
        for (int z = 0; z < OverworldMap.HEIGHT; z++) {
            for (int x = 0; x < OverworldMap.WIDTH; x++) {
                image[z * OverworldMap.WIDTH + x] = x < OverworldMap.WIDTH / 4 ? 0x2020e0 : 0x30c030; // a quarter sea
            }
        }
        final int[] palette = new int[64 * 64];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                // the left quarter blue, the rest green, varied a little across
                palette[y * 64 + x] = 0xFF000000 | (x < 16 ? 0x1040c0 + x : 0x40b020 + x);
            }
        }
        final int[] picture = OverworldMap.earthPicture(image, palette, 64);
        final java.util.Set<Integer> colours = new java.util.HashSet<>();
        for (int i = 0; i < palette.length; i++) {
            colours.add(palette[i] & 0xFFFFFF);
        }
        helper.assertTrue(Arrays.stream(picture).allMatch(colours::contains), "only palette colours");
        int blueSea = 0;
        for (int z = 0; z < OverworldMap.HEIGHT; z++) {
            for (int x = 0; x < OverworldMap.WIDTH / 4; x++) {
                final int col = picture[z * OverworldMap.WIDTH + x];
                if ((col & 255) > (col >> 8 & 255)) {
                    blueSea++;
                }
            }
        }
        helper.assertValueEqual(blueSea, OverworldMap.WIDTH / 4 * OverworldMap.HEIGHT, "the sea quarter is all blue");
        helper.succeed();
    }

    /** The local map: one colour where every sample is the same; heights blended between cells. */
    @GameTest(template = AIR)
    public static void localMapFillsTheScreen(GameTestHelper helper) {
        final byte[] samples = new byte[OverworldMap.LOCAL_CELLS * OverworldMap.LOCAL_CELLS * 2];
        for (int i = 0; i < samples.length; i += 2) {
            samples[i] = 1;
            samples[i + 1] = 68;
        }
        final int[] flat = OverworldMap.localPicture(samples, RandomSource.create(0L));
        helper.assertValueEqual(flat.length, OverworldMap.LOCAL * OverworldMap.LOCAL, "352 x 352");
        helper.assertTrue(Arrays.stream(flat).allMatch(c -> c == 0x497436), "all plains green");
        // A cell twice as high in the middle shades its neighbours a little lighter too
        final int mid = OverworldMap.LOCAL_CELLS / 2;
        samples[(mid * OverworldMap.LOCAL_CELLS + mid) * 2 + 1] = (byte) 162;
        final int[] hill = OverworldMap.localPicture(samples, RandomSource.create(0L));
        final int beside = hill[(mid * 4 + 2) * OverworldMap.LOCAL + mid * 4 - 2];
        helper.assertTrue(beside != 0x497436 && (beside >> 8 & 255) > 0x74, "smoothed up the slope: " + Integer.toHexString(beside));
        helper.succeed();
    }

    /**
     * 1.12.2 MapGen: samples from the Overworld's own generator with nothing loaded, the same each time: heights where
     * the land is and biomes in 1.12.2's numbering.
     */
    @GameTest(template = AIR, timeoutTicks = 400)
    public static void sampleFromOverworldGenerator(GameTestHelper helper) {
        final RegistryAccess access = helper.getLevel().registryAccess();
        final Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        final Registry<MultiNoiseBiomeSourceParameterList> presets = access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        final NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromPreset(presets.getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)), settings);
        final RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), 12345L);
        final byte[] first = OverworldMap.sample(generator, random, -768, -192, 24, 6, 64);
        final byte[] again = OverworldMap.sample(generator, random, -768, -192, 24, 6, 64);
        helper.assertTrue(Arrays.equals(first, again), "the same each time");
        // The quick ground height matches the generator's own (OCEAN_FLOOR_WG) to a block or two
        final net.minecraft.world.level.levelgen.NoiseSettings noise = settings.value().noiseSettings();
        int close = 0;
        for (int i = 0; i < 100; i++) {
            final int x = -2000 + i * 41;
            final int z = 700 - i * 29;
            final int real = generator.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), random);
            if (Math.abs(OverworldMap.ground(random, noise, x, z) - real) <= 2) {
                close++;
            }
        }
        helper.assertTrue(close >= 90, "within 2 blocks of the generator's ground: " + close + " of 100");
        int lowest = 255;
        int highest = 0;
        for (int i = 0; i < first.length; i += 2) {
            helper.assertTrue((first[i] & 255) < 40, "a 1.12.2 biome: " + (first[i] & 255));
            lowest = Math.min(lowest, first[i + 1] & 255);
            highest = Math.max(highest, first[i + 1] & 255);
        }
        helper.assertTrue(lowest > 0 && highest > lowest, "real ground heights: " + lowest + " to " + highest);
        // The GameTest server's own Overworld is flat: as in 1.12.2 there is no picture of it
        final CompletableFuture<byte[]> strip = OverworldMap.strip(helper.getLevel().getServer());
        helper.succeedWhen(() -> {
            helper.assertTrue(strip.isDone(), "strip made");
            final boolean noiseWorld = helper.getLevel().getServer().overworld().getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator;
            helper.assertValueEqual(strip.join().length, noiseWorld ? OverworldMap.WIDTH * OverworldMap.HEIGHT * 2 : 0, "strip size");
        });
    }
}
