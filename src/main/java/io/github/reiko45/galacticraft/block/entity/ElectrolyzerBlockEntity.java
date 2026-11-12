/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityElectrolyzer). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.GasMachineMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Water Electrolyzer (1.12.2 TileEntityElectrolyzer): splits water into oxygen and hydrogen, 120 gJ/t. Each operation
 * takes 1 mB of water and makes 2 mB of oxygen and 4 mB of hydrogen (as far as each tank has room). Water comes in
 * through the left face (or a water container in its slot); oxygen leaves through the back and hydrogen through the
 * right, up to 500 mB a tick each. An Atmospheric Valve in an output slot vents that gas. Tanks: 4,000 mB each.
 */
public class ElectrolyzerBlockEntity extends GasMachineBlockEntity {
    public static final int TANK_CAPACITY = 4000;

    private final FluidTank water = tank(TANK_CAPACITY, stack -> stack.is(FluidTags.WATER));
    private final FluidTank oxygen = tank(TANK_CAPACITY, stack -> stack.is(GCFluids.OXYGEN.get()));
    private final FluidTank hydrogen = tank(TANK_CAPACITY, stack -> stack.is(GCFluids.HYDROGEN.get()));
    private final FluidTank[] tanks = {this.water, this.oxygen, this.hydrogen};

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.ELECTROLYZER.get(), pos, state, 4, 120.0F);
    }

    @Override
    public FluidTank[] tanks() {
        return this.tanks;
    }

    @Override
    protected float energyPerTick() {
        return 120.0F;
    }

    @Override
    protected void tick() {
        final ItemStack container = this.inventory.getStackInSlot(1);
        if (!container.isEmpty() && contains(container, stack -> stack.is(FluidTags.WATER))) {
            final FluidActionResult result = FluidUtil.tryEmptyContainer(container, this.water, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                this.inventory.setStackInSlot(1, result.getResult());
            }
        }
        // 1.12.2: only an Atmospheric Valve does anything in the output slots
        if (isValve(this.inventory.getStackInSlot(2))) {
            this.oxygen.drain(VALVE_VENT, IFluidHandler.FluidAction.EXECUTE);
        }
        if (isValve(this.inventory.getStackInSlot(3))) {
            this.hydrogen.drain(VALVE_VENT, IFluidHandler.FluidAction.EXECUTE);
        }
        super.tick();
        push(this.oxygen, RelativeSide.BACK, GAS_OUTPUT_PER_TICK);
        push(this.hydrogen, RelativeSide.RIGHT, GAS_OUTPUT_PER_TICK);
    }

    /** 1.12.2 canProcess: water in, and room in either gas tank. */
    @Override
    protected boolean canProcess() {
        return !this.disabled && !this.water.isEmpty()
                && (this.oxygen.getSpace() > 0 || this.hydrogen.getSpace() > 0);
    }

    /** 1.12.2 doElectrolysis: 1 water makes 2 oxygen and 4 hydrogen. */
    @Override
    protected void process() {
        this.oxygen.fill(new FluidStack(GCFluids.OXYGEN.get(), 2), IFluidHandler.FluidAction.EXECUTE);
        this.hydrogen.fill(new FluidStack(GCFluids.HYDROGEN.get(), 4), IFluidHandler.FluidAction.EXECUTE);
        this.water.drain(1, IFluidHandler.FluidAction.EXECUTE);
    }

    /** 1.12.2 GuiWaterElectrolyzer status. */
    @Override
    public MachineStatus status() {
        final MachineStatus power = powerStatus();
        if (power != null) {
            return power;
        }
        if (this.water.isEmpty()) {
            return MachineStatus.NO_WATER;
        }
        if (this.disabled) {
            return MachineStatus.READY;
        }
        if (canProcess()) {
            return MachineStatus.RUNNING;
        }
        return this.oxygen.getSpace() == 0 && this.hydrogen.getSpace() == 0 ? MachineStatus.TANKS_FULL : MachineStatus.UNKNOWN;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem;
            case 1 -> contains(stack, fluid -> fluid.is(FluidTags.WATER)) || canFill(stack, new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1));
            case 2, 3 -> isValve(stack);
            default -> false;
        };
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0, 1};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return slot == 0 ? stack.getItem() instanceof ElectricItem electric && electric.stored(stack) > 0.0F
                : slot == 1 && contains(stack, fluid -> fluid.is(FluidTags.WATER));
    }

    /** 1.12.2: take out empty batteries and emptied containers. */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        return slot == 0 ? stack.getItem() instanceof ElectricItem electric && electric.stored(stack) <= 0.0F
                : slot == 1 && !contains(stack, fluid -> fluid.is(FluidTags.WATER));
    }

    /** Water in through the left face; oxygen out through the back, hydrogen through the right. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        final RelativeSide relative = side == null ? null : RelativeSide.of(facing(), side);
        if (relative == RelativeSide.LEFT) {
            return new DirectionalTank(this.water, true, false);
        }
        if (relative == RelativeSide.BACK) {
            return new DirectionalTank(this.oxygen, false, true);
        }
        if (relative == RelativeSide.RIGHT) {
            return new DirectionalTank(this.hydrogen, false, true);
        }
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new GasMachineMenu(GasMachineMenu.Kind.ELECTROLYZER, id, playerInventory, this);
    }
}
