/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityCargoLoader). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.entity.CargoCarrier;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Cargo Loader: its items go into the rocket or buggy on the pad; only batteries come out (1.12.2 canExtractItem false). */
public class CargoLoaderBlockEntity extends CargoMachineBlockEntity {
    public CargoLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.CARGO_LOADER.get(), pos, state);
    }

    @Override
    public boolean loads() {
        return true;
    }

    /** 1.12.2 update: out of items, a full target or one without storage stop it. */
    @Override
    protected MachineStatus transfer(CargoCarrier carrier, boolean doMove) {
        final ItemStack item = takeOwn(false).stack();
        if (item.isEmpty()) {
            return MachineStatus.NO_ITEMS;
        }
        final CargoCarrier.LoadState state = carrier.addCargo(item, false);
        if (state != CargoCarrier.LoadState.SUCCESS) {
            return state == CargoCarrier.LoadState.NO_INVENTORY ? MachineStatus.NO_INVENTORY_TARGET : MachineStatus.TARGET_FULL;
        }
        if (doMove) {
            carrier.addCargo(takeOwn(true).stack(), true);
        }
        return null;
    }
}
