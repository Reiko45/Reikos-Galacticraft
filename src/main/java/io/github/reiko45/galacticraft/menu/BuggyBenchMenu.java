package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.item.BuggyItem;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * NASA Workbench, Moon Buggy page (1.12.2 ContainerBuggyBench and the buggy recipes in RecipeManagerGC): eleven heavy
 * plates and a seat for the body, four wheels, and up to three storage boxes on top (18 slots each).
 */
public class BuggyBenchMenu extends PartBenchMenu {
    public static final int MATRIX_SIZE = 20;
    public static final int SEAT = 6;

    public BuggyBenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public BuggyBenchMenu(int id, Inventory playerInventory, ContainerLevelAccess access) {
        super(GCMenus.BUGGY_BENCH.get(), id, playerInventory, access, Schematics.MOON_BUGGY, MATRIX_SIZE);
        addResultSlot(142, 79 + OFFSET);
        // Body: a 3 x 4 grid, numbered down the columns
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                addPartSlot(col * 4 + row + 1, 39 + col * 18, 14 + row * 18 + OFFSET);
            }
        }
        // Wheels at the corners
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addPartSlot(col * 2 + row + 13, 21 + col * 72, 14 + row * 54 + OFFSET);
            }
        }
        // Storage boxes
        for (int i = 0; i < 3; i++) {
            addPartSlot(17 + i, 93 + i * 26, -15 + OFFSET);
        }
        addPlayerInventory(playerInventory, 111 + OFFSET);
    }

    @Override
    protected boolean accepts(int index, ItemStack stack) {
        return isPart(index, stack);
    }

    @Override
    protected ItemStack assemble(Container matrix) {
        return craft(matrix);
    }

    public static boolean isPart(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final Item wanted;
        if (index == SEAT) {
            wanted = GCItems.BUGGY_SEAT.get();
        } else if (index >= 1 && index <= 12) {
            wanted = GCItems.HEAVY_PLATING.get();
        } else if (index >= 13 && index <= 16) {
            wanted = GCItems.BUGGY_WHEEL.get();
        } else if (index >= 17 && index <= 19) {
            wanted = GCItems.BUGGY_STORAGE_BOX.get();
        } else {
            wanted = null;
        }
        return wanted != null && stack.is(wanted);
    }

    /** 1.12.2 RecipeUtil#findMatchingBuggy: the eight buggy recipes differ only in how many storage boxes there are. */
    public static ItemStack craft(Container matrix) {
        for (int i = 1; i <= 16; i++) {
            if (!isPart(i, matrix.getItem(i))) {
                return ItemStack.EMPTY;
            }
        }
        int boxes = 0;
        for (int i = 17; i < MATRIX_SIZE; i++) {
            final ItemStack stack = matrix.getItem(i);
            if (!stack.isEmpty()) {
                if (!isPart(i, stack)) {
                    return ItemStack.EMPTY;
                }
                boxes++;
            }
        }
        return BuggyItem.create(boxes, 0);
    }
}
