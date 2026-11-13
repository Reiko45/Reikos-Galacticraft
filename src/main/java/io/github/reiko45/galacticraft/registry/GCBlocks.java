package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.block.BossSpawnerBlock;
import io.github.reiko45.galacticraft.block.CavernousVinesBlock;
import io.github.reiko45.galacticraft.block.CheeseBlock;
import io.github.reiko45.galacticraft.block.TreasureChestBlock;
import io.github.reiko45.galacticraft.block.ConfigurableMachineBlock;
import io.github.reiko45.galacticraft.block.GlowstoneTorchBlock;
import io.github.reiko45.galacticraft.block.GlowstoneWallTorchBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.FallenMeteorBlock;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.block.NasaWorkbenchBlock;
import io.github.reiko45.galacticraft.block.ParachestBlock;
import io.github.reiko45.galacticraft.block.SolarPanelBlock;
import io.github.reiko45.galacticraft.block.SolarPartBlock;
import io.github.reiko45.galacticraft.block.entity.SolarPanelBlockEntity;
import io.github.reiko45.galacticraft.block.SwitchableWireBlock;
import io.github.reiko45.galacticraft.block.EnergyStorageBlock;
import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.FluidTankBlock;
import io.github.reiko45.galacticraft.block.BreathableAirBlock;
import io.github.reiko45.galacticraft.block.OxygenDetectorBlock;
import io.github.reiko45.galacticraft.block.OxygenStorageModuleBlock;
import io.github.reiko45.galacticraft.block.SpaceTorchBlock;
import io.github.reiko45.galacticraft.block.SpaceWallTorchBlock;
import io.github.reiko45.galacticraft.block.entity.OxygenCollectorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDecompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDistributorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenStorageModuleBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricFurnaceBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registry. 1.12.2 packed most of these into metadata variants of a few blocks
 * ({@code basic_block_core}, {@code basic_block_moon}, {@code slab_gc_half}, {@code wall_gc});
 * since the 1.13 flattening every variant is its own block. Hardness/resistance values are taken
 * from the 1.12.2 overrides ({@code BlockBasic#getBlockHardness}, {@code BlockBasicMoon#getExplosionResistance}).
 */
public final class GCBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GalacticraftCore.MOD_ID);

    private static final List<DeferredBlock<?>> BLOCKS_WITH_ITEMS = new ArrayList<>();

    // --- Overworld ores and storage blocks (copper uses the vanilla blocks since 1.17) ---
    public static final DeferredBlock<Block> TIN_ORE = withItem("tin_ore", () -> new Block(ore(MapColor.STONE, 5.0F, 3.0F)));
    public static final DeferredBlock<Block> ALUMINUM_ORE = withItem("aluminum_ore", () -> new Block(ore(MapColor.STONE, 6.0F, 3.0F)));
    public static final DeferredBlock<Block> SILICON_ORE = withItem("silicon_ore", () -> new Block(ore(MapColor.STONE, 3.0F, 3.0F)));

    public static final DeferredBlock<Block> TIN_BLOCK = withItem("tin_block", () -> new Block(metal(MapColor.METAL, 1.0F, 6.0F)));
    public static final DeferredBlock<Block> ALUMINUM_BLOCK = withItem("aluminum_block", () -> new Block(metal(MapColor.METAL, 1.0F, 6.0F)));
    public static final DeferredBlock<Block> METEORIC_IRON_BLOCK = withItem("meteoric_iron_block", () -> new Block(metal(MapColor.COLOR_GRAY, 1.0F, 8.0F)));
    public static final DeferredBlock<Block> SILICON_BLOCK = withItem("silicon_block", () -> new Block(metal(MapColor.COLOR_BLACK, 1.0F, 6.0F)));

    // --- Tin decoration family ("deco_block_0" / "deco_block_1") ---
    public static final DeferredBlock<Block> TIN_DECORATION_BLOCK_1 = withItem("tin_decoration_block_1", () -> new Block(decoration()));
    public static final DeferredBlock<Block> TIN_DECORATION_BLOCK_2 = withItem("tin_decoration_block_2", () -> new Block(decoration()));
    public static final DeferredBlock<StairBlock> TIN_DECORATION_STAIRS_1 = stairs("tin_decoration_stairs_1", TIN_DECORATION_BLOCK_1);
    public static final DeferredBlock<StairBlock> TIN_DECORATION_STAIRS_2 = stairs("tin_decoration_stairs_2", TIN_DECORATION_BLOCK_2);
    public static final DeferredBlock<SlabBlock> TIN_DECORATION_SLAB_1 = slab("tin_decoration_slab_1", TIN_DECORATION_BLOCK_1);
    public static final DeferredBlock<SlabBlock> TIN_DECORATION_SLAB_2 = slab("tin_decoration_slab_2", TIN_DECORATION_BLOCK_2);
    public static final DeferredBlock<WallBlock> TIN_DECORATION_WALL_1 = wall("tin_decoration_wall_1", TIN_DECORATION_BLOCK_1);
    public static final DeferredBlock<WallBlock> TIN_DECORATION_WALL_2 = wall("tin_decoration_wall_2", TIN_DECORATION_BLOCK_2);

    // --- Moon ---
    public static final DeferredBlock<Block> MOON_TURF = withItem("moon_turf", () -> new Block(moonSoil(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<Block> MOON_DIRT = withItem("moon_dirt", () -> new Block(moonSoil(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> MOON_ROCK = withItem("moon_rock", () -> new Block(moonStone(1.5F, 6.0F)));
    public static final DeferredBlock<Block> MOON_DUNGEON_BRICK = withItem("moon_dungeon_brick", () -> new Block(moonStone(4.0F, 40.0F)));
    public static final DeferredBlock<Block> MOON_COPPER_ORE = withItem("moon_copper_ore", () -> new Block(moonStone(5.0F, 3.0F)));
    public static final DeferredBlock<Block> MOON_TIN_ORE = withItem("moon_tin_ore", () -> new Block(moonStone(5.0F, 3.0F)));
    public static final DeferredBlock<Block> MOON_CHEESE_ORE = withItem("moon_cheese_ore", () -> new Block(moonStone(3.0F, 3.0F)));
    public static final DeferredBlock<Block> MOON_SAPPHIRE_ORE = withItem("moon_sapphire_ore", () -> new Block(moonStone(5.0F, 3.0F)));

    public static final DeferredBlock<StairBlock> MOON_ROCK_STAIRS = stairs("moon_rock_stairs", MOON_ROCK);
    public static final DeferredBlock<StairBlock> MOON_DUNGEON_BRICK_STAIRS = stairs("moon_dungeon_brick_stairs", MOON_DUNGEON_BRICK);
    public static final DeferredBlock<SlabBlock> MOON_ROCK_SLAB = slab("moon_rock_slab", MOON_ROCK);
    public static final DeferredBlock<SlabBlock> MOON_DUNGEON_BRICK_SLAB = slab("moon_dungeon_brick_slab", MOON_DUNGEON_BRICK);
    public static final DeferredBlock<WallBlock> MOON_ROCK_WALL = wall("moon_rock_wall", MOON_ROCK);
    public static final DeferredBlock<WallBlock> MOON_DUNGEON_BRICK_WALL = wall("moon_dungeon_brick_wall", MOON_DUNGEON_BRICK);

    // --- Mars (1.12.2 BlockBasicMars "mars": Material.ROCK, hardness 2.2, so even the regolith wants a tool) ---
    public static final DeferredBlock<Block> MARS_COPPER_ORE = withItem("mars_copper_ore", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 2.2F)));
    public static final DeferredBlock<Block> MARS_TIN_ORE = withItem("mars_tin_ore", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 2.2F)));
    public static final DeferredBlock<Block> MARS_IRON_ORE = withItem("mars_iron_ore", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 2.2F)));
    /** Drops Raw Desh and 2-5 experience (1.12.2 getExpDrop). */
    public static final DeferredBlock<DropExperienceBlock> DESH_ORE = withItem("desh_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), marsRock(MapColor.COLOR_RED, 2.2F, 2.2F)));
    public static final DeferredBlock<Block> MARS_FINE_REGOLITH = withItem("mars_fine_regolith", () -> new Block(marsRock(MapColor.DIRT, 2.2F, 2.2F)));
    public static final DeferredBlock<Block> MARS_REGOLITH = withItem("mars_regolith", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 2.2F)));
    public static final DeferredBlock<Block> MARS_STONE = withItem("mars_stone", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 2.2F)));
    public static final DeferredBlock<Block> MARS_COBBLESTONE = withItem("mars_cobblestone", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 2.2F)));
    public static final DeferredBlock<Block> MARS_DUNGEON_BRICK = withItem("mars_dungeon_brick",
            () -> new io.github.reiko45.galacticraft.block.MarsDungeonBrickBlock(marsRock(MapColor.COLOR_GREEN, 4.0F, 40.0F)));
    public static final DeferredBlock<Block> DESH_BLOCK = withItem("desh_block", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 60.0F)
            .instrument(NoteBlockInstrument.IRON_XYLOPHONE).sound(SoundType.METAL)));

    // --- Asteroids (1.12.2 BlockBasicAsteroids "asteroids_block": Material.ROCK, hardness 3 and no blast resistance) ---
    public static final DeferredBlock<Block> ASTEROID_ROCK_0 = withItem("asteroid_rock_0", () -> new Block(asteroidRock()));
    public static final DeferredBlock<Block> ASTEROID_ROCK_1 = withItem("asteroid_rock_1", () -> new Block(asteroidRock()));
    public static final DeferredBlock<Block> ASTEROID_ROCK_2 = withItem("asteroid_rock_2", () -> new Block(asteroidRock()));
    public static final DeferredBlock<Block> ASTEROID_ALUMINUM_ORE = withItem("asteroid_aluminum_ore", () -> new Block(asteroidRock()));
    /** Drops an Iron Shard and a Titanium Shard and 2-3 experience (1.12.2 getDrops, getExpDrop). */
    public static final DeferredBlock<DropExperienceBlock> ILMENITE_ORE = withItem("ilmenite_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 3), asteroidRock()));
    public static final DeferredBlock<Block> ASTEROID_IRON_ORE = withItem("asteroid_iron_ore", () -> new Block(asteroidRock()));
    /** 1.12.2 "Dark Decoration Block", drawn with the screen side texture. */
    public static final DeferredBlock<Block> DARK_DECORATION_BLOCK = withItem("dark_decoration_block", () -> new Block(asteroidRock()));
    /** 1.12.2 BlockSlabGC asteroids_slab (in the creative tab, with no recipe and no English name). */
    public static final DeferredBlock<SlabBlock> DARK_DECORATION_SLAB = slab("dark_decoration_slab", DARK_DECORATION_BLOCK);
    public static final DeferredBlock<Block> TITANIUM_BLOCK = withItem("titanium_block", () -> new Block(asteroidRock()
            .instrument(NoteBlockInstrument.IRON_XYLOPHONE).sound(SoundType.METAL)));
    // --- Venus (1.12.2 BlockBasicVenus "venus" and the planet's other blocks) ---
    public static final DeferredBlock<Block> VENUS_SOFT_ROCK = withItem("venus_soft_rock", () -> new Block(venusRock(MapColor.TERRACOTTA_YELLOW, 0.9F, 0.5F)));
    public static final DeferredBlock<Block> VENUS_HARD_ROCK = withItem("venus_hard_rock", () -> new Block(venusRock(MapColor.TERRACOTTA_ORANGE, 1.5F, 6.0F)));
    /** Mined (without Silk Touch) it leaves lava behind. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.VolcanicRockBlock> VENUS_VOLCANIC_ROCK = withItem("venus_volcanic_rock",
            () -> new io.github.reiko45.galacticraft.block.VolcanicRockBlock(venusRock(MapColor.COLOR_RED, 2.2F, 0.5F)));
    public static final DeferredBlock<Block> PUMICE = withItem("pumice", () -> new Block(venusRock(MapColor.TERRACOTTA_LIGHT_GRAY, 2.2F, 0.5F)));
    public static final DeferredBlock<Block> VENUS_DUNGEON_BRICK_1 = withItem("venus_dungeon_brick_1",
            () -> new Block(venusRock(MapColor.TERRACOTTA_BROWN, 4.0F, 40.0F)));
    public static final DeferredBlock<Block> VENUS_DUNGEON_BRICK_2 = withItem("venus_dungeon_brick_2",
            () -> new Block(venusRock(MapColor.TERRACOTTA_BROWN, 4.0F, 40.0F)));
    public static final DeferredBlock<Block> VENUS_ALUMINUM_ORE = withItem("venus_aluminum_ore", () -> new Block(venusRock(MapColor.TERRACOTTA_ORANGE, 5.0F, 3.0F)));
    public static final DeferredBlock<Block> VENUS_COPPER_ORE = withItem("venus_copper_ore", () -> new Block(venusRock(MapColor.TERRACOTTA_ORANGE, 5.0F, 3.0F)));
    public static final DeferredBlock<Block> GALENA_ORE = withItem("galena_ore", () -> new Block(venusRock(MapColor.TERRACOTTA_ORANGE, 5.0F, 3.0F)));
    /** Drops Nether Quartz and 2-5 experience (1.12.2 getItemDropped, getExpDrop). */
    public static final DeferredBlock<DropExperienceBlock> VENUS_QUARTZ_ORE = withItem("venus_quartz_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), venusRock(MapColor.TERRACOTTA_ORANGE, 5.0F, 3.0F)));
    /** Drops Raw Silicon (more with Fortune) and 2-5 experience. */
    public static final DeferredBlock<DropExperienceBlock> VENUS_SILICON_ORE = withItem("venus_silicon_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), venusRock(MapColor.TERRACOTTA_ORANGE, 5.0F, 3.0F)));
    public static final DeferredBlock<Block> VENUS_TIN_ORE = withItem("venus_tin_ore", () -> new Block(venusRock(MapColor.TERRACOTTA_ORANGE, 5.0F, 3.0F)));
    public static final DeferredBlock<Block> LEAD_BLOCK = withItem("lead_block", () -> new Block(venusRock(MapColor.COLOR_GRAY, 2.2F, 0.5F)));
    /** Drops Solar Dust (1.12.2 getItemDropped; no experience). */
    public static final DeferredBlock<Block> SOLAR_ORE = withItem("solar_ore", () -> new Block(venusRock(MapColor.TERRACOTTA_ORANGE, 5.0F, 3.0F)));
    /** 1.12.2 BlockScorchedRock: hardness 0.9, smoking. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.ScorchedRockBlock> SCORCHED_VENUS_ROCK = withItem("scorched_venus_rock",
            () -> new io.github.reiko45.galacticraft.block.ScorchedRockBlock(venusRock(MapColor.COLOR_BLACK, 0.9F, 0.5F)));
    /** 1.12.2 BlockSpout: hardness 4.5; drops Venus Soft Rock. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.VaporSpoutBlock> VAPOR_SPOUT = withItem("vapor_spout",
            () -> new io.github.reiko45.galacticraft.block.VaporSpoutBlock(venusRock(MapColor.TERRACOTTA_YELLOW, 4.5F, 0.5F)));
    /** 1.12.2 BlockTorchWeb web_torch_0 and web_torch_1 (the glowing one): Material.CIRCUITS, no hardness. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.WebTorchBlock> WEB_STRING = withItem("web_string",
            () -> new io.github.reiko45.galacticraft.block.WebTorchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollission()
                    .instabreak().noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY), false));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.WebTorchBlock> WEB_TORCH = withItem("web_torch",
            () -> new io.github.reiko45.galacticraft.block.WebTorchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollission()
                    .instabreak().noOcclusion().lightLevel(state -> 15).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY), true));
    /** 1.12.2 BlockCrashedProbe: Material.IRON, hardness 4.5, metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.CrashedProbeBlock> CRASHED_PROBE = withItem("crashed_probe",
            () -> new io.github.reiko45.galacticraft.block.CrashedProbeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(4.5F, 0.5F).sound(SoundType.METAL).noOcclusion()));

    /** 1.12.2 BlockIceAsteroids: Material.ICE, slipperiness 0.98, hardness 0.5, glass sounds, translucent. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.DenseIceBlock> DENSE_ICE = withItem("dense_ice",
            () -> new io.github.reiko45.galacticraft.block.DenseIceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).friction(0.98F)
                    .randomTicks().strength(0.5F).sound(SoundType.GLASS).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false)));
    /** 1.12.2 BlockBrightAir: lights the sunward side of asteroids; no item. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.BrightAirBlock> BRIGHT_AIR = BLOCKS.register("bright_air",
            () -> new io.github.reiko45.galacticraft.block.BrightAirBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air()
                    .lightLevel(state -> state.getValue(io.github.reiko45.galacticraft.block.BrightAirBlock.LEVEL))));

    public static final DeferredBlock<StairBlock> MARS_COBBLESTONE_STAIRS = stairs("mars_cobblestone_stairs", MARS_COBBLESTONE, 1.5F);
    public static final DeferredBlock<StairBlock> MARS_DUNGEON_BRICK_STAIRS = stairs("mars_dungeon_brick_stairs", MARS_DUNGEON_BRICK, 4.0F);
    public static final DeferredBlock<SlabBlock> MARS_COBBLESTONE_SLAB = slab("mars_cobblestone_slab", MARS_COBBLESTONE);
    public static final DeferredBlock<SlabBlock> MARS_DUNGEON_BRICK_SLAB = slab("mars_dungeon_brick_slab", MARS_DUNGEON_BRICK);
    public static final DeferredBlock<WallBlock> MARS_COBBLESTONE_WALL = wall("mars_cobblestone_wall", MARS_COBBLESTONE);
    public static final DeferredBlock<WallBlock> MARS_DUNGEON_BRICK_WALL = wall("mars_dungeon_brick_wall", MARS_DUNGEON_BRICK);

    /** 1.12.2 BlockCavernousVine: Material.VINE, hardness 0.1, plant sound. */
    public static final DeferredBlock<CavernousVinesBlock> CAVERNOUS_VINES = withItem("cavernous_vines", () -> new CavernousVinesBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().randomTicks().strength(0.1F)
                    .sound(SoundType.VINE).noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(CavernousVinesBlock::light)));

    // --- Energy ---
    public static final DeferredBlock<AluminumWireBlock> ALUMINUM_WIRE = withItem("aluminum_wire", () -> new AluminumWireBlock(wire(), false));
    public static final DeferredBlock<AluminumWireBlock> HEAVY_ALUMINUM_WIRE = withItem("heavy_aluminum_wire", () -> new AluminumWireBlock(wire(), true));
    public static final DeferredBlock<SwitchableWireBlock> ALUMINUM_WIRE_SWITCH = withItem("aluminum_wire_switch",
            () -> new SwitchableWireBlock(wire(), false));
    public static final DeferredBlock<SwitchableWireBlock> HEAVY_ALUMINUM_WIRE_SWITCH = withItem("heavy_aluminum_wire_switch",
            () -> new SwitchableWireBlock(wire(), true));

    // --- Machines (1.12.2 BlockMachineBase: hardness 1, metal sound) ---
    public static final DeferredBlock<MachineBlock> COAL_GENERATOR = withItem("coal_generator",
            () -> new MachineBlock(machine(), CoalGeneratorBlockEntity::new));
    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_MODULE = withItem("energy_storage_module",
            () -> new EnergyStorageBlock(machine(), EnergyStorageBlockEntity::module));
    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_CLUSTER = withItem("energy_storage_cluster",
            () -> new EnergyStorageBlock(machine(), EnergyStorageBlockEntity::cluster));
    public static final DeferredBlock<MachineBlock> COMPRESSOR = withItem("compressor",
            () -> new MachineBlock(machine(), CompressorBlockEntity::new));
    public static final DeferredBlock<ConfigurableMachineBlock> ELECTRIC_COMPRESSOR = withItem("electric_compressor",
            () -> new ConfigurableMachineBlock(machine(), ElectricCompressorBlockEntity::normal));
    public static final DeferredBlock<ConfigurableMachineBlock> ADVANCED_COMPRESSOR = withItem("advanced_compressor",
            () -> new ConfigurableMachineBlock(machine(), ElectricCompressorBlockEntity::advanced));
    public static final DeferredBlock<ConfigurableMachineBlock> ELECTRIC_FURNACE = withItem("electric_furnace",
            () -> new ConfigurableMachineBlock(machine(), ElectricFurnaceBlockEntity::furnace));
    public static final DeferredBlock<ConfigurableMachineBlock> ELECTRIC_ARC_FURNACE = withItem("electric_arc_furnace",
            () -> new ConfigurableMachineBlock(machine(), ElectricFurnaceBlockEntity::arcFurnace));
    /** 1.12.2 BlockMachine2 type 10 (Deconstructor). */
    public static final DeferredBlock<ConfigurableMachineBlock> DECONSTRUCTOR = withItem("deconstructor",
            () -> new ConfigurableMachineBlock(machine(), io.github.reiko45.galacticraft.block.entity.DeconstructorBlockEntity::new));
    /** 1.12.2 BlockMachine3 type 9 (painter: Chromatic Applicator). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.ChromaticApplicatorBlock> CHROMATIC_APPLICATOR = withItem("chromatic_applicator",
            () -> new io.github.reiko45.galacticraft.block.ChromaticApplicatorBlock(machine()));
    public static final DeferredBlock<ConfigurableMachineBlock> CIRCUIT_FABRICATOR = withItem("circuit_fabricator",
            () -> new ConfigurableMachineBlock(machine(), CircuitFabricatorBlockEntity::new));

    // Venus machines (1.12.2 BlockGeothermalGenerator: Material.IRON, hardness 2.5; BlockSolarArrayModule: hardness 2.5;
    // BlockSolarArrayController: hardness 1; all with metal sounds)
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.GeothermalGeneratorBlock> GEOTHERMAL_GENERATOR = withItem(
            "geothermal_generator", () -> new io.github.reiko45.galacticraft.block.GeothermalGeneratorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL)));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SolarArrayModuleBlock> SOLAR_ARRAY_MODULE = withItem(
            "solar_array_module", () -> new io.github.reiko45.galacticraft.block.SolarArrayModuleBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SolarArrayControllerBlock> SOLAR_ARRAY_CONTROLLER = withItem(
            "solar_array_controller", () -> new io.github.reiko45.galacticraft.block.SolarArrayControllerBlock(machine().noOcclusion()));

    // Solar panels: base block + invisible pole/panel parts (1.12.2 BlockSolar + BlockMulti)
    public static final DeferredBlock<SolarPanelBlock> BASIC_SOLAR_PANEL = withItem("basic_solar_panel",
            () -> new SolarPanelBlock(machine(), SolarPanelBlockEntity::basic));
    public static final DeferredBlock<SolarPanelBlock> ADVANCED_SOLAR_PANEL = withItem("advanced_solar_panel",
            () -> new SolarPanelBlock(machine(), SolarPanelBlockEntity::advanced));
    public static final DeferredBlock<SolarPartBlock> SOLAR_PART = BLOCKS.register("solar_panel_part", () -> new SolarPartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 3_600_000.0F).sound(SoundType.METAL)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));

    // Mars gas machines (1.12.2 BlockMachineMarsT2: gas_liquefier, methane_synthesizer, electrolyzer)
    public static final DeferredBlock<MachineBlock> GAS_LIQUEFIER = withItem("gas_liquefier",
            () -> new MachineBlock(machine(), io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity::new));
    public static final DeferredBlock<MachineBlock> METHANE_SYNTHESIZER = withItem("methane_synthesizer",
            () -> new MachineBlock(machine(), io.github.reiko45.galacticraft.block.entity.MethaneSynthesizerBlockEntity::new));
    public static final DeferredBlock<MachineBlock> ELECTROLYZER = withItem("electrolyzer",
            () -> new MachineBlock(machine(), io.github.reiko45.galacticraft.block.entity.ElectrolyzerBlockEntity::new));

    // Mars machines (1.12.2 BlockMachineMars: terraformer, cryogenic_chamber, launch_controller)
    public static final DeferredBlock<MachineBlock> TERRAFORMER = withItem("terraformer",
            () -> new MachineBlock(machine(), io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity::new));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.LaunchControllerBlock> LAUNCH_CONTROLLER = withItem("launch_controller",
            () -> new io.github.reiko45.galacticraft.block.LaunchControllerBlock(machine()));
    /** 1.12.2 BlockMachineMars cryogenic_chamber; open at the front, so it neither hides faces nor suffocates. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.CryogenicChamberBlock> CRYOGENIC_CHAMBER = withItem("cryogenic_chamber",
            () -> new io.github.reiko45.galacticraft.block.CryogenicChamberBlock(machine().noOcclusion()
                    .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.CryogenicChamberPartBlock> CRYOGENIC_CHAMBER_PART = BLOCKS.register(
            "cryogenic_chamber_part", () -> new io.github.reiko45.galacticraft.block.CryogenicChamberPartBlock(machine().noOcclusion().noLootTable()
                    .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false).pushReaction(PushReaction.BLOCK)));

    // Oil processing (1.12.2 BlockRefinery, BlockFluidTank, BlockFluidPipe)
    public static final DeferredBlock<MachineBlock> REFINERY = withItem("refinery",
            () -> new MachineBlock(machine(), RefineryBlockEntity::new));
    /** 1.12.2: Material.GLASS, hardness 3, resistance 8. */
    public static final DeferredBlock<FluidTankBlock> FLUID_TANK = withItem("fluid_tank", () -> new FluidTankBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(3.0F, 8.0F).sound(SoundType.GLASS).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));
    /** 1.12.2: Material.GLASS, hardness 0.3. */
    public static final DeferredBlock<FluidPipeBlock> FLUID_PIPE = withItem("fluid_pipe", () -> new FluidPipeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion()));

    // Oxygen (1.12.2 BlockOxygenCollector, BlockOxygenCompressor (compressor + decompressor), BlockMachine2 oxygen
    // storage, BlockOxygenDistributor, BlockOxygenSealer, BlockOxygenDetector)
    public static final DeferredBlock<MachineBlock> OXYGEN_COLLECTOR = withItem("oxygen_collector",
            () -> new MachineBlock(machine(), OxygenCollectorBlockEntity::new));
    public static final DeferredBlock<MachineBlock> OXYGEN_COMPRESSOR = withItem("oxygen_compressor",
            () -> new MachineBlock(machine(), OxygenCompressorBlockEntity::new));
    public static final DeferredBlock<MachineBlock> OXYGEN_DECOMPRESSOR = withItem("oxygen_decompressor",
            () -> new MachineBlock(machine(), OxygenDecompressorBlockEntity::new));
    public static final DeferredBlock<OxygenStorageModuleBlock> OXYGEN_STORAGE_MODULE = withItem("oxygen_storage_module",
            () -> new OxygenStorageModuleBlock(machine(), OxygenStorageModuleBlockEntity::new));
    public static final DeferredBlock<MachineBlock> OXYGEN_DISTRIBUTOR = withItem("oxygen_distributor",
            () -> new MachineBlock(machine(), OxygenDistributorBlockEntity::new));
    public static final DeferredBlock<MachineBlock> OXYGEN_SEALER = withItem("oxygen_sealer",
            () -> new MachineBlock(machine(), OxygenSealerBlockEntity::new));
    public static final DeferredBlock<OxygenDetectorBlock> OXYGEN_DETECTOR = withItem("oxygen_detector",
            () -> new OxygenDetectorBlock(machine()));
    /** 1.12.2 BlockBreathableAir: hardness 0, resistance 1000. */
    public static final DeferredBlock<BreathableAirBlock> BREATHABLE_AIR = BLOCKS.register("breathable_air", () -> new BreathableAirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().strength(0.0F, 1000.0F)
                    .pushReaction(PushReaction.DESTROY)));
    /** 1.12.2 BlockBrightBreathableAir: breathable air an Arc Lamp lights up. */
    public static final DeferredBlock<BreathableAirBlock> BRIGHT_BREATHABLE_AIR = BLOCKS.register("bright_breathable_air", () -> new BreathableAirBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().strength(0.0F, 1000.0F).lightLevel(state -> 15)
                    .pushReaction(PushReaction.DESTROY)));

    // --- Space base blocks (phase 7g) ---
    /** 1.12.2 BlockSpaceGlass: hardness 0.3 (strong 4), resistance 3 (tin frames 4; strong 35). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceGlassBlock> SPACE_GLASS_VANILLA = spaceGlass("space_glass_vanilla",
            io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassType.VANILLA, io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassFrame.PLAIN, 0.3F, 3.0F);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceGlassBlock> SPACE_GLASS_CLEAR = spaceGlass("space_glass_clear",
            io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassType.CLEAR, io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassFrame.PLAIN, 0.3F, 3.0F);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceGlassBlock> SPACE_GLASS_STRONG = spaceGlass("space_glass_strong",
            io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassType.STRONG, io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassFrame.PLAIN, 4.0F, 35.0F);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceGlassBlock> SPACE_GLASS_VANILLA_TIN = spaceGlass("space_glass_vanilla_tin",
            io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassType.VANILLA, io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassFrame.TIN_DECO, 0.3F, 4.0F);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceGlassBlock> SPACE_GLASS_CLEAR_TIN = spaceGlass("space_glass_clear_tin",
            io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassType.CLEAR, io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassFrame.TIN_DECO, 0.3F, 4.0F);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceGlassBlock> SPACE_GLASS_STRONG_TIN = spaceGlass("space_glass_strong_tin",
            io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassType.STRONG, io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassFrame.TIN_DECO, 4.0F, 35.0F);
    /** 1.12.2 BlockAirLockFrame: Material.ROCK, hardness 1, metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.airlock.AirLockFrameBlock> AIR_LOCK_FRAME = withItem("air_lock_frame",
            () -> new io.github.reiko45.galacticraft.block.airlock.AirLockFrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.airlock.AirLockControllerBlock> AIR_LOCK_CONTROLLER = withItem("air_lock_controller",
            () -> new io.github.reiko45.galacticraft.block.airlock.AirLockControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0F).sound(SoundType.METAL)));
    /** 1.12.2 BlockAirLockWall: hardness 1000, no drops. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.airlock.AirLockSealBlock> AIR_LOCK_SEAL = BLOCKS.register("air_lock_seal",
            () -> new io.github.reiko45.galacticraft.block.airlock.AirLockSealBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1000.0F).sound(SoundType.METAL).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    /** 1.12.2 BlockBrightLamp: hardness 0.1, light 0.9 (13). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.ArcLampBlock> ARC_LAMP = withItem("arc_lamp",
            () -> new io.github.reiko45.galacticraft.block.ArcLampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.1F)
                    .sound(SoundType.METAL).lightLevel(state -> 13).noOcclusion()));
    /** 1.12.2 BlockConcealedRedstone / Repeater / Detector: Material.IRON, hardness 1, resistance 15 (3 against blasts). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.concealed.ConcealedRedstoneBlock> CONCEALED_REDSTONE = withItem("concealed_redstone",
            () -> new io.github.reiko45.galacticraft.block.concealed.ConcealedRedstoneBlock(concealed()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.concealed.ConcealedRepeaterBlock> CONCEALED_REPEATER = withItem("concealed_repeater",
            () -> new io.github.reiko45.galacticraft.block.concealed.ConcealedRepeaterBlock(concealed()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.concealed.ConcealedDetectorBlock> CONCEALED_DETECTOR = withItem("concealed_detector",
            () -> new io.github.reiko45.galacticraft.block.concealed.ConcealedDetectorBlock(concealed().noLootTable()));
    /** 1.12.2 BlockEnclosed: Material.CLAY, hardness 0.4, stone sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.EnclosedWireBlock> SEALABLE_ALUMINUM_WIRE = withItem("sealable_aluminum_wire",
            () -> new io.github.reiko45.galacticraft.block.EnclosedWireBlock(enclosed(), false));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.EnclosedWireBlock> SEALABLE_HEAVY_ALUMINUM_WIRE = withItem(
            "sealable_heavy_aluminum_wire", () -> new io.github.reiko45.galacticraft.block.EnclosedWireBlock(enclosed(), true));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.EnclosedPipeBlock> SEALABLE_FLUID_PIPE = withItem("sealable_fluid_pipe",
            () -> new io.github.reiko45.galacticraft.block.EnclosedPipeBlock(enclosed()));
    /** 1.12.2 BlockGrating: hardness 0.5, resistance 15 (3 against blasts), metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.GratingBlock> GRATING = withItem("grating",
            () -> new io.github.reiko45.galacticraft.block.GratingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F, 3.0F)
                    .sound(SoundType.METAL).noOcclusion()));
    /** 1.12.2 BlockPanelLighting: Material.IRON, hardness 1, metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.LightingPanelBlock> LIGHTING_PANEL_SQUARE = lightingPanel("lighting_panel_square",
            io.github.reiko45.galacticraft.block.LightingPanelBlock.PanelType.SQUARE);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.LightingPanelBlock> LIGHTING_PANEL_SPOTS = lightingPanel("lighting_panel_spots",
            io.github.reiko45.galacticraft.block.LightingPanelBlock.PanelType.SPOTS);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.LightingPanelBlock> LIGHTING_PANEL_LINEAR = lightingPanel("lighting_panel_linear",
            io.github.reiko45.galacticraft.block.LightingPanelBlock.PanelType.LINEAR);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.LightingPanelBlock> LIGHTING_PANEL_SF = lightingPanel("lighting_panel_sf",
            io.github.reiko45.galacticraft.block.LightingPanelBlock.PanelType.SF);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.LightingPanelBlock> LIGHTING_PANEL_SFDIAG = lightingPanel("lighting_panel_sfdiag",
            io.github.reiko45.galacticraft.block.LightingPanelBlock.PanelType.SFDIAG);

    /** 1.12.2 BlockUnlitTorch (unlit_torch / unlit_torch_lit as one block with LIT). */
    public static final DeferredBlock<SpaceTorchBlock> UNLIT_TORCH = BLOCKS.register("unlit_torch",
            () -> new SpaceTorchBlock(spaceTorch()));
    public static final DeferredBlock<SpaceWallTorchBlock> UNLIT_WALL_TORCH = BLOCKS.register("unlit_wall_torch",
            () -> new SpaceWallTorchBlock(spaceTorch().lootFrom(UNLIT_TORCH)));
    public static final DeferredItem<StandingAndWallBlockItem> UNLIT_TORCH_ITEM = GCItems.ITEMS.register("unlit_torch",
            () -> new StandingAndWallBlockItem(UNLIT_TORCH.get(), UNLIT_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));

    // --- Rockets (1.12.2 BlockLandingPad(Full), BlockFuelLoader, BlockNasaWorkbench, BlockParaChest) ---
    /** 1.12.2: hardness 1, resistance 10, metal sound. */
    public static final DeferredBlock<LandingPadBlock> LANDING_PAD = withItem("landing_pad", () -> new LandingPadBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.BuggyFuelingPadBlock> BUGGY_FUELING_PAD = withItem("buggy_fueling_pad",
            () -> new io.github.reiko45.galacticraft.block.BuggyFuelingPadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0F, 10.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<MachineBlock> FUEL_LOADER = withItem("fuel_loader",
            () -> new MachineBlock(machine(), FuelLoaderBlockEntity::new));
    /** 1.12.2 BlockCargoLoader (cargo_loader / cargo_unloader). */
    public static final DeferredBlock<MachineBlock> CARGO_LOADER = withItem("cargo_loader",
            () -> new MachineBlock(machine(), io.github.reiko45.galacticraft.block.entity.CargoLoaderBlockEntity::new));
    public static final DeferredBlock<MachineBlock> CARGO_UNLOADER = withItem("cargo_unloader",
            () -> new MachineBlock(machine(), io.github.reiko45.galacticraft.block.entity.CargoUnloaderBlockEntity::new));
    /** 1.12.2: Material.IRON, hardness 2.5, metal sound. */
    public static final DeferredBlock<NasaWorkbenchBlock> NASA_WORKBENCH = withItem("nasa_workbench", () -> new NasaWorkbenchBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion()));
    /** 1.12.2 BlockCompactNasaWorkbench: Material.IRON, hardness 2.5, metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.CompactNasaWorkbenchBlock> COMPACT_NASA_WORKBENCH = withItem("compact_nasa_workbench",
            () -> new io.github.reiko45.galacticraft.block.CompactNasaWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    /** 1.12.2 BlockCrafting (Magnetic Crafting Table): Material.IRON, hardness 0.6. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.MagneticCraftingTableBlock> MAGNETIC_CRAFTING_TABLE = withItem("magnetic_crafting_table",
            () -> new io.github.reiko45.galacticraft.block.MagneticCraftingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.6F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    /** 1.12.2 BlockTelemetry: Material.IRON, hardness 1, metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.TelemetryBlock> TELEMETRY = withItem("telemetry",
            () -> new io.github.reiko45.galacticraft.block.TelemetryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    /** 1.12.2 BlockScreen: Material.CIRCUITS, hardness 0.1, glass sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.DisplayScreenBlock> DISPLAY_SCREEN = withItem("display_screen",
            () -> new io.github.reiko45.galacticraft.block.DisplayScreenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.1F).sound(SoundType.GLASS).noOcclusion()));
    /** 1.12.2 BlockDish: Material.IRON, hardness 1, metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.CommsDishBlock> COMMS_DISH = withItem("comms_dish",
            () -> new io.github.reiko45.galacticraft.block.CommsDishBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    /** 1.12.2 BlockMulti DISH_LARGE: the dish's invisible upper parts. */
    public static final DeferredBlock<SolarPartBlock> COMMS_DISH_PART = BLOCKS.register("comms_dish_part", () -> new SolarPartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 3_600_000.0F).sound(SoundType.METAL)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    /** 1.12.2 BlockEmergencyBox: Material.IRON, hardness 4, resistance 70 (42 against blasts), light 15, metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.EmergencyPostBlock> EMERGENCY_POST = BLOCKS.register("emergency_post",
            () -> new io.github.reiko45.galacticraft.block.EmergencyPostBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(4.0F, 42.0F).sound(SoundType.METAL).lightLevel(state -> 15).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<io.github.reiko45.galacticraft.item.EmergencyPostItem> EMERGENCY_POST_ITEM = GCItems.ITEMS.register("emergency_post",
            () -> new io.github.reiko45.galacticraft.item.EmergencyPostItem(EMERGENCY_POST.get(), new Item.Properties()));
    /** 1.12.2 BlockPlatform: Material.IRON, hardness 1, resistance 10 (6 against blasts), metal sounds. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.HydraulicPlatformBlock> HYDRAULIC_PLATFORM = withItem("hydraulic_platform",
            () -> new io.github.reiko45.galacticraft.block.HydraulicPlatformBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<SolarPartBlock> NASA_WORKBENCH_PART = BLOCKS.register("nasa_workbench_part", () -> new SolarPartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F, 3_600_000.0F).sound(SoundType.METAL)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    /** 1.12.2: Material.WOOD, hardness 3, wood sound. */
    public static final DeferredBlock<ParachestBlock> PARACHEST = withItem("parachest", () -> new ParachestBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));

    // --- Space stations (1.12.2 BlockSpaceStationBase) ---
    /** Unbreakable (hardness -1), no item. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceStationBaseBlock> SPACE_STATION_BASE = BLOCKS.register("space_station_base",
            () -> new io.github.reiko45.galacticraft.block.SpaceStationBaseBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(-1.0F, 3_600_000.0F).sound(SoundType.METAL).noLootTable().pushReaction(PushReaction.BLOCK)));

    /** 1.12.2: Material.CIRCUITS, hardness 0.1, metal sound. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpinThrusterBlock> SPIN_THRUSTER = withItem("spin_thruster",
            () -> new io.github.reiko45.galacticraft.block.SpinThrusterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.1F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));

    // --- Moon dungeon (1.12.2 BlockBossSpawner, BlockTier1TreasureChest) ---
    /** Invisible, unbreakable holder of the boss room's spawner block entity. */
    public static final DeferredBlock<BossSpawnerBlock> BOSS_SPAWNER = BLOCKS.register("boss_spawner", () -> new BossSpawnerBlock(
            BossSpawnerBlock.Boss.SKELETON, bossSpawner()));
    /** 1.12.2: hardness 100,000 (only the key opens it). */
    public static final DeferredBlock<TreasureChestBlock> TREASURE_CHEST_TIER_1 = withItem("treasure_chest_tier_1", () -> new TreasureChestBlock(1,
            treasureChest()));

    // --- Mars dungeon (1.12.2 BlockBossSpawnerMars, BlockTier2TreasureChest, BlockCreeperEgg) ---
    public static final DeferredBlock<BossSpawnerBlock> MARS_BOSS_SPAWNER = BLOCKS.register("mars_boss_spawner", () -> new BossSpawnerBlock(
            BossSpawnerBlock.Boss.CREEPER, bossSpawner()));
    public static final DeferredBlock<TreasureChestBlock> TREASURE_CHEST_TIER_2 = withItem("treasure_chest_tier_2", () -> new TreasureChestBlock(2,
            treasureChest()));
    // --- Venus dungeon (1.12.2 BlockBossSpawnerVenus, BlockTier3TreasureChest) ---
    public static final DeferredBlock<BossSpawnerBlock> VENUS_BOSS_SPAWNER = BLOCKS.register("venus_boss_spawner", () -> new BossSpawnerBlock(
            BossSpawnerBlock.Boss.SPIDER_QUEEN, bossSpawner()));
    public static final DeferredBlock<TreasureChestBlock> TREASURE_CHEST_TIER_3 = withItem("treasure_chest_tier_3", () -> new TreasureChestBlock(3,
            treasureChest()));
    /**
     * 1.12.2 BlockCreeperEgg (a dragon egg that doesn't teleport): falls, can't be broken except with the Sticky Desh
     * Pickaxe, and hatches a baby Evolved Creeper when an explosion breaks it.
     */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.CreeperEggBlock> CREEPER_EGG = withItem("creeper_egg",
            () -> new io.github.reiko45.galacticraft.block.CreeperEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(-1.0F, 0.0F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK)));
    // 1.12.2 BlockSlimelingEgg (Material.ROCK, hardness 0.75, pickaxe level 3), one block per egg colour
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SlimelingEggBlock> RED_SLIMELING_EGG = slimelingEgg("red_slimeling_egg",
            io.github.reiko45.galacticraft.block.SlimelingEggBlock.Color.RED);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SlimelingEggBlock> BLUE_SLIMELING_EGG = slimelingEgg("blue_slimeling_egg",
            io.github.reiko45.galacticraft.block.SlimelingEggBlock.Color.BLUE);
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SlimelingEggBlock> YELLOW_SLIMELING_EGG = slimelingEgg("yellow_slimeling_egg",
            io.github.reiko45.galacticraft.block.SlimelingEggBlock.Color.YELLOW);

    // --- Misc ---
    public static final DeferredBlock<CheeseBlock> CHEESE_BLOCK = withItem("cheese_block", () -> new CheeseBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5F).sound(SoundType.WOOL)
                    .forceSolidOn().pushReaction(PushReaction.DESTROY)));

    /** Hardness 40 like 1.12.2; drops raw meteoric iron; lands hot from meteor showers. */
    public static final DeferredBlock<FallenMeteorBlock> FALLEN_METEOR = withItem("fallen_meteor", () -> new FallenMeteorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(40.0F, 40.0F).sound(SoundType.STONE).noOcclusion()));

    public static final DeferredBlock<GlowstoneTorchBlock> GLOWSTONE_TORCH = BLOCKS.register("glowstone_torch",
            () -> new GlowstoneTorchBlock(torch()));
    public static final DeferredBlock<GlowstoneWallTorchBlock> GLOWSTONE_WALL_TORCH = BLOCKS.register("glowstone_wall_torch",
            () -> new GlowstoneWallTorchBlock(torch().lootFrom(GLOWSTONE_TORCH)));

    static {
        // Torches need a combined standing/wall item instead of a plain BlockItem.
        GCItems.ITEMS.register("glowstone_torch",
                () -> new StandingAndWallBlockItem(GLOWSTONE_TORCH.get(), GLOWSTONE_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));
    }

    private GCBlocks() {
    }

    /** Blocks that have a plain {@link net.minecraft.world.item.BlockItem}, in registration order. */
    public static List<DeferredBlock<?>> blocksWithItems() {
        return Collections.unmodifiableList(BLOCKS_WITH_ITEMS);
    }

    private static DeferredBlock<io.github.reiko45.galacticraft.block.SlimelingEggBlock> slimelingEgg(String name,
            io.github.reiko45.galacticraft.block.SlimelingEggBlock.Color color) {
        return withItem(name, () -> new io.github.reiko45.galacticraft.block.SlimelingEggBlock(color, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                .strength(0.75F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY)));
    }

    // --- Phase 7e: asteroid machines and decoration ---
    /** 1.12.2 BlockWalkway: Material.IRON, hardness 1, metal sound. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.WalkwayBlock> WALKWAY = withItem("walkway",
            () -> new io.github.reiko45.galacticraft.block.WalkwayBlock(walkway()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.WalkwayWireBlock> WALKWAY_WIRE = withItem("walkway_wire",
            () -> new io.github.reiko45.galacticraft.block.WalkwayWireBlock(walkway()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.WalkwayPipeBlock> WALKWAY_PIPE = withItem("walkway_pipe",
            () -> new io.github.reiko45.galacticraft.block.WalkwayPipeBlock(walkway()));
    /** 1.12.2 BlockBeamReflector / BlockBeamReceiver (BlockAdvanced: hardness 0.6, resistance 2.5 x 3 / 5). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.BeamReflectorBlock> BEAM_REFLECTOR = withItem("beam_reflector",
            () -> new io.github.reiko45.galacticraft.block.BeamReflectorBlock(beam()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.BeamReceiverBlock> BEAM_RECEIVER = withItem("beam_receiver",
            () -> new io.github.reiko45.galacticraft.block.BeamReceiverBlock(beam()));
    /** 1.12.2 BlockShortRangeTelepad (hardness 3) and BlockTelepadFake (as hard as its telepad, unbreakable by explosions). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.ShortRangeTelepadBlock> SHORT_RANGE_TELEPAD = withItem("short_range_telepad",
            () -> new io.github.reiko45.galacticraft.block.ShortRangeTelepadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.TelepadFakeBlock> TELEPAD_FAKE = BLOCKS.register("telepad_fake",
            () -> new io.github.reiko45.galacticraft.block.TelepadFakeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0F, 3_600_000.0F).sound(SoundType.METAL).noOcclusion().noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    /** 1.12.2 BlockMinerBase (Material.ROCK, hardness 3) and BlockMinerBaseFull (resistance 35). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.MinerBaseBlock> MINER_BASE = withItem("miner_base",
            () -> new io.github.reiko45.galacticraft.block.MinerBaseBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.MinerBaseFullBlock> MINER_BASE_FULL = BLOCKS.register("miner_base_full",
            () -> new io.github.reiko45.galacticraft.block.MinerBaseFullBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0F, 21.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    /** 1.12.2 BlockSpaceWart (nether wart, no item). */
    public static final DeferredBlock<io.github.reiko45.galacticraft.block.SpaceWartBlock> SPACE_WART = BLOCKS.register("space_wart",
            () -> new io.github.reiko45.galacticraft.block.SpaceWartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_WART)));

    private static BlockBehaviour.Properties beam() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.6F, 1.5F).sound(SoundType.METAL).noOcclusion();
    }

    private static BlockBehaviour.Properties walkway() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion();
    }

    private static DeferredBlock<io.github.reiko45.galacticraft.block.SpaceGlassBlock> spaceGlass(String name,
            io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassType type, io.github.reiko45.galacticraft.block.SpaceGlassBlock.GlassFrame frame,
            float hardness, float resistance) {
        return withItem(name, () -> new io.github.reiko45.galacticraft.block.SpaceGlassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                .strength(hardness, resistance).sound(SoundType.GLASS).noOcclusion().dynamicShape().isValidSpawn((s, l, p, e) -> false)
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false), type, frame));
    }

    private static DeferredBlock<io.github.reiko45.galacticraft.block.LightingPanelBlock> lightingPanel(String name,
            io.github.reiko45.galacticraft.block.LightingPanelBlock.PanelType type) {
        return withItem(name, () -> new io.github.reiko45.galacticraft.block.LightingPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(1.0F).sound(SoundType.METAL), type));
    }

    private static BlockBehaviour.Properties concealed() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties enclosed() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.4F, 0.6F).sound(SoundType.STONE);
    }

    private static <B extends Block> DeferredBlock<B> withItem(String name, Supplier<B> factory) {
        final DeferredBlock<B> block = BLOCKS.register(name, factory);
        GCItems.ITEMS.registerSimpleBlockItem(block);
        BLOCKS_WITH_ITEMS.add(block);
        return block;
    }

    private static DeferredBlock<StairBlock> stairs(String name, DeferredBlock<Block> base) {
        return withItem(name, () -> new StairBlock(base.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(base.get())));
    }

    /** Stairs whose 1.12.2 hardness differed from their base block (the explosion resistance follows the base). */
    private static DeferredBlock<StairBlock> stairs(String name, DeferredBlock<Block> base, float hardness) {
        return withItem(name, () -> new StairBlock(base.get().defaultBlockState(),
                BlockBehaviour.Properties.ofFullCopy(base.get()).destroyTime(hardness)));
    }

    private static DeferredBlock<SlabBlock> slab(String name, DeferredBlock<Block> base) {
        return withItem(name, () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(base.get())));
    }

    private static DeferredBlock<WallBlock> wall(String name, DeferredBlock<Block> base) {
        return withItem(name, () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(base.get()).forceSolidOn()));
    }

    private static BlockBehaviour.Properties ore(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.METAL);
    }

    /** 1.12.2 decoration blocks were "soft, like cauldrons": hardness 1, resistance 2. */
    private static BlockBehaviour.Properties decoration() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.0F, 2.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties moonSoil(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.5F, 0.5F).sound(SoundType.GRAVEL);
    }

    private static BlockBehaviour.Properties moonStone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.STONE);
    }

    /**
     * 1.12.2 BlockBasicVenus "venus": Material.ROCK, hardness 2.2 unless getBlockHardness says otherwise, blast resistance
     * as getExplosionResistance gave it (0.5 for most).
     */
    private static BlockBehaviour.Properties venusRock(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(hardness, resistance).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties asteroidRock() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(3.0F, 0.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties marsRock(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties bossSpawner() {
        return BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).noLootTable().noCollission().noOcclusion()
                .pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false);
    }

    private static BlockBehaviour.Properties treasureChest() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(100_000.0F, 3_600_000.0F).sound(SoundType.STONE)
                .noOcclusion().pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.METAL);
    }

    /** 1.12.2 BlockAluminumWire: Material.CLOTH, hardness 0.075, cloth sound. */
    private static BlockBehaviour.Properties wire() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.075F).sound(SoundType.WOOL).noOcclusion();
    }

    private static BlockBehaviour.Properties spaceTorch() {
        return BlockBehaviour.Properties.of().noCollission().instabreak().lightLevel(SpaceTorchBlock::light)
                .sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY);
    }

    /** setLightLevel(0.85F) in 1.12.2 == light level 12. */
    private static BlockBehaviour.Properties torch() {
        return BlockBehaviour.Properties.of().noCollission().instabreak().lightLevel(state -> 12)
                .sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY);
    }
}
