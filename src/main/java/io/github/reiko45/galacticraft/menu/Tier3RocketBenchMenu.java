/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.inventory.ContainerSchematicTier3Rocket,
 * SlotSchematicTier3Rocket and recipe.RecipeManagerAsteroids). Copyright (c) 2023 Team Galacticraft. Licensed under the
 * MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
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
 * NASA Workbench, Tier 3 Rocket page (1.12.2 ContainerSchematicTier3Rocket): laid out like the Tier 2 page; a heavy nose
 * cone, ten Tier 3 heavy-duty plates, two boosters, four heavy fins and a heavy rocket engine make a Tier 3 Rocket, and
 * each wooden chest adds 18 cargo slots. Unlocked with the Tier 3 Rocket Schematic the Creeper Boss drops.
 */
public class Tier3RocketBenchMenu extends PartBenchMenu {
    public static final int MATRIX_SIZE = 22;

    public Tier3RocketBenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public Tier3RocketBenchMenu(int id, Inventory playerInventory, ContainerLevelAccess access) {
        super(GCMenus.TIER_3_ROCKET_BENCH.get(), id, playerInventory, access, Schematics.ROCKET_T3, MATRIX_SIZE);
        layout(slots());
        addPlayerInventory(playerInventory, 129 + OFFSET);
    }

    /** Where this page's slots go (shared with the JEI and EMI pages). */
    public static void layout(Layout layout) {
        layout.result(142, 18 + 69 + OFFSET);
        layout.part(1, 48, -8 + OFFSET);
        for (int i = 0; i < 5; i++) {
            layout.part(2 + i, 39, -6 + i * 18 + 16 + OFFSET);
        }
        for (int i = 0; i < 5; i++) {
            layout.part(7 + i, 57, -6 + i * 18 + 16 + OFFSET);
        }
        layout.part(12, 21, 64 + OFFSET);
        layout.part(13, 21, 82 + OFFSET);
        layout.part(14, 21, 100 + OFFSET);
        layout.part(15, 48, 100 + OFFSET);
        layout.part(16, 75, 64 + OFFSET);
        layout.part(17, 75, 82 + OFFSET);
        layout.part(18, 75, 100 + OFFSET);
        for (int i = 0; i < 3; i++) {
            layout.part(19 + i, 93 + i * 26, -15 + OFFSET);
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

    /** What the Tier 3 Rocket page wants in matrix slot {@code index} (1.12.2 RecipeManagerAsteroids#addRecipes). */
    public static boolean isPart(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (index >= 19) {
            return stack.is(Tags.Items.CHESTS_WOODEN);
        }
        final Item wanted = switch (index) {
            case 1 -> GCItems.HEAVY_NOSE_CONE.get();
            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 -> GCItems.HEAVY_DUTY_PLATE_TIER_3.get();
            case 12, 16 -> GCItems.TIER_1_BOOSTER.get();
            case 13, 14, 17, 18 -> GCItems.HEAVY_ROCKET_FINS.get();
            case 15 -> GCItems.HEAVY_ROCKET_ENGINE.get();
            default -> null;
        };
        return wanted != null && stack.is(wanted);
    }

    /** Every part, plus 0-3 chests in any of the chest slots. */
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
        return GCItems.TIER_3_ROCKET.get().stack(chests, 0);
    }
}
