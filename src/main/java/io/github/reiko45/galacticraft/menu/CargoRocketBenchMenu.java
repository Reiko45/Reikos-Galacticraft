/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.inventory.ContainerSchematicCargoRocket,
 * SlotSchematicCargoRocket and recipe.RecipeManagerMars cargo rocket recipes). Copyright (c) 2023 Team Galacticraft.
 * Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
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
 * The Cargo Rocket page of the NASA Workbench: nose cone, advanced wafer, six heavy-duty plates, four fins, a rocket
 * engine and one to three chests (18 slots each).
 */
public class CargoRocketBenchMenu extends PartBenchMenu {
    public static final int MATRIX_SIZE = 17;

    public CargoRocketBenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public CargoRocketBenchMenu(int id, Inventory playerInventory, ContainerLevelAccess access) {
        super(GCMenus.CARGO_ROCKET_BENCH.get(), id, playerInventory, access, Schematics.CARGO_ROCKET, MATRIX_SIZE);
        layout(slots());
        addPlayerInventory(playerInventory, 129 + 9);
    }

    /** Where this page's slots go (shared with the JEI and EMI pages). */
    public static void layout(Layout layout) {
        layout.result(142, 69 + OFFSET);
        layout.part(1, 48, -9 + OFFSET);
        layout.part(2, 48, -9 + 18 + OFFSET);
        for (int i = 0; i < 3; i++) {
            layout.part(3 + i, 39, -7 + i * 18 + 16 + 18 + OFFSET);
        }
        for (int i = 0; i < 3; i++) {
            layout.part(6 + i, 57, -7 + i * 18 + 16 + 18 + OFFSET);
        }
        layout.part(9, 21, 63 + OFFSET);
        layout.part(10, 21, 81 + OFFSET);
        layout.part(11, 48, 81 + OFFSET);
        layout.part(12, 75, 63 + OFFSET);
        layout.part(13, 75, 81 + OFFSET);
        for (int i = 0; i < 3; i++) {
            layout.part(14 + i, 93 + i * 26, -15 + OFFSET);
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

    /** What the Cargo Rocket page wants in matrix slot {@code index}. */
    public static boolean isPart(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (index >= 14) {
            return stack.is(Tags.Items.CHESTS_WOODEN);
        }
        final Item wanted = switch (index) {
            case 1 -> GCItems.NOSE_CONE.get();
            case 2 -> GCItems.ADVANCED_WAFER.get();
            case 3, 4, 5, 6, 7, 8 -> GCItems.HEAVY_DUTY_PLATE.get();
            case 9, 10, 12, 13 -> GCItems.ROCKET_FINS.get();
            case 11 -> GCItems.TIER_1_ROCKET_ENGINE.get();
            default -> null;
        };
        return wanted != null && stack.is(wanted);
    }

    /** 1.12.2 adCargoRocketRecipe recipes: every part and one, two or three chests (in any chest slots). */
    public static ItemStack craft(Container matrix) {
        for (int i = 1; i < 14; i++) {
            if (!isPart(i, matrix.getItem(i))) {
                return ItemStack.EMPTY;
            }
        }
        int chests = 0;
        for (int i = 14; i < MATRIX_SIZE; i++) {
            final ItemStack stack = matrix.getItem(i);
            if (!stack.isEmpty()) {
                if (!isPart(i, stack)) {
                    return ItemStack.EMPTY;
                }
                chests++;
            }
        }
        return chests == 0 ? ItemStack.EMPTY : GCItems.CARGO_ROCKET.get().stack(chests, 0);
    }
}
