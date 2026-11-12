package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricFurnaceBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FallenMeteorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FluidPipeBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ParachestBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenCollectorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDecompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDetectorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDistributorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenStorageModuleBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPanelBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.block.entity.WireBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GalacticraftCore.MOD_ID);
    private static final List<Supplier<? extends BlockEntityType<? extends MachineBlockEntity>>> MACHINES = new ArrayList<>();

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<WireBlockEntity>> WIRE = BLOCK_ENTITIES.register("wire",
            () -> BlockEntityType.Builder.of(WireBlockEntity::new, GCBlocks.ALUMINUM_WIRE.get(), GCBlocks.HEAVY_ALUMINUM_WIRE.get(),
                    GCBlocks.ALUMINUM_WIRE_SWITCH.get(), GCBlocks.HEAVY_ALUMINUM_WIRE_SWITCH.get(), GCBlocks.WALKWAY_WIRE.get()).build(null));

    public static final Supplier<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR = machine("coal_generator",
            CoalGeneratorBlockEntity::new, GCBlocks.COAL_GENERATOR);
    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_MODULE = machine("energy_storage_module",
            EnergyStorageBlockEntity::module, GCBlocks.ENERGY_STORAGE_MODULE);
    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_CLUSTER = machine("energy_storage_cluster",
            EnergyStorageBlockEntity::cluster, GCBlocks.ENERGY_STORAGE_CLUSTER);
    public static final Supplier<BlockEntityType<CompressorBlockEntity>> COMPRESSOR = machine("compressor",
            CompressorBlockEntity::new, GCBlocks.COMPRESSOR);
    public static final Supplier<BlockEntityType<ElectricCompressorBlockEntity>> ELECTRIC_COMPRESSOR = machine("electric_compressor",
            ElectricCompressorBlockEntity::normal, GCBlocks.ELECTRIC_COMPRESSOR);
    public static final Supplier<BlockEntityType<ElectricCompressorBlockEntity>> ADVANCED_COMPRESSOR = machine("advanced_compressor",
            ElectricCompressorBlockEntity::advanced, GCBlocks.ADVANCED_COMPRESSOR);
    public static final Supplier<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE = machine("electric_furnace",
            ElectricFurnaceBlockEntity::furnace, GCBlocks.ELECTRIC_FURNACE);
    public static final Supplier<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_ARC_FURNACE = machine("electric_arc_furnace",
            ElectricFurnaceBlockEntity::arcFurnace, GCBlocks.ELECTRIC_ARC_FURNACE);
    public static final Supplier<BlockEntityType<CircuitFabricatorBlockEntity>> CIRCUIT_FABRICATOR = machine("circuit_fabricator",
            CircuitFabricatorBlockEntity::new, GCBlocks.CIRCUIT_FABRICATOR);

    public static final Supplier<BlockEntityType<SolarPanelBlockEntity>> BASIC_SOLAR_PANEL = machine("basic_solar_panel",
            SolarPanelBlockEntity::basic, GCBlocks.BASIC_SOLAR_PANEL);
    public static final Supplier<BlockEntityType<SolarPanelBlockEntity>> ADVANCED_SOLAR_PANEL = machine("advanced_solar_panel",
            SolarPanelBlockEntity::advanced, GCBlocks.ADVANCED_SOLAR_PANEL);
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<SolarPartBlockEntity>> SOLAR_PART = BLOCK_ENTITIES.register("solar_panel_part",
            () -> BlockEntityType.Builder.of(SolarPartBlockEntity::new, GCBlocks.SOLAR_PART.get(), GCBlocks.NASA_WORKBENCH_PART.get()).build(null));

    public static final Supplier<BlockEntityType<RefineryBlockEntity>> REFINERY = machine("refinery",
            RefineryBlockEntity::new, GCBlocks.REFINERY);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity>> GAS_LIQUEFIER = machine("gas_liquefier",
            io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity::new, GCBlocks.GAS_LIQUEFIER);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.MethaneSynthesizerBlockEntity>> METHANE_SYNTHESIZER = machine(
            "methane_synthesizer", io.github.reiko45.galacticraft.block.entity.MethaneSynthesizerBlockEntity::new, GCBlocks.METHANE_SYNTHESIZER);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.ElectrolyzerBlockEntity>> ELECTROLYZER = machine("electrolyzer",
            io.github.reiko45.galacticraft.block.entity.ElectrolyzerBlockEntity::new, GCBlocks.ELECTROLYZER);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity>> LAUNCH_CONTROLLER = machine(
            "launch_controller", io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity::new, GCBlocks.LAUNCH_CONTROLLER);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity>> TERRAFORMER = machine("terraformer",
            io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity::new, GCBlocks.TERRAFORMER);
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK = BLOCK_ENTITIES.register("fluid_tank",
            () -> BlockEntityType.Builder.of(FluidTankBlockEntity::new, GCBlocks.FLUID_TANK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE = BLOCK_ENTITIES.register("fluid_pipe",
            () -> BlockEntityType.Builder.of(FluidPipeBlockEntity::new, GCBlocks.FLUID_PIPE.get(), GCBlocks.WALKWAY_PIPE.get()).build(null));

    public static final Supplier<BlockEntityType<OxygenCollectorBlockEntity>> OXYGEN_COLLECTOR = machine("oxygen_collector",
            OxygenCollectorBlockEntity::new, GCBlocks.OXYGEN_COLLECTOR);
    public static final Supplier<BlockEntityType<OxygenCompressorBlockEntity>> OXYGEN_COMPRESSOR = machine("oxygen_compressor",
            OxygenCompressorBlockEntity::new, GCBlocks.OXYGEN_COMPRESSOR);
    public static final Supplier<BlockEntityType<OxygenDecompressorBlockEntity>> OXYGEN_DECOMPRESSOR = machine("oxygen_decompressor",
            OxygenDecompressorBlockEntity::new, GCBlocks.OXYGEN_DECOMPRESSOR);
    public static final Supplier<BlockEntityType<OxygenStorageModuleBlockEntity>> OXYGEN_STORAGE_MODULE = machine("oxygen_storage_module",
            OxygenStorageModuleBlockEntity::new, GCBlocks.OXYGEN_STORAGE_MODULE);
    public static final Supplier<BlockEntityType<OxygenDistributorBlockEntity>> OXYGEN_DISTRIBUTOR = machine("oxygen_distributor",
            OxygenDistributorBlockEntity::new, GCBlocks.OXYGEN_DISTRIBUTOR);
    public static final Supplier<BlockEntityType<OxygenSealerBlockEntity>> OXYGEN_SEALER = machine("oxygen_sealer",
            OxygenSealerBlockEntity::new, GCBlocks.OXYGEN_SEALER);
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<OxygenDetectorBlockEntity>> OXYGEN_DETECTOR = BLOCK_ENTITIES.register("oxygen_detector",
            () -> BlockEntityType.Builder.of(OxygenDetectorBlockEntity::new, GCBlocks.OXYGEN_DETECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<FuelLoaderBlockEntity>> FUEL_LOADER = machine("fuel_loader",
            FuelLoaderBlockEntity::new, GCBlocks.FUEL_LOADER);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.CargoLoaderBlockEntity>> CARGO_LOADER = machine("cargo_loader",
            io.github.reiko45.galacticraft.block.entity.CargoLoaderBlockEntity::new, GCBlocks.CARGO_LOADER);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.CargoUnloaderBlockEntity>> CARGO_UNLOADER = machine(
            "cargo_unloader", io.github.reiko45.galacticraft.block.entity.CargoUnloaderBlockEntity::new, GCBlocks.CARGO_UNLOADER);
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<ParachestBlockEntity>> PARACHEST = BLOCK_ENTITIES.register("parachest",
            () -> BlockEntityType.Builder.of(ParachestBlockEntity::new, GCBlocks.PARACHEST.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<FallenMeteorBlockEntity>> FALLEN_METEOR = BLOCK_ENTITIES.register("fallen_meteor",
            () -> BlockEntityType.Builder.of(FallenMeteorBlockEntity::new, GCBlocks.FALLEN_METEOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.VaporSpoutBlockEntity>> VAPOR_SPOUT = BLOCK_ENTITIES.register(
            "vapor_spout", () -> BlockEntityType.Builder.of(io.github.reiko45.galacticraft.block.entity.VaporSpoutBlockEntity::new,
                    GCBlocks.VAPOR_SPOUT.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.CrashedProbeBlockEntity>> CRASHED_PROBE = BLOCK_ENTITIES.register(
            "crashed_probe", () -> BlockEntityType.Builder.of(io.github.reiko45.galacticraft.block.entity.CrashedProbeBlockEntity::new,
                    GCBlocks.CRASHED_PROBE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.SlimelingEggBlockEntity>> SLIMELING_EGG = BLOCK_ENTITIES.register("slimeling_egg",
            () -> BlockEntityType.Builder.of(io.github.reiko45.galacticraft.block.entity.SlimelingEggBlockEntity::new, GCBlocks.RED_SLIMELING_EGG.get(),
                    GCBlocks.BLUE_SLIMELING_EGG.get(), GCBlocks.YELLOW_SLIMELING_EGG.get()).build(null));
    public static final Supplier<BlockEntityType<DungeonSpawnerBlockEntity>> DUNGEON_SPAWNER = BLOCK_ENTITIES.register("dungeon_spawner",
            () -> BlockEntityType.Builder.of(DungeonSpawnerBlockEntity::new, GCBlocks.BOSS_SPAWNER.get(), GCBlocks.MARS_BOSS_SPAWNER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.BeamReflectorBlockEntity>> BEAM_REFLECTOR = BLOCK_ENTITIES.register(
            "beam_reflector", () -> BlockEntityType.Builder.of(io.github.reiko45.galacticraft.block.entity.BeamReflectorBlockEntity::new,
                    GCBlocks.BEAM_REFLECTOR.get()).build(null));
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.BeamReceiverBlockEntity>> BEAM_RECEIVER = BLOCK_ENTITIES.register(
            "beam_receiver", () -> BlockEntityType.Builder.of(io.github.reiko45.galacticraft.block.entity.BeamReceiverBlockEntity::new,
                    GCBlocks.BEAM_RECEIVER.get()).build(null));
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity>> SHORT_RANGE_TELEPAD = machine(
            "short_range_telepad", io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity::new, GCBlocks.SHORT_RANGE_TELEPAD);
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.TelepadFakeBlockEntity>> TELEPAD_FAKE = BLOCK_ENTITIES.register(
            "telepad_fake", () -> BlockEntityType.Builder.of(io.github.reiko45.galacticraft.block.entity.TelepadFakeBlockEntity::new,
                    GCBlocks.TELEPAD_FAKE.get()).build(null));
    public static final Supplier<BlockEntityType<io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity>> MINER_BASE = machine(
            "miner_base", io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity::new, GCBlocks.MINER_BASE_FULL);
    public static final Supplier<BlockEntityType<TreasureChestBlockEntity>> TREASURE_CHEST = BLOCK_ENTITIES.register("treasure_chest",
            () -> BlockEntityType.Builder.of(TreasureChestBlockEntity::new, GCBlocks.TREASURE_CHEST_TIER_1.get(),
                    GCBlocks.TREASURE_CHEST_TIER_2.get()).build(null));

    /** Every machine block entity type (for capability registration). */
    public static List<Supplier<? extends BlockEntityType<? extends MachineBlockEntity>>> machineTypes() {
        return List.copyOf(MACHINES);
    }

    /** The oxygen machines (fluid capability for oxygen). */
    public static List<Supplier<? extends BlockEntityType<? extends io.github.reiko45.galacticraft.block.entity.OxygenMachineBlockEntity>>> oxygenMachineTypes() {
        return List.of(OXYGEN_COLLECTOR, OXYGEN_COMPRESSOR, OXYGEN_DECOMPRESSOR, OXYGEN_STORAGE_MODULE, OXYGEN_DISTRIBUTOR, OXYGEN_SEALER);
    }

    @SuppressWarnings("DataFlowIssue")
    private static <T extends MachineBlockEntity> Supplier<BlockEntityType<T>> machine(String name,
            BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        final Supplier<BlockEntityType<T>> type = BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        MACHINES.add(type);
        return type;
    }

    private GCBlockEntities() {
    }
}
