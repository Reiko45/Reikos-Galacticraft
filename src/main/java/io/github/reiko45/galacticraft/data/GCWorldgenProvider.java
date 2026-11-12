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
import net.minecraft.world.entity.MobCategory;
import io.github.reiko45.galacticraft.registry.GCEntities;
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

    // Mars (BiomeDecoratorMars)
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARS_ICE = configured("mars_ice");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARS_REGOLITH_BLOB = configured("mars_regolith_blob");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARS_ORE_DESH = configured("mars_ore_desh");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARS_ORE_COPPER = configured("mars_ore_copper");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARS_ORE_TIN = configured("mars_ore_tin");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARS_ORE_IRON = configured("mars_ore_iron");
    public static final ResourceKey<PlacedFeature> MARS_ICE_PLACED = placed("mars_ice");
    public static final ResourceKey<PlacedFeature> MARS_REGOLITH_BLOB_PLACED = placed("mars_regolith_blob");
    public static final ResourceKey<PlacedFeature> MARS_ORE_DESH_PLACED = placed("mars_ore_desh");
    public static final ResourceKey<PlacedFeature> MARS_ORE_COPPER_PLACED = placed("mars_ore_copper");
    public static final ResourceKey<PlacedFeature> MARS_ORE_TIN_PLACED = placed("mars_ore_tin");
    public static final ResourceKey<PlacedFeature> MARS_ORE_IRON_PLACED = placed("mars_ore_iron");

    // Overworld crude oil pockets
    public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_POCKET = configured("oil_pocket");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SLIMELING_EGGS = configured("slimeling_eggs");
    public static final ResourceKey<PlacedFeature> SLIMELING_EGGS_PLACED = placed("slimeling_eggs");
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
            .add(Registries.STRUCTURE, GCWorldgenProvider::structures)
            .add(Registries.STRUCTURE_SET, GCWorldgenProvider::structureSets)
            .add(Registries.DAMAGE_TYPE, context -> {
                context.register(GCDamageTypes.OXYGEN_SUFFOCATION, new DamageType("oxygen_suffocation", DamageScaling.NEVER, 0.0F));
                context.register(GCDamageTypes.SPACESHIP_CRASH, new DamageType("spaceship_crash", DamageScaling.NEVER, 0.1F));
                context.register(GCDamageTypes.SLIMELING, new DamageType("slimeling", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
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
        FeatureUtils.register(context, SLIMELING_EGGS, GCFeatures.SLIMELING_EGGS.get(), NoneFeatureConfiguration.INSTANCE);

        // BiomeDecoratorMoon: WorldGenMinableMeta vein sizes.
        FeatureUtils.register(context, MOON_DIRT_BLOB, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_DIRT.get().defaultBlockState(), 32));
        FeatureUtils.register(context, MOON_ORE_COPPER, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_COPPER_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, MOON_ORE_TIN, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_TIN_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, MOON_ORE_CHEESE, Feature.ORE, new OreConfiguration(moonRock, GCBlocks.MOON_CHEESE_ORE.get().defaultBlockState(), 3));
        // BiomeDecoratorMars: ores and regolith pockets replace Mars stone, ice replaces regolith.
        final RuleTest marsStone = new BlockMatchTest(GCBlocks.MARS_STONE.get());
        FeatureUtils.register(context, MARS_ICE, Feature.ORE, new OreConfiguration(new BlockMatchTest(GCBlocks.MARS_REGOLITH.get()),
                net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 18));
        FeatureUtils.register(context, MARS_REGOLITH_BLOB, Feature.ORE, new OreConfiguration(marsStone, GCBlocks.MARS_REGOLITH.get().defaultBlockState(), 32));
        FeatureUtils.register(context, MARS_ORE_DESH, Feature.ORE, new OreConfiguration(marsStone, GCBlocks.DESH_ORE.get().defaultBlockState(), 6));
        FeatureUtils.register(context, MARS_ORE_COPPER, Feature.ORE, new OreConfiguration(marsStone, GCBlocks.MARS_COPPER_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, MARS_ORE_TIN, Feature.ORE, new OreConfiguration(marsStone, GCBlocks.MARS_TIN_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, MARS_ORE_IRON, Feature.ORE, new OreConfiguration(marsStone, GCBlocks.MARS_IRON_ORE.get().defaultBlockState(), 8));
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
        // 1.12.2 EventHandlerMars#onPlanetDecorated: 2 a chunk at y 24 + nextInt(104)
        PlacementUtils.register(context, SLIMELING_EGGS_PLACED, configured.getOrThrow(SLIMELING_EGGS), List.of(CountPlacement.of(2),
                InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(24), VerticalAnchor.absolute(127)), BiomeFilter.biome()));

        ore(context, MOON_DIRT_BLOB_PLACED, configured, MOON_DIRT_BLOB, 20, 0, 199);
        ore(context, MOON_ORE_COPPER_PLACED, configured, MOON_ORE_COPPER, 26, 0, 59);
        ore(context, MOON_ORE_TIN_PLACED, configured, MOON_ORE_TIN, 23, 0, 59);
        ore(context, MOON_ORE_CHEESE_PLACED, configured, MOON_ORE_CHEESE, 14, 0, 84);
        ore(context, MARS_ICE_PLACED, configured, MARS_ICE, 4, 60, 119);
        ore(context, MARS_REGOLITH_BLOB_PLACED, configured, MARS_REGOLITH_BLOB, 20, 0, 199);
        ore(context, MARS_ORE_DESH_PLACED, configured, MARS_ORE_DESH, 15, 20, 63);
        ore(context, MARS_ORE_COPPER_PLACED, configured, MARS_ORE_COPPER, 26, 0, 59);
        ore(context, MARS_ORE_TIN_PLACED, configured, MARS_ORE_TIN, 23, 0, 59);
        ore(context, MARS_ORE_IRON_PLACED, configured, MARS_ORE_IRON, 20, 0, 63);
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

    public static final ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> MOON_DUNGEON =
            ResourceKey.create(Registries.STRUCTURE, GalacticraftCore.id("moon_dungeon"));
    public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet> MOON_DUNGEONS =
            ResourceKey.create(Registries.STRUCTURE_SET, GalacticraftCore.id("moon_dungeons"));
    public static final ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> MARS_DUNGEON =
            ResourceKey.create(Registries.STRUCTURE, GalacticraftCore.id("mars_dungeon"));
    public static final ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet> MARS_DUNGEONS =
            ResourceKey.create(Registries.STRUCTURE_SET, GalacticraftCore.id("mars_dungeons"));

    /**
     * The Moon and Mars dungeons, each in its planet's biome only, built after caves like 1.12.2 (no terrain adaptation:
     * they dig their own crater).
     */
    private static void structures(BootstrapContext<net.minecraft.world.level.levelgen.structure.Structure> context) {
        dungeon(context, MOON_DUNGEON, GCDimensions.MOON_BIOME, io.github.reiko45.galacticraft.world.dungeon.DungeonConfig.MOON);
        dungeon(context, MARS_DUNGEON, GCDimensions.MARS_BIOME, io.github.reiko45.galacticraft.world.dungeon.DungeonConfig.MARS);
    }

    private static void dungeon(BootstrapContext<net.minecraft.world.level.levelgen.structure.Structure> context,
                                ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key, ResourceKey<Biome> biome,
                                io.github.reiko45.galacticraft.world.dungeon.DungeonConfig config) {
        context.register(key, new io.github.reiko45.galacticraft.world.dungeon.Dungeon(
                new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
                        net.minecraft.core.HolderSet.direct(context.lookup(Registries.BIOME).getOrThrow(biome)),
                        java.util.Map.of(), GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
                        net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE), config));
    }

    /**
     * 1.12.2 MapGenDungeon: one per 704x704 block (44x44 chunk) region, at a random chunk of it. 1.12.2 salted the
     * position with 10387340 plus the dimension id; Mars takes its 1.12.2 id (-29) so its dungeons fall elsewhere than
     * the Moon's (which keeps the plain salt it was given in phase 5b).
     */
    private static void structureSets(BootstrapContext<net.minecraft.world.level.levelgen.structure.StructureSet> context) {
        context.register(MOON_DUNGEONS, new net.minecraft.world.level.levelgen.structure.StructureSet(
                context.lookup(Registries.STRUCTURE).getOrThrow(MOON_DUNGEON),
                new net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement(44, 0,
                        net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.LINEAR, 10387340)));
        context.register(MARS_DUNGEONS, new net.minecraft.world.level.levelgen.structure.StructureSet(
                context.lookup(Registries.STRUCTURE).getOrThrow(MARS_DUNGEON),
                new net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement(44, 0,
                        net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.LINEAR, 10387340 - 29)));
    }

    private static void biomes(BootstrapContext<Biome> context) {
        orbitBiome(context);
        marsBiome(context);
        asteroidsBiome(context);
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
                // 1.12.2 GalacticraftCore#init: moonMoon.addMobInfo(...)
                .mobSpawnSettings(new MobSpawnSettings.Builder()
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_ZOMBIE.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_SPIDER.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_SKELETON.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_CREEPER.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_ENDERMAN.get(), 10, 1, 4))
                        .creatureGenerationProbability(0.1F)
                        .build())
                .generationSettings(generation.build())
                .build());
    }

    /**
     * 1.12.2 BiomeMars "Mars Flat": the evolved mobs of MarsModule#init with 1.12.2's 0.01 spawning chance. The sky and
     * fog colours are WorldProviderMars's; MarsEffects dims them at night.
     */
    private static void marsBiome(BootstrapContext<Biome> context) {
        final BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(
                context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MARS_ICE_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MARS_REGOLITH_BLOB_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MARS_ORE_DESH_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MARS_ORE_COPPER_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MARS_ORE_TIN_PLACED);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MARS_ORE_IRON_PLACED);
        generation.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, SLIMELING_EGGS_PLACED);
        context.register(GCDimensions.MARS_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x9A7242)
                        .fogColor(0xD2783B)
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder()
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_ZOMBIE.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_SPIDER.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_SKELETON.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_CREEPER.get(), 8, 2, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_ENDERMAN.get(), 10, 1, 4))
                        .creatureGenerationProbability(0.01F)
                        .build())
                .generationSettings(generation.build())
                .build());
    }

    /** 1.12.2 BiomeAsteroids: black sky and fog, evolved mobs with 1.12.2's weights and 0.01 spawning chance. */
    private static void asteroidsBiome(BootstrapContext<Biome> context) {
        context.register(GCDimensions.ASTEROIDS_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.0F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x000000)
                        .fogColor(0x000000)
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder()
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_ZOMBIE.get(), 3000, 1, 3))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_SPIDER.get(), 2000, 1, 2))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_SKELETON.get(), 1500, 1, 1))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(GCEntities.EVOLVED_CREEPER.get(), 2000, 1, 1))
                        .creatureGenerationProbability(0.01F)
                        .build())
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build());
    }

    /** 1.12.2 BiomeOrbit.space: black sky, nothing spawns. */
    private static void orbitBiome(BootstrapContext<Biome> context) {
        context.register(GCDimensions.ORBIT_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x000000)
                        .fogColor(0x000000)
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
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
        // 1.12.2 WorldProviderMars: a 24,660 tick day (vanilla cannot express that either), y 0 to 256. Beds work (1.12.2
        // let them be slept in without exploding); GCPlayerHandler keeps them from setting a spawn point, as 1.12.2 did.
        context.register(GCDimensions.MARS_TYPE, new DimensionType(
                OptionalLong.empty(), true, false, false, false, 1.0D, true, false, 0, 256, 256,
                BlockTags.INFINIBURN_OVERWORLD, GCDimensions.MARS_EFFECTS, 0.0F,
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 0)));
        // 1.12.2 WorldProviderAsteroids: no sky light, the sun fixed on the horizon (celestial angle 0.25, which day time
        // 12,785 gives), y 0 to 256. Beds can't be slept in (1.12.2 isDaytime was always true; see GCPlayerHandler).
        context.register(GCDimensions.ASTEROIDS_TYPE, new DimensionType(
                OptionalLong.of(12785L), false, false, false, false, 1.0D, true, false, 0, 256, 256,
                BlockTags.INFINIBURN_OVERWORLD, GCDimensions.ASTEROIDS_EFFECTS, 0.0F,
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 0)));
        // 1.12.2 WorldProviderOverworldOrbit: 24,000 tick day, worlds from y 0 to 256, no weather
        context.register(GCDimensions.ORBIT_TYPE, new DimensionType(
                OptionalLong.empty(), true, false, false, false, 1.0D, false, false, 0, 256, 256,
                BlockTags.INFINIBURN_OVERWORLD, GCDimensions.ORBIT_EFFECTS, 0.0F,
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
        context.register(GCDimensions.MARS_STEM, new LevelStem(
                context.lookup(Registries.DIMENSION_TYPE).getOrThrow(GCDimensions.MARS_TYPE),
                new io.github.reiko45.galacticraft.world.MarsChunkGenerator(
                        new FixedBiomeSource(context.lookup(Registries.BIOME).getOrThrow(GCDimensions.MARS_BIOME)),
                        GCBlocks.MARS_FINE_REGOLITH.get().defaultBlockState(), GCBlocks.MARS_REGOLITH.get().defaultBlockState(),
                        GCBlocks.MARS_STONE.get().defaultBlockState())));
        context.register(GCDimensions.ASTEROIDS_STEM, new LevelStem(
                context.lookup(Registries.DIMENSION_TYPE).getOrThrow(GCDimensions.ASTEROIDS_TYPE),
                new io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator(
                        new FixedBiomeSource(context.lookup(Registries.BIOME).getOrThrow(GCDimensions.ASTEROIDS_BIOME)))));
        // 1.12.2 ChunkProviderOrbit: empty space (stations are built when they are created)
        context.register(GCDimensions.ORBIT_STEM, new LevelStem(
                context.lookup(Registries.DIMENSION_TYPE).getOrThrow(GCDimensions.ORBIT_TYPE),
                new net.minecraft.world.level.levelgen.FlatLevelSource(new net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings(
                        java.util.Optional.of(HolderSet.direct()), context.lookup(Registries.BIOME).getOrThrow(GCDimensions.ORBIT_BIOME), List.of()))));
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
