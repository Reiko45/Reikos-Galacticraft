/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityElectrolyzer,
 * TileEntityMethaneSynthesizer and TileEntityGasLiquefier, on TileBaseElectricBlockWithInventory). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * The tier-2 Mars gas machines (1.12.2 BlockMachineMarsT2): power through the bottom, one operation every 3 ticks (2
 * on tier-2 power) while they can work, a Process / Stop switch, and up to three tanks shown in the GUI.
 */
public abstract class GasMachineBlockEntity extends ProcessingMachineBlockEntity {
    /** 1.12.2 TileEntityOxygenStorageModule.OUTPUT_PER_TICK, the gas output rate of the electrolyzer and synthesizer. */
    public static final int GAS_OUTPUT_PER_TICK = 500;
    /** What an Atmospheric Valve vents from an output tank per tick. */
    public static final int VALVE_VENT = 4;
    /** Fluids a gas machine's tank can show, by index (0 = empty), for the GUI. */
    private static final List<Supplier<Fluid>> SHOWN = List.of(() -> Fluids.EMPTY, () -> Fluids.WATER, () -> GCFluids.OXYGEN.get(),
            () -> GCFluids.HYDROGEN.get(), () -> GCFluids.METHANE.get(), () -> GCFluids.ATMOSPHERIC_GASES.get(), () -> GCFluids.CARBON_DIOXIDE.get(),
            () -> GCFluids.NITROGEN.get(), () -> GCFluids.ARGON.get(), () -> GCFluids.FUEL.get(), () -> GCFluids.LIQUID_OXYGEN.get(),
            () -> GCFluids.LIQUID_NITROGEN.get(), () -> GCFluids.LIQUID_ARGON.get());

    protected boolean disabled;

    protected GasMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, float energyPerTick) {
        super(type, pos, state, slots, 16_000.0F, energyPerTick);
    }

    /** The tanks the GUI shows, in order (at most three). */
    public abstract FluidTank[] tanks();

    protected FluidTank tank(int capacity, Predicate<FluidStack> valid) {
        return new FluidTank(capacity, valid) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    /** 1.12.2: processTimeRequired = max(1, 4 - poweredByTierGC). */
    @Override
    public int processTime() {
        return poweredTier() > 1 ? 2 : 3;
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public void toggleDisabled() {
        this.disabled = !this.disabled;
        setChanged();
    }

    /** OFF with a redstone signal, LOW_ENERGY below one tick's use, otherwise null: the machine's own status. */
    @Nullable
    protected MachineStatus powerStatus() {
        final MachineStatus power = super.status();
        if (power == MachineStatus.OFF) {
            return power;
        }
        return power == MachineStatus.NO_POWER || power == MachineStatus.LOW_POWER ? MachineStatus.LOW_ENERGY : null;
    }

    /** 1.12.2: power only through the bottom. */
    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.BOTTOM ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    /** Push up to {@code rate} from {@code tank} into whatever takes fluids on that face. */
    protected void push(FluidTank tank, RelativeSide side, int rate) {
        final Direction direction = side.toDirection(facing());
        if (this.level == null || direction == null || tank.isEmpty()) {
            return;
        }
        final IFluidHandler target = this.level.getCapability(Capabilities.FluidHandler.BLOCK, this.worldPosition.relative(direction), direction.getOpposite());
        if (target != null) {
            final int accepted = target.fill(tank.getFluid().copyWithAmount(Math.min(rate, tank.getFluidAmount())), IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    public static boolean isValve(ItemStack stack) {
        return stack.is(GCItems.ATMOSPHERIC_VALVE.get());
    }

    /** 1.12.2 checkFluidTankTransfer: an Atmospheric Valve in an output slot vents 4 a tick; a container is filled. */
    protected void emptyIntoSlot(int slot, FluidTank tank) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        if (stack.isEmpty() || tank.isEmpty()) {
            return;
        }
        if (isValve(stack)) {
            tank.drain(VALVE_VENT, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
        final FluidActionResult result = FluidUtil.tryFillContainer(stack, tank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            this.inventory.setStackInSlot(slot, result.getResult());
        }
    }

    /** Whether the block above is open air (not breathable air): where an Atmospheric Valve draws from. */
    protected boolean openAirAbove() {
        if (this.level == null) {
            return false;
        }
        final BlockState above = this.level.getBlockState(this.worldPosition.above());
        return above.isAir() && !above.is(GCBlocks.BREATHABLE_AIR.get());
    }

    /** Whether {@code stack} can take some of {@code fluid}. */
    public static boolean canFill(ItemStack stack, FluidStack fluid) {
        final IFluidHandler handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        return handler != null && !fluid.isEmpty() && handler.fill(fluid.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    public static boolean contains(ItemStack stack, Predicate<FluidStack> fluid) {
        return FluidUtil.getFluidContained(stack).filter(fluid).isPresent();
    }

    /** The GUI index of a fluid (0 for none or one the GUI doesn't know). */
    public static int index(FluidStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        for (int i = 1; i < SHOWN.size(); i++) {
            if (stack.is(SHOWN.get(i).get())) {
                return i;
            }
        }
        return 0;
    }

    public static Fluid shownFluid(int index) {
        return index > 0 && index < SHOWN.size() ? SHOWN.get(index).get() : Fluids.EMPTY;
    }

    /** Synced after the processing values: amount and fluid of each of the three tanks, then the switch. */
    @Override
    protected void addSyncedData(SyncedData data) {
        super.addSyncedData(data);
        for (int i = 0; i < 3; i++) {
            final int tank = i;
            data.addShort(() -> tank < tanks().length ? tanks()[tank].getFluidAmount() : 0);
            data.addShort(() -> tank < tanks().length ? index(tanks()[tank].getFluid()) : 0);
        }
        data.addShort(() -> this.disabled ? 1 : 0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final FluidTank[] tanks = tanks();
        for (int i = 0; i < tanks.length; i++) {
            tag.put("Tank" + i, tanks[i].writeToNBT(registries, new CompoundTag()));
        }
        tag.putBoolean("Disabled", this.disabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final FluidTank[] tanks = tanks();
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(registries, tag.getCompound("Tank" + i));
        }
        this.disabled = tag.getBoolean("Disabled");
    }

    /** A tank view that only allows filling or only draining. */
    protected record DirectionalTank(FluidTank tank, boolean canFill, boolean canDrain) implements IFluidHandler {
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
