package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenMachineBlockEntity;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.gear.InfiniteOxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.energy.GCEnergy;
import io.github.reiko45.galacticraft.energy.IGCEnergyHandler;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes machine energy (gJ and FE), inventories and fluid tanks to wires, pipes, hoppers and other mods. */
public final class GCCapabilities {
    private GCCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        for (Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type : GCBlockEntities.machineTypes()) {
            registerMachine(event, type.get());
        }
        // 1.12.2 TileEntityCrafting was an ISidedInventory: hoppers fill the grid and take the results
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GCBlockEntities.MAGNETIC_CRAFTING_TABLE.get(),
                (table, side) -> new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(table, side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.REFINERY.get(), RefineryBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.GAS_LIQUEFIER.get(),
                io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.METHANE_SYNTHESIZER.get(),
                io.github.reiko45.galacticraft.block.entity.MethaneSynthesizerBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.ELECTROLYZER.get(),
                io.github.reiko45.galacticraft.block.entity.ElectrolyzerBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.TERRAFORMER.get(),
                io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.FUEL_LOADER.get(), FuelLoaderBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.FLUID_TANK.get(), (tank, side) -> tank.stackHandler());
        // The telepad's canopy middle takes power for it on its top face
        event.registerBlockEntity(GCEnergy.BLOCK, GCBlockEntities.TELEPAD_FAKE.get(),
                io.github.reiko45.galacticraft.block.entity.TelepadFakeBlockEntity::energyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GCBlockEntities.TELEPAD_FAKE.get(), (fake, side) -> {
            final IGCEnergyHandler handler = fake.energyHandler(side);
            return handler != null ? GCEnergy.asFE(handler) : null;
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.FLUID_PIPE.get(), (pipe, side) -> pipe.fluidHandler());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new LiquidCanisterItem.Handler(stack),
                LiquidCanisterItem.items());
        for (Supplier<? extends BlockEntityType<? extends OxygenMachineBlockEntity>> type : GCBlockEntities.oxygenMachineTypes()) {
            registerOxygen(event, type.get());
        }
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new OxygenTankItem.Handler(stack),
                GCItems.LIGHT_OXYGEN_TANK.get(), GCItems.MEDIUM_OXYGEN_TANK.get(), GCItems.HEAVY_OXYGEN_TANK.get());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new InfiniteOxygenTankItem.Handler(stack),
                GCItems.INFINITE_OXYGEN.get());
    }

    private static <T extends OxygenMachineBlockEntity> void registerOxygen(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, OxygenMachineBlockEntity::fluidHandler);
    }

    private static <T extends MachineBlockEntity> void registerMachine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(GCEnergy.BLOCK, type, MachineBlockEntity::energyHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (machine, side) -> {
            final IGCEnergyHandler handler = machine.energyHandler(side);
            return handler != null ? GCEnergy.asFE(handler) : null;
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, MachineBlockEntity::itemHandler);
    }
}
