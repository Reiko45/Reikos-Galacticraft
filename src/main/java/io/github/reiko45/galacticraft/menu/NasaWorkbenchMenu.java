package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/**
 * NASA Workbench, Tier 1 Rocket page (1.12.2 ContainerSchematicTier1Rocket, InventoryRocketBench, SlotRocketBench and
 * the recipe in RecipeManagerGC): nose cone, eight heavy plates, four fins and an engine make a rocket, and each wooden
 * chest in the three top-right slots adds 18 cargo slots. Each slot takes one part of its kind.
 */
public class NasaWorkbenchMenu extends AbstractContainerMenu {
    public static final int MATRIX_SIZE = 18;
    private static final int OFFSET = 27;

    private final SimpleContainer matrix = new SimpleContainer(MATRIX_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;

    public NasaWorkbenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public NasaWorkbenchMenu(int id, Inventory playerInventory, ContainerLevelAccess access) {
        super(GCMenus.NASA_WORKBENCH.get(), id);
        this.access = access;
        addSlot(new ResultSlot(142, 69 + OFFSET));
        addSlot(new PartSlot(1, 48, -8 + OFFSET));
        for (int i = 0; i < 4; i++) {
            addSlot(new PartSlot(2 + i, 39, -6 + i * 18 + 16 + OFFSET));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new PartSlot(6 + i, 57, -6 + i * 18 + 16 + OFFSET));
        }
        addSlot(new PartSlot(10, 21, 64 + OFFSET));
        addSlot(new PartSlot(11, 21, 82 + OFFSET));
        addSlot(new PartSlot(12, 48, 82 + OFFSET));
        addSlot(new PartSlot(13, 75, 64 + OFFSET));
        addSlot(new PartSlot(14, 75, 82 + OFFSET));
        for (int i = 0; i < 3; i++) {
            addSlot(new PartSlot(15 + i, 93 + i * 26, -15 + OFFSET));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 111 + row * 18 + OFFSET));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 169 + OFFSET));
        }
    }

    /** What the Tier 1 Rocket page wants in matrix slot {@code index}. */
    public static boolean isPart(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (index >= 15) {
            return stack.is(Tags.Items.CHESTS_WOODEN);
        }
        final Item wanted = switch (index) {
            case 1 -> GCItems.NOSE_CONE.get();
            case 2, 3, 4, 5, 6, 7, 8, 9 -> GCItems.HEAVY_PLATING.get();
            case 10, 11, 13, 14 -> GCItems.ROCKET_FINS.get();
            case 12 -> GCItems.TIER_1_ROCKET_ENGINE.get();
            default -> null;
        };
        return wanted != null && stack.is(wanted);
    }

    /** 1.12.2 RecipeUtil#findMatchingSpaceshipRecipe for the Tier 1 recipes. */
    public static ItemStack craft(Container matrix) {
        for (int i = 1; i < 15; i++) {
            if (!isPart(i, matrix.getItem(i))) {
                return ItemStack.EMPTY;
            }
        }
        int chests = 0;
        for (int i = 15; i < MATRIX_SIZE; i++) {
            final ItemStack stack = matrix.getItem(i);
            if (!stack.isEmpty()) {
                if (!isPart(i, stack)) {
                    return ItemStack.EMPTY;
                }
                chests++;
            }
        }
        return Tier1RocketItem.create(chests, 0);
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.matrix) {
            this.result.setItem(0, craft(this.matrix));
        }
        super.slotsChanged(container);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, GCBlocks.NASA_WORKBENCH.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> clearContainer(player, this.matrix));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < MATRIX_SIZE) {
            if (!moveItemStackTo(stack, MATRIX_SIZE, this.slots.size(), index == 0)) {
                return ItemStack.EMPTY;
            }
            if (index == 0) {
                slot.onQuickCraft(stack, original);
            }
        } else {
            // 1.12.2 mergeOneItem: one part into the first empty slot that takes it.
            boolean moved = false;
            for (int i = 1; i < MATRIX_SIZE && !moved; i++) {
                final Slot target = this.slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    target.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) {
                final int hotbar = this.slots.size() - 9;
                if (index < hotbar ? !moveItemStackTo(stack, hotbar, this.slots.size(), false)
                        : !moveItemStackTo(stack, MATRIX_SIZE, hotbar, false)) {
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
            super(NasaWorkbenchMenu.this.matrix, index, x, y);
            this.index = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isPart(this.index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** 1.12.2 SlotRocketBenchResult: taking the rocket uses up one of every part. */
    private class ResultSlot extends Slot {
        ResultSlot(int x, int y) {
            super(NasaWorkbenchMenu.this.result, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            final SimpleContainer matrix = NasaWorkbenchMenu.this.matrix;
            for (int i = 1; i < MATRIX_SIZE; i++) {
                if (!matrix.getItem(i).isEmpty()) {
                    matrix.removeItem(i, 1);
                }
            }
            super.onTake(player, stack);
        }
    }
}
