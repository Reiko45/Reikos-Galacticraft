/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.inventory.ContainerSchematicAstroMiner,
 * SlotSchematicAstroMiner and recipe.RecipeManagerAsteroids). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
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
 * NASA Workbench, Astro Miner page (1.12.2 ContainerSchematicAstroMiner, 221 tall, drawn from 26 down its texture): four
 * heavy plates, five Orion Drives, an advanced wafer, two wooden chests, a Beam Core and a steel pole make an Astro
 * Miner. Unlocked with the Astro Miner Schematic the Creeper Boss drops.
 */
public class AstroMinerBenchMenu extends PartBenchMenu {
    public static final int MATRIX_SIZE = 15;

    public AstroMinerBenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public AstroMinerBenchMenu(int id, Inventory playerInventory, ContainerLevelAccess access) {
        super(GCMenus.ASTRO_MINER_BENCH.get(), id, playerInventory, access, Schematics.ASTRO_MINER, MATRIX_SIZE);
        addResultSlot(142, 72);
        int index = 1;
        for (int i = 0; i < 4; i++) {
            addPartSlot(index++, 27 + i * 18, 35);
        }
        for (int i = 0; i < 5; i++) {
            addPartSlot(index++, 16 + i * 18, 53);
        }
        for (int i = 0; i < 3; i++) {
            addPartSlot(index++, 44 + i * 18, 71);
        }
        for (int i = 0; i < 2; i++) {
            addPartSlot(index++, 8 + i * 18, 77);
        }
        addPlayerInventory(playerInventory, 140 - 26);
    }

    @Override
    protected boolean accepts(int index, ItemStack stack) {
        return isPart(index, stack);
    }

    @Override
    protected ItemStack assemble(Container matrix) {
        return craft(matrix);
    }

    /** What the Astro Miner page wants in matrix slot {@code index} (1.12.2 RecipeManagerAsteroids#addRecipes). */
    public static boolean isPart(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (index == 7 || index == 8) {
            return stack.is(Tags.Items.CHESTS_WOODEN);
        }
        final Item wanted = switch (index) {
            case 1, 3, 5, 11 -> GCItems.HEAVY_PLATING.get();
            case 2, 4, 9, 10, 12 -> GCItems.ORION_DRIVE.get();
            case 6 -> GCItems.ADVANCED_WAFER.get();
            case 13 -> GCItems.BEAM_CORE.get();
            case 14 -> GCItems.STEEL_POLE.get();
            default -> null;
        };
        return wanted != null && stack.is(wanted);
    }

    public static ItemStack craft(Container matrix) {
        for (int i = 1; i < MATRIX_SIZE; i++) {
            if (!isPart(i, matrix.getItem(i))) {
                return ItemStack.EMPTY;
            }
        }
        return new ItemStack(GCItems.ASTRO_MINER.get());
    }
}
