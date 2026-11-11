package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.gear.InfiniteOxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Oxygen Decompressor (1.12.2 TileEntityOxygenDecompressor) empties the oxygen tank in slot 0 by 1 mB/t into its
 * 1,200 mB store (10 gJ/t) and pushes it out of the face opposite the power port, up to 100 mB/t. Slots: 0 tank,
 * 1 battery.
 */
public class OxygenDecompressorBlockEntity extends OxygenMachineBlockEntity {
    private boolean usingEnergy;

    public OxygenDecompressorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.OXYGEN_DECOMPRESSOR.get(), pos, state, 2, 1200, 0, 16_000.0F, 10.0F);
    }

    @Override
    protected int batterySlot() {
        return 1;
    }

    @Override
    protected Set<RelativeSide> oxygenInputs() {
        return Set.of();
    }

    @Override
    protected Set<RelativeSide> oxygenOutputs() {
        return Set.of(RelativeSide.RIGHT);
    }

    @Override
    protected int oxygenOutputRate() {
        return 100;
    }

    @Override
    protected boolean acceptsOxygen() {
        return false;
    }

    @Override
    protected boolean shouldUseEnergy() {
        return this.usingEnergy;
    }

    @Override
    protected void work() {
        this.usingEnergy = false;
        final ItemStack tank = this.inventory.getStackInSlot(0);
        if (!tank.isEmpty() && this.hasEnoughEnergyToRun && this.oxygen.getFluidAmount() < this.oxygen.getCapacity()) {
            if (tank.getItem() instanceof OxygenTankItem && OxygenTankItem.oxygen(tank) > 0) {
                OxygenTankItem.setOxygen(tank, OxygenTankItem.oxygen(tank) - 1);
                this.inventory.setStackInSlot(0, tank);
                setOxygenStored(this.oxygen.getFluidAmount() + 1);
                this.usingEnergy = true;
            } else if (tank.getItem() instanceof InfiniteOxygenTankItem) {
                setOxygenStored(this.oxygen.getFluidAmount() + 1);
                this.usingEnergy = true;
            }
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 ? stack.getItem() instanceof OxygenTankItem || stack.getItem() instanceof InfiniteOxygenTankItem
                : super.isItemValid(slot, stack);
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        if (slot == 0) {
            return stack.getItem() instanceof OxygenTankItem && OxygenTankItem.oxygen(stack) > 0;
        }
        return super.canAutomationInsert(slot, stack, side);
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        if (slot == 0) {
            return stack.getItem() instanceof OxygenTankItem && OxygenTankItem.oxygen(stack) <= 0;
        }
        return super.canAutomationExtract(slot, side);
    }

    @Override
    public MachineStatus status() {
        final ItemStack tank = this.inventory.getStackInSlot(0);
        if (!(tank.getItem() instanceof OxygenTankItem) && !(tank.getItem() instanceof InfiniteOxygenTankItem)) {
            return MachineStatus.MISSING_TANK;
        }
        if (tank.getItem() instanceof OxygenTankItem && OxygenTankItem.oxygen(tank) <= 0) {
            return MachineStatus.TANK_EMPTY;
        }
        return electricStatus();
    }

    @Override
    public OxygenMachineMenu.Kind menuKind() {
        return OxygenMachineMenu.Kind.DECOMPRESSOR;
    }
}
