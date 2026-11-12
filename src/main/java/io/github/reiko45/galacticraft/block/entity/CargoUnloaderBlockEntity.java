/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityCargoUnloader). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.entity.CargoCarrier;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Cargo Unloader: takes the rocket's or buggy's cargo into its own slots, which hoppers and pipes can empty. */
public class CargoUnloaderBlockEntity extends CargoMachineBlockEntity {
    public CargoUnloaderBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.CARGO_UNLOADER.get(), pos, state);
    }

    @Override
    public boolean loads() {
        return false;
    }

    /** 1.12.2 update: an empty target, or one without storage, stops it; so does a full unloader. */
    @Override
    protected MachineStatus transfer(CargoCarrier carrier, boolean doMove) {
        final CargoCarrier.Removal removal = carrier.removeCargo(false);
        if (removal.stack().isEmpty()) {
            return removal.state() == CargoCarrier.LoadState.NO_INVENTORY ? MachineStatus.NO_INVENTORY_TARGET : MachineStatus.TARGET_EMPTY;
        }
        // A full unloader just waits (1.12.2 showed its plain status then)
        if (doMove && putOwn(removal.stack(), false) == CargoCarrier.LoadState.SUCCESS) {
            putOwn(carrier.removeCargo(true).stack(), true);
        }
        return null;
    }

    /** 1.12.2 canExtractItem: the unloaded items (and spent batteries) come out. */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return side != RelativeSide.LEFT;
    }
}
