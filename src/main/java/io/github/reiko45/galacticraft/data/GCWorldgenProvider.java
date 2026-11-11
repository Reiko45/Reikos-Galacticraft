package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import io.github.reiko45.galacticraft.registry.GCFeatures;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.MoonChunkGenerator;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Data-driven registries: ore and oil features, the Overworld biome modifiers, the Moon biome, dimension type and
 * dimension, and damage types.
 *
 * <p>1.12.2 generated ores and oil imperatively ({@code OverworldGenerator}, {@code BiomeDecoratorMoon});
 * counts, vein sizes and height ranges below are copied from those classes.
 */
public class GCWorldgenProvider extends DatapackBuiltinEntriesProvider {
    // Overworld ores (copper is vanilla's own since 1.17, so GC copper ore is not generated)
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN = configured("ore_tin");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ALUMINUM = configured("ore_aluminum");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SILICON = configured("ore_silicon");
    public static final ResourceKey<PlacedFeature> ORE_TIN_PLACED = placed("ore_tin");
    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_PLACED = placed("ore_aluminum");
    public static final ResourceKey<PlacedFeature> ORE_SILICON_PLACED = placed("ore_silicon");

    // Moon
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_DIRT_BLOB = configured("moon_dirt_blob");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_ORE_COPPER = configured("moon_ore_copper");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_ORE_TIN = configured("moon_ore_tin");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_ORE_CHEESE = configured("moon_ore_cheese");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_ORE_SAPPHIRE = configured("moon_ore_sapphire");
    public static final ResourceKey<PlacedFeature> MOON_DIRT_BLOB_PLACED = placed("moon_dirt_blob");
    public static final ResourceKey<PlacedFeature> MOON_ORE_COPPER_PLACED = placed("moon_ore_copper");
    public static final ResourceKey<PlacedFeature> MOON_ORE_TIN_PLACED = placed("moon_ore_tin");
    public static final ResourceKey<PlacedFeature> MOON_ORE_CHEESE_PLACED = placed("moon_ore_cheese");
    public static final ResourceKey<PlacedFeature> MOON_ORE_SAPPHIRE_PLACED = placed("moon_ore_sapphire");

    // Overworld crude oil pockets
    public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_POCKET = configured("oil_pocket");
    public static final ResourceKey<PlacedFeature> OIL_POCKET_PLACED = placed("oil_pocket");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_OIL =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GalacticraftCore.id("add_overworld_oil"));

    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_ORES =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GalacticraftCore.id("add_overworld_ores"));

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, GCWorldgenProvider::configuredFeatures)
            .add(Registries.PLACED_FEATURE, GCWorldgenProvider::placedFeatures)
            .add(Registries.BIOME, GCWorldgenProvider::biomes)
            .add(Registries.DIMENSION_TYPE, GCWorldgenProvider::dimensionTypes)
            .add(Registries.LEVEL_STEM, GCWorldgenProvider::levelStems)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GCWorldgenProvider::biomeModifiers)
            .add(Registries.DAMAGE_TYPE, context -> {
                context.register(GCDamageTypes.OXYGEN_SUFFOCATION, new DamageType("oxygen_suffocation", DamageScaling.NEVER, 0.0F));
                context.register(GCDamageTypes.SPACESHIP_CRASH, new DamageType("spaceship_crash", DamageScaling.NEVER, 0.1F));
            });

    public GCWorldgenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(GalacticraftCore.MOD_ID));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, GalacticraftCore.id(name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, GalacticraftCore.id(name));
    }

    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        final RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        final RuleTest moonRock = new BlockMatchTest(GCBlocks.MOON_ROCK.get());

        // OverworldGenerator(block, meta, amountPerChunk, minY, maxY, amountPerVein): vein size 7 for all.
        FeatureUtils.register(context, ORE_TIN, Feature.ORE, new OreConfiguration(stone, GCBlocks.TIN_ORE.get().defaultBlockState(), 7));
        FeatureUtils.register(context, ORE_ALUMINUM, Feature.ORE, new OreConfiguration(stone, GCBlocks.ALUMINUM_ORE.get().defaultBlockState(), 7));
        FeatureUtils.register(context, ORE_SILICON, Feature.ORE, new OreConfiguration(stone, GCBlocks.SILICON_ORE.get().defaultBlockState(), 7));

        // The pocket feature rolls its own (biome-dependent) chance, like 1.12.2 EventHandlerGC#oilPresent.
        FeatureUtils.register(context, OIL_POCKET, GCFeatures.OIL_POCKET.get(), NoneFeatureConfiguration.INSTANCE);

        // BiomeDecoratorMoon: WorldGenMinableMeta vein sizes.
        FeatureUtils.register(context, MOON_DIRT_BLOB, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_DIRT.get().defaultBlockState(), 32));
        FeatureUtils.register(context, MOON_ORE_COPPER, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_COPPER_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, MOON_ORE_TIN, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_TIN_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, MOON_ORE_CHEESE, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_CHEESE_ORE.get().defaultBlockState(), 3));
        // Sapphire was placed as single blocks replacing moon rock.
        FeatureUtils.register(context, MOON_ORE_SAPPHIRE, Feature.REPLACE_SINGLE_BLOCK, new ReplaceBlockConfiguration(
                List.of(OreConfiguration.target(moonRock, GCBlocks.MOON_SAPPHIRE_ORE.get().defaultBlockState()))));
    }

    private static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        final HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        // 1.12.2 picked y = minY + nextInt(maxY - minY), i.e. [minY, maxY - 1].
        ore(context, ORE_TIN_PLACED, configured, ORE_TIN, 22, 0, 59);
        ore(context, ORE_ALUMINUM_PLACED, configured, ORE_ALUMINUM, 18, 0, 44);
        ore(context, ORE_SILICON_PLACED, configured, ORE_SILICON, 3, 0, 24);

        PlacementUtils.register(context, OIL_POCKET_PLACED, configured.getOrThrow(OIL_POCKET), List.of(BiomeFilter.biome()));

        ore(context, MOON_DIRT_BLOB_PLACED, configured, MOON_DIRT_BLOB, 20, 0, 199);
        ore(context, MOON_ORE_COPPER_PLACED, configured, MOON_ORE_COPPER, 26, 0, 59);
        ore(context, MOON_ORE_TIN_PLACED, configured, MOON_ORE_TIN, 23, 0, 59);
        ore(context, MOON_ORE_CHEESE_PLACED, configured, MOON_ORE_CHEESE, 14, 0, 84);
        // 3 + nextInt(6) attempts per chunk at y = 4 + nextInt(28).
        PlacementUtils.register(context, MOON_ORE_SAPPHIRE_PLACED, configured.getOrThrow(MOON_ORE_SAPPHIRE), List.of(
                CountPlacement.of(UniformInt.of(3, 8)), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(4), VerticalAnchor.absolute(31)), BiomeFilter.biome()));
    }

    private static void ore(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                            HolderGetter<ConfiguredFeature<?, ?>> configured, ResourceKey<ConfiguredFeature<?, ?>> feature,
                            int count, int minY, int maxY) {
        PlacementUtils.register(context, key, configured.getOrThrow(feature), List.of(
                CountPlacement.of(count), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY)), BiomeFilter.biome()));
    }

    private static void biomes(BootstrapContext<Biome> context) {
        final BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(
                context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MOON_DIRT_BLOB_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MOON_ORE_COPPER_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MOON_ORE_TIN_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MOON_ORE_CHEESE_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MOON_ORE_SAPPHIRE_PLACED);

        context.register(GCDimensions.MOON_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x000000)
                        .fogColor(0x000000)
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .build())
                // Evolved mobs spawn here in 1.12.2; they arrive with the entity porting phase.
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(generation.build())
                .build());
    }

    private static void dimensionTypes(BootstrapContext<DimensionType> context) {
        context.register(GCDimensions.MOON_TYPE, new DimensionType(
                OptionalLong.empty(),   // day/night cycle (1.12.2 used an 8x longer day; vanilla cannot express that)
                true,                   // has sky light
                false,                  // no ceiling
                false,                  // not ultrawarm
                false,                  // not natural: compasses/clocks spin, beds explode
                1.0D,                   // coordinate scale
                false,                  // beds don't work
                false,                  // respawn anchors don't work
                0,                      // min y (1.12.2 worlds started at 0)
                256,                    // height
                256,                    // logical height
                BlockTags.INFINIBURN_OVERWORLD,
                GCDimensions.MOON_EFFECTS,
                0.0F,                   // ambient light
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 0)));
    }

    private static void levelStems(BootstrapContext<LevelStem> context) {
        final BlockState turf = GCBlocks.MOON_TURF.get().defaultBlockState();
        final BlockState dirt = GCBlocks.MOON_DIRT.get().defaultBlockState();
        final BlockState rock = GCBlocks.MOON_ROCK.get().defaultBlockState();
        context.register(GCDimensions.MOON_STEM, new LevelStem(
                context.lookup(Registries.DIMENSION_TYPE).getOrThrow(GCDimensions.MOON_TYPE),
                new MoonChunkGenerator(new FixedBiomeSource(context.lookup(Registries.BIOME).getOrThrow(GCDimensions.MOON_BIOME)),
                        turf, dirt, rock)));
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        final HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        context.register(ADD_OVERWORLD_ORES, new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placed.getOrThrow(ORE_TIN_PLACED), placed.getOrThrow(ORE_ALUMINUM_PLACED), placed.getOrThrow(ORE_SILICON_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // LAKES: after caves are carved (so pockets stay sealed) but before structures, which then win over oil.
        context.register(ADD_OVERWORLD_OIL, new BiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placed.getOrThrow(OIL_POCKET_PLACED)), GenerationStep.Decoration.LAKES));
    }
}
