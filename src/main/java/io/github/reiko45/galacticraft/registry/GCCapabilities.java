package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
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
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.REFINERY.get(), RefineryBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.FLUID_TANK.get(), (tank, side) -> tank.stackHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GCBlockEntities.FLUID_PIPE.get(), (pipe, side) -> pipe.fluidHandler());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new LiquidCanisterItem.Handler(stack),
                GCItems.OIL_CANISTER.get(), GCItems.FUEL_CANISTER.get());
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
