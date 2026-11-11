package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Oxygen Compressor (1.12.2 TileEntityOxygenCompressor) fills the oxygen tank in slot 0 by 2 mB/t from its 1,200 mB
 * store, using 15 gJ/t while it does. Oxygen comes in through the face opposite the power port (32 mB/t).
 * Slots: 0 tank, 1 battery, 2 oxygen supply.
 */
public class OxygenCompressorBlockEntity extends OxygenMachineBlockEntity {
    public static final int TANK_TRANSFER_SPEED = 2;
    private boolean usingEnergy;

    public OxygenCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.OXYGEN_COMPRESSOR.get(), pos, state, 3, 1200, 16, 16_000.0F, 15.0F);
    }

    @Override
    protected int batterySlot() {
        return 1;
    }

    @Override
    protected int oxygenSupplySlot() {
        return 2;
    }

    @Override
    protected boolean shouldUseEnergy() {
        return this.usingEnergy;
    }

    @Override
    protected void work() {
        this.usingEnergy = false;
        if (this.oxygen.getFluidAmount() > 0 && this.hasEnoughEnergyToRun) {
            final ItemStack tank = this.inventory.getStackInSlot(0);
            if (tank.getItem() instanceof OxygenTankItem item && OxygenTankItem.oxygen(tank) < item.capacity()) {
                final int moved = Math.min(TANK_TRANSFER_SPEED, Math.min(this.oxygen.getFluidAmount(), item.capacity() - OxygenTankItem.oxygen(tank)));
                OxygenTankItem.setOxygen(tank, OxygenTankItem.oxygen(tank) + moved);
                this.oxygen.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                this.inventory.setStackInSlot(0, tank);
                this.usingEnergy = true;
            }
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 ? stack.getItem() instanceof OxygenTankItem : super.isItemValid(slot, stack);
    }

    /** Full tanks come out; tanks that are not full go in (1.12.2 canInsertItem/canExtractItem). */
    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        if (slot == 0) {
            return stack.getItem() instanceof OxygenTankItem tank && OxygenTankItem.oxygen(stack) < tank.capacity();
        }
        return super.canAutomationInsert(slot, stack, side);
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        if (slot == 0) {
            return stack.getItem() instanceof OxygenTankItem tank && OxygenTankItem.oxygen(stack) >= tank.capacity();
        }
        return super.canAutomationExtract(slot, side);
    }

    @Override
    public MachineStatus status() {
        final ItemStack tank = this.inventory.getStackInSlot(0);
        if (!(tank.getItem() instanceof OxygenTankItem item)) {
            return MachineStatus.MISSING_TANK;
        }
        if (OxygenTankItem.oxygen(tank) >= item.capacity()) {
            return MachineStatus.FULL_TANK;
        }
        if (this.oxygen.getFluidAmount() < 1) {
            return MachineStatus.MISSING_OXYGEN;
        }
        return electricStatus();
    }

    @Override
    public OxygenMachineMenu.Kind menuKind() {
        return OxygenMachineMenu.Kind.COMPRESSOR;
    }
}
