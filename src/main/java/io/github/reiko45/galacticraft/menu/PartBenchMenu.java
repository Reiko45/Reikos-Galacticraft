package io.github.reiko45.galacticraft.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A NASA Workbench crafting page (1.12.2 ContainerSchematicTier1Rocket / ContainerBuggyBench with SlotRocketBench,
 * SlotBuggyBench and SlotRocketBenchResult): a matrix of part slots that take one item each, and a result slot that
 * uses up one of every part. Matrix slot 0 is unused, as in 1.12.2, so slot numbers match the 1.12.2 recipes.
 */
public abstract class PartBenchMenu extends SchematicPageMenu {
    /** Pixels the 1.12.2 slots sit below the 1.12.2 GUI top. */
    protected static final int OFFSET = 27;
    private final SimpleContainer matrix;
    private final ResultContainer result = new ResultContainer();

    protected PartBenchMenu(@Nullable MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access, Schematics.Page page, int matrixSize) {
        super(type, id, inventory, access, page);
        this.matrix = new SimpleContainer(matrixSize) {
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(this);
            }
        };
    }

    /** Where a page puts its result and part slots, as its menu and the JEI and EMI pages read it. */
    public interface Layout {
        void result(int x, int y);

        void part(int index, int x, int y);
    }

    /** A layout that adds the slots to this menu. */
    protected Layout slots() {
        return new Layout() {
            @Override
            public void result(int x, int y) {
                addResultSlot(x, y);
            }

            @Override
            public void part(int index, int x, int y) {
                addPartSlot(index, x, y);
            }
        };
    }

    /** Whether matrix slot {@code index} takes this item. */
    protected abstract boolean accepts(int index, ItemStack stack);

    /** The result of the parts now in the matrix, or empty. */
    protected abstract ItemStack assemble(Container matrix);

    protected int matrixSize() {
        return this.matrix.getContainerSize();
    }

    protected void addResultSlot(int x, int y) {
        addSlot(new ResultSlot(x, y));
    }

    protected void addPartSlot(int index, int x, int y) {
        addSlot(new PartSlot(index, x, y));
    }

    protected void addPlayerInventory(Inventory inventory, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, y + 58));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.matrix) {
            this.result.setItem(0, assemble(this.matrix));
        }
        super.slotsChanged(container);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access().execute((level, pos) -> clearContainer(player, this.matrix));
    }

    /** The result slot plus the part slots come first; then the player's inventory. */
    private int benchSlots() {
        return this.matrix.getContainerSize();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final int bench = benchSlots();
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < bench) {
            if (!moveItemStackTo(stack, bench, this.slots.size(), index == 0)) {
                return ItemStack.EMPTY;
            }
            if (index == 0) {
                slot.onQuickCraft(stack, original);
            }
        } else {
            // 1.12.2 mergeOneItem: one part into the first empty slot that takes it.
            boolean moved = false;
            for (int i = 1; i < bench && !moved; i++) {
                final Slot target = this.slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    target.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) {
                final int hotbar = this.slots.size() - 9;
                if (index < hotbar ? !moveItemStackTo(stack, hotbar, this.slots.size(), false)
                        : !moveItemStackTo(stack, bench, hotbar, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private class PartSlot extends Slot {
        private final int index;

        PartSlot(int index, int x, int y) {
            super(PartBenchMenu.this.matrix, index, x, y);
            this.index = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.isEmpty() && accepts(this.index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** 1.12.2 SlotRocketBenchResult: taking the result uses up one of every part. */
    private class ResultSlot extends Slot {
        ResultSlot(int x, int y) {
            super(PartBenchMenu.this.result, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            final SimpleContainer matrix = PartBenchMenu.this.matrix;
            for (int i = 1; i < matrix.getContainerSize(); i++) {
                if (!matrix.getItem(i).isEmpty()) {
                    matrix.removeItem(i, 1);
                }
            }
            super.onTake(player, stack);
        }
    }
}
