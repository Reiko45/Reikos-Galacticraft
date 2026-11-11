package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.RefineryMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Refinery (1.12.2 TileEntityRefinery): turns oil into fuel, 1 mB every 2 ticks (every tick on tier-2 power) at
 * 60 gJ/t. Oil and fuel tanks hold 24,000 mB each. Oil enters through the left face, fuel leaves through the right,
 * power through the top. Slots: 0 battery, 1 oil container (emptied into the oil tank), 2 fuel container (filled).
 */
public class RefineryBlockEntity extends ProcessingMachineBlockEntity {
    public static final int TANK_CAPACITY = 24_000;

    private final FluidTank oil = new FluidTank(TANK_CAPACITY, stack -> stack.is(GCFluidTags.OIL)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank fuel = new FluidTank(TANK_CAPACITY, stack -> stack.is(GCFluids.FUEL.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private boolean disabled;

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.REFINERY.get(), pos, state, 3, 16_000.0F, 60.0F);
    }

    public FluidTank oilTank() {
        return this.oil;
    }

    public FluidTank fuelTank() {
        return this.fuel;
    }

    @Override
    protected float energyPerTick() {
        return 60.0F;
    }

    @Override
    public int processTime() {
        return poweredTier() > 1 ? 1 : 2;
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    @Override
    protected void tick() {
        emptyOilContainer();
        fillFuelContainer();
        super.tick();
    }

    private void emptyOilContainer() {
        final ItemStack stack = this.inventory.getStackInSlot(1);
        if (!stack.isEmpty() && isOilContainer(stack)) {
            final FluidActionResult result = FluidUtil.tryEmptyContainer(stack, this.oil, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                this.inventory.setStackInSlot(1, result.getResult());
            }
        }
    }

    private void fillFuelContainer() {
        final ItemStack stack = this.inventory.getStackInSlot(2);
        if (!stack.isEmpty() && !this.fuel.isEmpty()) {
            final FluidActionResult result = FluidUtil.tryFillContainer(stack, this.fuel, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                this.inventory.setStackInSlot(2, result.getResult());
            }
        }
    }

    public static boolean isOilContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).filter(fluid -> fluid.is(GCFluidTags.OIL)).isPresent();
    }

    /** Containers that can take fuel: empty canisters, partly filled fuel canisters, buckets... */
    public static boolean isFuelContainer(ItemStack stack) {
        final IFluidHandler handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        return handler != null && handler.fill(new FluidStack(GCFluids.FUEL.get(), 1), IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    @Override
    protected boolean canProcess() {
        return !this.disabled && !this.oil.isEmpty() && this.fuel.getFluidAmount() < this.fuel.getCapacity();
    }

    @Override
    protected void process() {
        final int amount = Math.min(1, Math.min(this.oil.getFluidAmount(), this.fuel.getCapacity() - this.fuel.getFluidAmount()));
        if (amount > 0) {
            this.oil.drain(amount, IFluidHandler.FluidAction.EXECUTE);
            this.fuel.fill(new FluidStack(GCFluids.FUEL.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public MachineStatus status() {
        final MachineStatus power = super.status();
        if (power == MachineStatus.OFF || power == MachineStatus.NO_POWER || power == MachineStatus.LOW_POWER) {
            return power;
        }
        if (this.oil.isEmpty()) {
            return MachineStatus.NO_OIL;
        }
        return isWorking() ? MachineStatus.REFINING : MachineStatus.IDLE;
    }

    public void toggleDisabled() {
        this.disabled = !this.disabled;
        setChanged();
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        super.addSyncedData(data);
        // Lambdas, not method references: this runs in the super constructor, before the tanks exist.
        data.addShort(() -> this.oil.getFluidAmount());
        data.addShort(() -> this.fuel.getFluidAmount());
        data.addShort(() -> this.disabled ? 1 : 0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem;
            case 1 -> isOilContainer(stack);
            case 2 -> isFuelContainer(stack);
            default -> false;
        };
    }

    /** 1.12.2: power only through the top. */
    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.TOP ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0, 1, 2};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return isItemValid(slot, stack);
    }

    /** 1.12.2: take out empty batteries, emptied oil containers and filled fuel containers. */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem electric && electric.stored(stack) <= 0.0F;
            case 1 -> !isOilContainer(stack);
            case 2 -> !isFuelContainer(stack);
            default -> false;
        };
    }

    /** Fluid capability: oil in through the left face, fuel out through the right face. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        final RelativeSide relative = side == null ? null : RelativeSide.of(facing(), side);
        if (relative == RelativeSide.LEFT) {
            return new DirectionalTank(this.oil, true, false);
        }
        if (relative == RelativeSide.RIGHT) {
            return new DirectionalTank(this.fuel, false, true);
        }
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RefineryMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Oil", this.oil.writeToNBT(registries, new CompoundTag()));
        tag.put("Fuel", this.fuel.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("Disabled", this.disabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.oil.readFromNBT(registries, tag.getCompound("Oil"));
        this.fuel.readFromNBT(registries, tag.getCompound("Fuel"));
        this.disabled = tag.getBoolean("Disabled");
    }

    /** A tank view that only allows filling or only draining. */
    private record DirectionalTank(FluidTank tank, boolean canFill, boolean canDrain) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return this.tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return this.tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return this.canFill && this.tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return this.canFill ? this.tank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return this.canDrain ? this.tank.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return this.canDrain ? this.tank.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }
}
