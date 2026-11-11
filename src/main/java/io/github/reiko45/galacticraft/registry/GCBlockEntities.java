package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
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
                    GCBlocks.ALUMINUM_WIRE_SWITCH.get(), GCBlocks.HEAVY_ALUMINUM_WIRE_SWITCH.get()).build(null));

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
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK = BLOCK_ENTITIES.register("fluid_tank",
            () -> BlockEntityType.Builder.of(FluidTankBlockEntity::new, GCBlocks.FLUID_TANK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE = BLOCK_ENTITIES.register("fluid_pipe",
            () -> BlockEntityType.Builder.of(FluidPipeBlockEntity::new, GCBlocks.FLUID_PIPE.get()).build(null));

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
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<ParachestBlockEntity>> PARACHEST = BLOCK_ENTITIES.register("parachest",
            () -> BlockEntityType.Builder.of(ParachestBlockEntity::new, GCBlocks.PARACHEST.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<FallenMeteorBlockEntity>> FALLEN_METEOR = BLOCK_ENTITIES.register("fallen_meteor",
            () -> BlockEntityType.Builder.of(FallenMeteorBlockEntity::new, GCBlocks.FALLEN_METEOR.get()).build(null));

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
