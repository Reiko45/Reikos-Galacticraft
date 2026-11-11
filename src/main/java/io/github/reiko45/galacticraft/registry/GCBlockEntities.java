package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricFurnaceBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
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
            () -> BlockEntityType.Builder.of(WireBlockEntity::new, GCBlocks.ALUMINUM_WIRE.get(), GCBlocks.HEAVY_ALUMINUM_WIRE.get()).build(null));

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

    /** Every machine block entity type (for capability registration). */
    public static List<Supplier<? extends BlockEntityType<? extends MachineBlockEntity>>> machineTypes() {
        return List.copyOf(MACHINES);
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
