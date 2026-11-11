package io.github.reiko45.galacticraft.machine;

import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Automation view of a machine inventory for one side: a subset of slots with insert/extract rules evaluated at
 * call time (1.12.2 ISidedInventory#getSlotsForFace / canInsertItem / canExtractItem).
 */
public final class SidedItemHandler implements IItemHandler {
    private final IItemHandlerModifiable inventory;
    private final int[] slots;
    private final BiPredicate<Integer, ItemStack> canInsert;
    private final IntPredicate canExtract;

    public SidedItemHandler(IItemHandlerModifiable inventory, int[] slots, BiPredicate<Integer, ItemStack> canInsert, IntPredicate canExtract) {
        this.inventory = inventory;
        this.slots = slots;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int getSlots() {
        return this.slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.inventory.getStackInSlot(this.slots[slot]);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.canInsert.test(this.slots[slot], stack) ? this.inventory.insertItem(this.slots[slot], stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.canExtract.test(this.slots[slot]) ? this.inventory.extractItem(this.slots[slot], amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.inventory.getSlotLimit(this.slots[slot]);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.canInsert.test(this.slots[slot], stack) && this.inventory.isItemValid(this.slots[slot], stack);
    }
}
