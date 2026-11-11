package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/**
 * NASA Workbench, Tier 2 Rocket page (1.12.2 ContainerSchematicTier2Rocket, SlotSchematicTier2Rocket and
 * RecipeManagerMars): a nose cone, ten heavy-duty plates, two boosters, four fins and an engine make a Tier 2 Rocket,
 * and each wooden chest in the three top-right slots adds 18 cargo slots. Unlocked with the Tier 2 Rocket Schematic
 * from Moon dungeons.
 */
public class Tier2RocketBenchMenu extends PartBenchMenu {
    public static final int MATRIX_SIZE = 22;

    public Tier2RocketBenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public Tier2RocketBenchMenu(int id, Inventory playerInventory, ContainerLevelAccess access) {
        super(GCMenus.TIER_2_ROCKET_BENCH.get(), id, playerInventory, access, Schematics.ROCKET_T2, MATRIX_SIZE);
        addResultSlot(142, 18 + 69 + OFFSET);
        addPartSlot(1, 48, -8 + OFFSET);
        for (int i = 0; i < 5; i++) {
            addPartSlot(2 + i, 39, -6 + i * 18 + 16 + OFFSET);
        }
        for (int i = 0; i < 5; i++) {
            addPartSlot(7 + i, 57, -6 + i * 18 + 16 + OFFSET);
        }
        addPartSlot(12, 21, 64 + OFFSET);
        addPartSlot(13, 21, 82 + OFFSET);
        addPartSlot(14, 21, 100 + OFFSET);
        addPartSlot(15, 48, 100 + OFFSET);
        addPartSlot(16, 75, 64 + OFFSET);
        addPartSlot(17, 75, 82 + OFFSET);
        addPartSlot(18, 75, 100 + OFFSET);
        for (int i = 0; i < 3; i++) {
            addPartSlot(19 + i, 93 + i * 26, -15 + OFFSET);
        }
        addPlayerInventory(playerInventory, 129 + OFFSET);
    }

    @Override
    protected boolean accepts(int index, ItemStack stack) {
        return isPart(index, stack);
    }

    @Override
    protected ItemStack assemble(Container matrix) {
        return craft(matrix);
    }

    /** What the Tier 2 Rocket page wants in matrix slot {@code index}. */
    public static boolean isPart(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (index >= 19) {
            return stack.is(Tags.Items.CHESTS_WOODEN);
        }
        final Item wanted = switch (index) {
            case 1 -> GCItems.NOSE_CONE.get();
            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 -> GCItems.HEAVY_DUTY_PLATE.get();
            case 12, 16 -> GCItems.TIER_1_BOOSTER.get();
            case 13, 14, 17, 18 -> GCItems.ROCKET_FINS.get();
            case 15 -> GCItems.TIER_1_ROCKET_ENGINE.get();
            default -> null;
        };
        return wanted != null && stack.is(wanted);
    }

    /** 1.12.2 MarsUtil#addRocketBenchT2Recipe recipes: every part, plus 0-3 chests in any of the chest slots. */
    public static ItemStack craft(Container matrix) {
        for (int i = 1; i < 19; i++) {
            if (!isPart(i, matrix.getItem(i))) {
                return ItemStack.EMPTY;
            }
        }
        int chests = 0;
        for (int i = 19; i < MATRIX_SIZE; i++) {
            final ItemStack stack = matrix.getItem(i);
            if (!stack.isEmpty()) {
                if (!isPart(i, stack)) {
                    return ItemStack.EMPTY;
                }
                chests++;
            }
        }
        return GCItems.TIER_2_ROCKET.get().stack(chests, 0);
    }
}
