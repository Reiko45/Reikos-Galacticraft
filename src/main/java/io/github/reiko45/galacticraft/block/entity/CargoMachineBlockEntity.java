/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityCargoLoader and
 * TileEntityCargoUnloader). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.entity.CargoCarrier;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.CargoLoaderMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Cargo Loader and Unloader (1.12.2 TileEntityCargoLoader / TileEntityCargoUnloader), 45 gJ/t while working: beside a
 * joined launch pad (or buggy fueling pad), they move one item every 15 ticks (9 on tier 2 power) into or out of the
 * rocket (or buggy) on it. Slot 0 is the battery, slots 1-14 the items. Power comes in through the left face; items
 * through any other.
 */
public abstract class CargoMachineBlockEntity extends MachineBlockEntity {
    public static final float ENERGY_USE = 45.0F;
    public static final int SLOTS = 15;

    protected boolean disabled;
    /** 1.12.2 TileEntityCargoLoader#locked (abandoned bases): slots 1-14 can't be taken from, and are lost when it breaks. */
    protected boolean locked;
    protected int disableCooldown;
    protected boolean hasEnoughEnergyToRun;
    @Nullable
    protected BlockPos attachedPad;
    protected int ticks;
    protected MachineStatus targetStatus = MachineStatus.IDLE;

    protected CargoMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOTS, 16_000.0F, 2.5F * ENERGY_USE, 0.0F);
    }

    /** True for the loader (items go into the vehicle), false for the unloader. */
    public abstract boolean loads();

    /** Move one item (or just check whether one could move, when {@code doMove} is false); the status to show. */
    protected abstract MachineStatus transfer(CargoCarrier carrier, boolean doMove);

    public boolean isLocked() {
        return this.locked;
    }

    /** Lock the items in (only Cargo Loaders keep the flag, as in 1.12.2). */
    public void lock() {
        this.locked = loads();
        setChanged();
    }

    /** 1.12.2 BlockAdvancedTile#dropEntireInventory: clearLockedInventory first, so a locked loader drops its battery only. */
    @Override
    public void dropContents(net.minecraft.world.level.Level level, BlockPos pos) {
        if (this.locked) {
            for (int i = 1; i < SLOTS; i++) {
                this.inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        super.dropContents(level, pos);
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    /** 1.12.2 setDisabled: 10 tick cooldown between presses. */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 10;
            setChanged();
        }
    }

    @Override
    protected void tick() {
        this.ticks++;
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        dischargeBattery(0);
        // 1.12.2 TileBaseElectricBlock#update: uses its 45 gJ/t unless switched off
        final boolean redstoneOff = this.level.hasNeighborSignal(this.worldPosition);
        this.hasEnoughEnergyToRun = this.energy.getStoredGJ() > ENERGY_USE && !redstoneOff;
        if (this.hasEnoughEnergyToRun && !this.disabled) {
            this.energy.consume(ENERGY_USE);
        } else if (this.level.getGameTime() % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
        if (this.ticks % 100 == 1) {
            this.attachedPad = PadVehicles.findPad(this.level, this.worldPosition);
        }
        final CargoCarrier carrier = PadVehicles.dockedVehicle(this.level, this.attachedPad) instanceof CargoCarrier found ? found : null;
        if (carrier == null) {
            this.targetStatus = loads() ? MachineStatus.NO_TARGET_LOAD : MachineStatus.NO_TARGET_UNLOAD;
            return;
        }
        this.targetStatus = transfer(carrier, false);
        final int interval = this.energy.poweredTier() > 1 ? 9 : 15;
        if (this.targetStatus == null && this.ticks % interval == 0 && !this.disabled && this.hasEnoughEnergyToRun) {
            transfer(carrier, true);
        }
    }

    /** 1.12.2 getStatus: what's wrong with the target, else TileBaseElectricBlock#getGUIstatus. */
    public MachineStatus status() {
        if (this.targetStatus != null) {
            return this.targetStatus;
        }
        if (this.level != null && this.level.hasNeighborSignal(this.worldPosition)) {
            return MachineStatus.OFF;
        }
        if (this.energy.getStoredGJ() <= 0.0F) {
            return MachineStatus.MISSING_POWER;
        }
        if (this.disabled) {
            return MachineStatus.READY;
        }
        if (this.energy.getStoredGJ() < ENERGY_USE) {
            return MachineStatus.LOW_MISSING_POWER;
        }
        return MachineStatus.ACTIVE;
    }

    /** The first item in slots 1-14 (1.12.2 removeCargo on its own inventory). */
    protected CargoCarrier.Removal takeOwn(boolean doRemove) {
        for (int i = 1; i < SLOTS; i++) {
            final ItemStack at = this.inventory.getStackInSlot(i);
            if (!at.isEmpty()) {
                final ItemStack one = at.copyWithCount(1);
                if (doRemove) {
                    this.inventory.extractItem(i, 1, false);
                }
                return new CargoCarrier.Removal(CargoCarrier.LoadState.SUCCESS, one);
            }
        }
        return new CargoCarrier.Removal(CargoCarrier.LoadState.EMPTY, ItemStack.EMPTY);
    }

    /** Into slots 1-14 (1.12.2 addCargo on its own inventory). */
    protected CargoCarrier.LoadState putOwn(ItemStack stack, boolean doAdd) {
        ItemStack rest = stack.copy();
        for (int i = 1; i < SLOTS && !rest.isEmpty(); i++) {
            rest = this.inventory.insertItem(i, rest, !doAdd);
        }
        return rest.isEmpty() ? CargoCarrier.LoadState.SUCCESS : CargoCarrier.LoadState.FULL;
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> status().ordinal());
        data.addShort(() -> this.disabled ? 1 : 0);
        data.addShort(() -> this.disableCooldown);
        data.addShort(() -> this.locked ? 1 : 0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot != 0 || stack.getItem() instanceof ElectricItem;
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.LEFT ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    /** 1.12.2 getSlotsForFace: every slot but through the power face. */
    @Override
    protected int[] automationSlots(RelativeSide side) {
        if (side == RelativeSide.LEFT) {
            return new int[0];
        }
        final int[] slots = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return isItemValid(slot, stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CargoLoaderMenu(loads(), id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Disabled", this.disabled);
        if (this.locked) {
            tag.putBoolean("Locked", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.disabled = tag.getBoolean("Disabled");
        this.locked = tag.getBoolean("Locked");
    }

    @Override
    public float powerDraw() {
        return ENERGY_USE;
    }
}
