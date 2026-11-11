package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.SyncedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An electric machine that turns inputs into outputs over time (1.12.2 TileBaseElectricBlock subclasses: Electric
 * Furnace, Electric Compressor, Circuit Fabricator). Rules ported from TileBaseElectricBlock#update:
 * <ul>
 *     <li>a battery in {@link #batterySlot()} tops up the buffer whenever it is not full;</li>
 *     <li>a redstone signal switches the machine off;</li>
 *     <li>while working it uses exactly {@link #energyPerTick()} gJ per tick, and only runs while it holds at least that;</li>
 *     <li>when not using energy it slowly loses 5 gJ every 10 ticks;</li>
 *     <li>by default progress resets when power or a valid recipe is missing.</li>
 * </ul>
 * Energy enters through the face on the left of the front, receive rate 2.5x usage (1.12.2 setMaxExtract).
 */
public abstract class ProcessingMachineBlockEntity extends MachineBlockEntity {
    protected int progress;

    protected ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots,
                                           float capacity, float energyPerTick) {
        super(type, pos, state, slots, capacity, 2.5F * energyPerTick, 0.0F);
    }

    /** gJ used per tick of work. */
    protected abstract float energyPerTick();

    /** Ticks for one operation; may depend on {@link #poweredTier()}. */
    public abstract int processTime();

    /** Inventory slot holding a battery, or -1. */
    protected abstract int batterySlot();

    /** Whether the current inputs can produce something and the outputs have room. */
    protected abstract boolean canProcess();

    /** Consume inputs and produce outputs. */
    protected abstract void process();

    @Override
    protected void tick() {
        if (batterySlot() >= 0) {
            dischargeBattery(batterySlot());
        }
        final boolean switchedOff = this.level != null && this.level.hasNeighborSignal(this.worldPosition);
        final boolean powered = !switchedOff && this.energy.getStoredGJ() >= energyPerTick();
        boolean used = false;
        if (canProcess()) {
            if (powered) {
                this.energy.consume(energyPerTick());
                used = true;
                this.progress++;
                if (this.progress >= processTime()) {
                    this.progress = 0;
                    process();
                }
                setChanged();
            } else {
                onUnpowered();
            }
        } else if (this.progress != 0) {
            this.progress = 0;
            setChanged();
        }
        if (!used && this.level != null && this.level.getGameTime() % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
    }

    /** Valid recipe but not enough power this tick. 1.12.2 default: start over. */
    protected void onUnpowered() {
        if (this.progress != 0) {
            this.progress = 0;
            setChanged();
        }
    }

    public boolean isWorking() {
        return this.progress > 0;
    }

    /** Tier of the power last received: 2 with tier-2 sources over heavy wire, 1 otherwise (and from batteries). */
    public int poweredTier() {
        return this.energy.poweredTier();
    }

    /** 1.12.2 getGUIstatus. */
    public MachineStatus status() {
        if (this.level != null && this.level.hasNeighborSignal(this.worldPosition)) {
            return MachineStatus.OFF;
        }
        if (this.energy.getStoredGJ() <= 0.0F) {
            return MachineStatus.NO_POWER;
        }
        if (this.energy.getStoredGJ() < energyPerTick()) {
            return MachineStatus.LOW_POWER;
        }
        return isWorking() ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }

    /** Synced: progress, processTime, status (indices EXTRA.. in the menu). */
    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> this.progress);
        data.addShort(this::processTime);
        data.addShort(() -> status().ordinal());
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.LEFT ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    /** Merge {@code result} into {@code slot}, or report whether it would fit. */
    protected boolean fits(int slot, ItemStack result) {
        final ItemStack current = this.inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= Math.min(current.getMaxStackSize(), this.inventory.getSlotLimit(slot));
    }

    protected void output(int slot, ItemStack result) {
        final ItemStack current = this.inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            this.inventory.setStackInSlot(slot, result.copy());
        } else {
            current.grow(result.getCount());
            this.inventory.setStackInSlot(slot, current);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("Progress");
    }
}
