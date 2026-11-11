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
    public static final DeferredBlock<Block> MARS_DUNGEON_BRICK = withItem("mars_dungeon_brick", () -> new Block(marsRock(MapColor.COLOR_GREEN, 4.0F, 40.0F)));
    public static final DeferredBlock<Block> DESH_BLOCK = withItem("desh_block", () -> new Block(marsRock(MapColor.COLOR_RED, 2.2F, 60.0F)
            .instrument(NoteBlockInstrument.IRON_XYLOPHONE).sound(SoundType.METAL)));

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
    public static final DeferredBlock<ConfigurableMachineBlock> CIRCUIT_FABRICATOR = withItem("circuit_fabricator",
            () -> new ConfigurableMachineBlock(machine(), CircuitFabricatorBlockEntity::new));

    // Solar panels: base block + invisible pole/panel parts (1.12.2 BlockSolar + BlockMulti)
    public static final DeferredBlock<SolarPanelBlock> BASIC_SOLAR_PANEL = withItem("basic_solar_panel",
            () -> new SolarPanelBlock(machine(), SolarPanelBlockEntity::basic));
    public static final DeferredBlock<SolarPanelBlock> ADVANCED_SOLAR_PANEL = withItem("advanced_solar_panel",
            () -> new SolarPanelBlock(machine(), SolarPanelBlockEntity::advanced));
    public static final DeferredBlock<SolarPartBlock> SOLAR_PART = BLOCKS.register("solar_panel_part", () -> new SolarPartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 3_600_000.0F).sound(SoundType.METAL)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));

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
    /** 1.12.2: Material.IRON, hardness 2.5, metal sound. */
    public static final DeferredBlock<NasaWorkbenchBlock> NASA_WORKBENCH = withItem("nasa_workbench", () -> new NasaWorkbenchBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion()));
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
            BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).noLootTable().noCollission().noOcclusion()
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)));
    /** 1.12.2: hardness 100,000 (only the key opens it). */
    public static final DeferredBlock<TreasureChestBlock> TREASURE_CHEST_TIER_1 = withItem("treasure_chest_tier_1", () -> new TreasureChestBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(100_000.0F, 3_600_000.0F).sound(SoundType.STONE)
                    .noOcclusion().pushReaction(PushReaction.BLOCK)));

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

    private static BlockBehaviour.Properties marsRock(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.STONE);
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
