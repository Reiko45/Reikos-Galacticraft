package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/**
 * NASA Workbench, Tier 1 Rocket page (1.12.2 ContainerSchematicTier1Rocket, InventoryRocketBench, SlotRocketBench and
 * the recipe in RecipeManagerGC): nose cone, eight heavy plates, four fins and an engine make a rocket, and each wooden
 * chest in the three top-right slots adds 18 cargo slots. Each slot takes one part of its kind.
 */
public class NasaWorkbenchMenu extends PartBenchMenu {
    public static final int MATRIX_SIZE = 18;

    public NasaWorkbenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public NasaWorkbenchMenu(int id, Inventory playerInventory, ContainerLevelAccess access) {
        super(GCMenus.NASA_WORKBENCH.get(), id, playerInventory, access, Schematics.ROCKET_T1, MATRIX_SIZE);
        layout(slots());
        addPlayerInventory(playerInventory, 111 + OFFSET);
    }

    /** Where this page's slots go (shared with the JEI and EMI pages). */
    public static void layout(Layout layout) {
        layout.result(142, 69 + OFFSET);
        layout.part(1, 48, -8 + OFFSET);
        for (int i = 0; i < 4; i++) {
            layout.part(2 + i, 39, -6 + i * 18 + 16 + OFFSET);
        }
        for (int i = 0; i < 4; i++) {
            layout.part(6 + i, 57, -6 + i * 18 + 16 + OFFSET);
        }
        layout.part(10, 21, 64 + OFFSET);
        layout.part(11, 21, 82 + OFFSET);
        layout.part(12, 48, 82 + OFFSET);
        layout.part(13, 75, 64 + OFFSET);
        layout.part(14, 75, 82 + OFFSET);
        for (int i = 0; i < 3; i++) {
            layout.part(15 + i, 93 + i * 26, -15 + OFFSET);
        }
    }

    @Override
    protected boolean accepts(int index, ItemStack stack) {
        return isPart(index, stack);
    }

    @Override
    protected ItemStack assemble(Container matrix) {
        return craft(matrix);
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
}
