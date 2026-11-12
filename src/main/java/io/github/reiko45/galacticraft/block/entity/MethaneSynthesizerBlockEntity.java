/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityMethaneSynthesizer).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.GasMachineMenu;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.Atmosphere;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
 * Methane Synthesizer (1.12.2 TileEntityMethaneSynthesizer): hydrogen and carbon make methane gas, 45 gJ/t. Each
 * operation makes 2 mB of methane from 16 mB of hydrogen and either a fortieth of a piece of Fragmented Carbon or 1 mB
 * of carbon dioxide. On a world with CO2 in its air (Mars), an Atmospheric Valve in the CO2 slot draws 4 mB a tick
 * from the open air above (not inside an oxygen bubble). Hydrogen comes in through the left face; methane leaves
 * through the right, up to 500 mB a tick, or fills a Methane Gas Canister. Tanks: hydrogen 4,000, CO2 and methane
 * 2,000 mB.
 */
public class MethaneSynthesizerBlockEntity extends GasMachineBlockEntity {
    public static final int HYDROGEN_CAPACITY = 4000;
    public static final int CO2_CAPACITY = 2000;
    public static final int METHANE_CAPACITY = 2000;
    /** 1.12.2 coalPartial: one piece of Fragmented Carbon lasts 40 operations. */
    public static final int USES_PER_CARBON = 40;

    private final FluidTank hydrogen = tank(HYDROGEN_CAPACITY, stack -> stack.is(GCFluids.HYDROGEN.get()));
    private final FluidTank carbonDioxide = tank(CO2_CAPACITY, stack -> stack.is(GCFluids.CARBON_DIOXIDE.get()));
    private final FluidTank methane = tank(METHANE_CAPACITY, stack -> stack.is(GCFluids.METHANE.get()));
    private final FluidTank[] tanks = {this.hydrogen, this.carbonDioxide, this.methane};
    private int carbonUses;

    public MethaneSynthesizerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.METHANE_SYNTHESIZER.get(), pos, state, 5, 45.0F);
    }

    @Override
    public FluidTank[] tanks() {
        return this.tanks;
    }

    @Override
    protected float energyPerTick() {
        return 45.0F;
    }

    /** 1.12.2 getAirProducts: whether this world's air holds carbon dioxide. */
    public boolean airHasCarbonDioxide() {
        if (this.level == null) {
            return false;
        }
        final List<Atmosphere.Gas> gases = Atmosphere.of(this.level.dimension());
        return gases != null && gases.contains(Atmosphere.Gas.CO2);
    }

    @Override
    protected void tick() {
        if (!airHasCarbonDioxide() && !this.carbonDioxide.isEmpty()) {
            this.carbonDioxide.setFluid(FluidStack.EMPTY); // 1.12.2: somehow CO2 in a world without any is flushed out
        }
        final ItemStack hydrogenInput = this.inventory.getStackInSlot(1);
        if (!hydrogenInput.isEmpty() && contains(hydrogenInput, stack -> stack.is(GCFluids.HYDROGEN.get()))) {
            final FluidActionResult result = FluidUtil.tryEmptyContainer(hydrogenInput, this.hydrogen, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                this.inventory.setStackInSlot(1, result.getResult());
            }
        }
        if (isValve(this.inventory.getStackInSlot(2)) && airHasCarbonDioxide() && this.carbonDioxide.getSpace() > 0 && openAirAbove()
                && !OxygenUtil.inOxygenBubble(this.level, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D)) {
            this.carbonDioxide.fill(new FluidStack(GCFluids.CARBON_DIOXIDE.get(), VALVE_VENT), IFluidHandler.FluidAction.EXECUTE);
        }
        emptyIntoSlot(4, this.methane);
        super.tick();
        push(this.methane, RelativeSide.RIGHT, GAS_OUTPUT_PER_TICK);
    }

    private boolean hasCarbon() {
        return this.inventory.getStackInSlot(3).is(GCItems.CARBON_FRAGMENTS.get());
    }

    /** 1.12.2 canProcess: hydrogen, some carbon (a fragment in use, a fragment, or CO2) and room for methane. */
    @Override
    protected boolean canProcess() {
        if (this.disabled || this.hydrogen.isEmpty()) {
            return false;
        }
        if (!hasCarbon() && this.carbonUses == 0 && this.carbonDioxide.isEmpty()) {
            return false;
        }
        return this.methane.getSpace() > 0;
    }

    /** 1.12.2 doLiquefaction: carbon from a fragment (40 uses each) or 1 mB of CO2; 2 methane for 16 hydrogen. */
    @Override
    protected void process() {
        if (!hasCarbon() && this.carbonUses == 0) {
            if (this.carbonDioxide.drain(1, IFluidHandler.FluidAction.EXECUTE).getAmount() < 1) {
                return;
            }
        } else {
            if (this.carbonUses == 0) {
                this.inventory.extractItem(3, 1, false);
            }
            this.carbonUses = (this.carbonUses + 1) % USES_PER_CARBON;
        }
        final int made = this.methane.fill(new FluidStack(GCFluids.METHANE.get(), 2), IFluidHandler.FluidAction.EXECUTE);
        this.hydrogen.drain(made * 8, IFluidHandler.FluidAction.EXECUTE);
    }

    /** 1.12.2 GuiMethaneSynthesizer status. */
    @Override
    public MachineStatus status() {
        final MachineStatus power = powerStatus();
        if (power != null) {
            return power;
        }
        if (isWorking() || canProcess()) {
            return MachineStatus.SYNTHESIZING;
        }
        if (this.hydrogen.isEmpty()) {
            return MachineStatus.NO_GAS;
        }
        if (this.disabled) {
            return MachineStatus.READY;
        }
        return this.methane.getSpace() == 0 ? MachineStatus.TANK_FULL : MachineStatus.NEEDS_CARBON;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem;
            case 1 -> contains(stack, fluid -> fluid.is(GCFluids.HYDROGEN.get()));
            case 2 -> isValve(stack);
            case 3 -> stack.is(GCItems.CARBON_FRAGMENTS.get());
            case 4 -> isValve(stack) || canFill(stack, new FluidStack(GCFluids.METHANE.get(), 1));
            default -> false;
        };
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0, 1, 2, 3, 4};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem electric && electric.stored(stack) > 0.0F;
            case 3 -> stack.is(GCItems.CARBON_FRAGMENTS.get());
            case 4 -> canFill(stack, new FluidStack(GCFluids.METHANE.get(), 1));
            default -> false;
        };
    }

    /** 1.12.2: take out empty batteries and full methane canisters. */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem electric && electric.stored(stack) <= 0.0F;
            case 4 -> !isValve(stack) && !canFill(stack, new FluidStack(GCFluids.METHANE.get(), 1));
            default -> false;
        };
    }

    /** Hydrogen in through the left face; methane out through the right. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        final RelativeSide relative = side == null ? null : RelativeSide.of(facing(), side);
        if (relative == RelativeSide.LEFT) {
            return new DirectionalTank(this.hydrogen, true, false);
        }
        if (relative == RelativeSide.RIGHT) {
            return new DirectionalTank(this.methane, false, true);
        }
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new GasMachineMenu(GasMachineMenu.Kind.METHANE_SYNTHESIZER, id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CarbonUses", this.carbonUses);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.carbonUses = tag.getInt("CarbonUses");
    }
}
