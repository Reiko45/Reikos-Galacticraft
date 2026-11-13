package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Base of the oxygen machines (1.12.2 TileEntityOxygen on TileBaseElectricBlock).
 *
 * <p>Energy (1.12.2 TileBaseElectricBlock#update): a battery tops the buffer up; the machine "has enough energy to run"
 * while it holds more than its per-tick use (and no redstone signal, unless it ignores redstone); while running it uses
 * that much per tick, otherwise it loses 5 gJ every 10 ticks. It receives power at 2.5x its use through the face left of
 * the front.
 *
 * <p>Oxygen (1.12.2 TileEntityOxygen): an internal oxygen tank in mB of oxygen gas. Oxygen comes in through the face
 * opposite the power port (at most 2x the machine's oxygen use per tick) and leaves through the output face, pushed into
 * whatever pipe or machine is there. An oxygen supply item (an Infinite Oxygen Supply, or any oxygen container) in the
 * oxygen slot is drained into the tank.
 */
public abstract class OxygenMachineBlockEntity extends MachineBlockEntity {
    protected final FluidTank oxygen;
    /** Oxygen used per tick while {@link #shouldUseOxygen()}; also sets the input rate. */
    protected int oxygenPerTick;
    private float energyUse;
    protected boolean noRedstoneControl;
    protected boolean hasEnoughEnergyToRun;
    protected boolean disabled;
    protected int disableCooldown;
    private int received;
    protected int ticks;

    protected OxygenMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, int oxygenCapacity,
                                       int oxygenPerTick, float energyCapacity, float energyUse) {
        super(type, pos, state, slots, energyCapacity, 2.5F * energyUse, 0.0F);
        this.oxygen = new FluidTank(oxygenCapacity, stack -> stack.is(GCFluids.OXYGEN.get())) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.oxygenPerTick = oxygenPerTick;
        this.energyUse = energyUse;
    }

    // ------------------------------------------------------------------ hooks

    /** 1.12.2 shouldUseEnergy: use {@link #energyUse()} this tick. */
    protected abstract boolean shouldUseEnergy();

    /** 1.12.2 shouldUseOxygen: use {@link #oxygenPerTick} this tick. */
    protected boolean shouldUseOxygen() {
        return false;
    }

    /** The machine's own work, after the energy and oxygen bookkeeping. */
    protected abstract void work();

    protected int batterySlot() {
        return -1;
    }

    protected int oxygenSupplySlot() {
        return -1;
    }

    /** Faces that take oxygen in (default: the one opposite the power port). */
    protected Set<RelativeSide> oxygenInputs() {
        return Set.of(RelativeSide.RIGHT);
    }

    /** Faces that give oxygen out (default: none). */
    protected Set<RelativeSide> oxygenOutputs() {
        return Set.of();
    }

    /** Oxygen pushed out per tick through each output face. */
    protected int oxygenOutputRate() {
        return 0;
    }

    /** 1.12.2 shouldPullOxygen. */
    protected boolean acceptsOxygen() {
        return this.oxygen.getFluidAmount() < this.oxygen.getCapacity();
    }

    public abstract MachineStatus status();

    // ------------------------------------------------------------------ state

    public FluidTank oxygenTank() {
        return this.oxygen;
    }

    public int oxygenStored() {
        return this.oxygen.getFluidAmount();
    }

    public void setOxygenStored(int amount) {
        this.oxygen.setFluid(amount > 0 ? new FluidStack(GCFluids.OXYGEN.get(), Math.min(amount, this.oxygen.getCapacity())) : FluidStack.EMPTY);
    }

    public float energyUse() {
        return this.energyUse;
    }

    protected void setEnergyUse(float use, float receive) {
        this.energyUse = use;
        this.energy.setMaxReceive(receive);
    }

    public boolean hasEnoughEnergyToRun() {
        return this.hasEnoughEnergyToRun;
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

    protected boolean redstoneOff() {
        return !this.noRedstoneControl && this.level != null && this.level.hasNeighborSignal(this.worldPosition);
    }

    /** 1.12.2 TileBaseElectricBlock#getGUIstatus. */
    protected MachineStatus electricStatus() {
        if (redstoneOff()) {
            return MachineStatus.OFF;
        }
        if (this.energy.getStoredGJ() <= 0.0F) {
            return MachineStatus.MISSING_POWER;
        }
        if (this.disabled) {
            return MachineStatus.READY;
        }
        if (this.energy.getStoredGJ() < this.energyUse) {
            return MachineStatus.LOW_MISSING_POWER;
        }
        return MachineStatus.ACTIVE;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void tick() {
        this.ticks++;
        this.received = 0;
        drainSupplyItem();
        if (this.energy.getCapacityGJ() > 0.0F) {
            if (batterySlot() >= 0 && (shouldUseEnergy() || this.energy.getStoredGJ() < this.energy.getCapacityGJ())) {
                dischargeBattery(batterySlot());
            }
            if (this.energy.getStoredGJ() > this.energyUse && !redstoneOff()) {
                this.hasEnoughEnergyToRun = true;
                if (shouldUseEnergy()) {
                    this.energy.consume(this.energyUse);
                } else {
                    slowDischarge();
                }
            } else {
                this.hasEnoughEnergyToRun = false;
                slowDischarge();
            }
        }
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        if (shouldUseOxygen() && !this.oxygen.isEmpty()) {
            this.oxygen.drain(this.oxygenPerTick, IFluidHandler.FluidAction.EXECUTE);
        }
        work();
        pushOxygen();
    }

    private void slowDischarge() {
        if (this.level != null && this.level.getGameTime() % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
    }

    /** 1.12.2 IItemOxygenSupply#discharge: up to 2.5x the oxygen use per tick from the oxygen slot. */
    private void drainSupplyItem() {
        final int slot = oxygenSupplySlot();
        if (slot < 0) {
            return;
        }
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        final IFluidHandlerItem handler = stack.isEmpty() ? null : stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return;
        }
        final int wanted = Math.min((int) Math.floor(this.oxygenPerTick * 2.5F), this.oxygen.getSpace());
        if (wanted > 0) {
            final FluidStack drained = handler.drain(new FluidStack(GCFluids.OXYGEN.get(), wanted), IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                this.oxygen.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                this.inventory.setStackInSlot(slot, handler.getContainer());
            }
        }
    }

    /** 1.12.2 produceOxygen: push into the pipe network or machine on each output face. */
    private void pushOxygen() {
        if (this.level == null || this.oxygen.isEmpty() || oxygenOutputRate() <= 0) {
            return;
        }
        for (RelativeSide relative : oxygenOutputs()) {
            final Direction side = relative.toDirection(facing());
            if (side == null) {
                continue;
            }
            final IFluidHandler target = this.level.getCapability(Capabilities.FluidHandler.BLOCK, this.worldPosition.relative(side), side.getOpposite());
            if (target != null) {
                final int offer = Math.min(oxygenOutputRate(), this.oxygen.getFluidAmount());
                final int accepted = target.fill(new FluidStack(GCFluids.OXYGEN.get(), offer), IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    this.oxygen.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    // ------------------------------------------------------------------ capabilities

    /** Oxygen in through the input faces (2x oxygen use per tick), out through the output faces. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        final RelativeSide relative = side == null ? null : RelativeSide.of(facing(), side);
        final boolean in = relative != null && oxygenInputs().contains(relative);
        final boolean out = relative != null && oxygenOutputs().contains(relative);
        if (!in && !out) {
            return null;
        }
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return OxygenMachineBlockEntity.this.oxygen.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return OxygenMachineBlockEntity.this.oxygen.getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return in && stack.is(GCFluids.OXYGEN.get());
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                final OxygenMachineBlockEntity machine = OxygenMachineBlockEntity.this;
                if (!in || !resource.is(GCFluids.OXYGEN.get()) || !machine.acceptsOxygen()) {
                    return 0;
                }
                final int budget = Math.max(1, machine.oxygenPerTick * 2) - machine.received;
                final int accepted = machine.oxygen.fill(resource.copyWithAmount(Math.min(resource.getAmount(), Math.max(0, budget))), action);
                if (action.execute()) {
                    machine.received += accepted;
                }
                return accepted;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return out ? OxygenMachineBlockEntity.this.oxygen.drain(resource, action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return out ? OxygenMachineBlockEntity.this.oxygen.drain(maxDrain, action) : FluidStack.EMPTY;
            }
        };
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return this.energy.getCapacityGJ() > 0.0F && side == RelativeSide.LEFT ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == batterySlot()) {
            return stack.getItem() instanceof ElectricItem;
        }
        if (slot == oxygenSupplySlot()) {
            final IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
            return handler != null && !handler.drain(new FluidStack(GCFluids.OXYGEN.get(), 1), IFluidHandler.FluidAction.SIMULATE).isEmpty();
        }
        return false;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        final int[] slots = new int[this.inventory.getSlots()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return isItemValid(slot, stack);
    }

    /** Take out flat batteries and empty oxygen containers (1.12.2 canExtractItem). */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        if (slot == batterySlot()) {
            return stack.getItem() instanceof ElectricItem electric && electric.stored(stack) <= 0.0F;
        }
        if (slot == oxygenSupplySlot()) {
            return !isItemValid(slot, stack);
        }
        return false;
    }

    // ------------------------------------------------------------------ GUI

    /** Synced: oxygen (int), oxygen capacity (int), status, disabled, two machine-specific values. */
    @Override
    protected void addSyncedData(SyncedData data) {
        data.addInt(() -> this.oxygen.getFluidAmount());
        data.addInt(() -> this.oxygen.getCapacity());
        data.addShort(() -> status().ordinal());
        data.addShort(() -> this.disabled ? 1 : 0);
        data.addShort(this::guiValue1);
        data.addShort(this::guiValue2);
    }

    protected int guiValue1() {
        return 0;
    }

    protected int guiValue2() {
        return 0;
    }

    public abstract OxygenMachineMenu.Kind menuKind();

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new OxygenMachineMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Oxygen", this.oxygen.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("Disabled", this.disabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.oxygen.readFromNBT(registries, tag.getCompound("Oxygen"));
        this.disabled = tag.getBoolean("Disabled");
    }

    @Override
    public float powerDraw() {
        return energyUse();
    }
}
