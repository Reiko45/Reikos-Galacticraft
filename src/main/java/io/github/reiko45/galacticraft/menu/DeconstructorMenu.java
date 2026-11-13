/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.inventory.ContainerDeconstructor). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.DeconstructorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 ContainerDeconstructor: battery (55,75), input (26,36), the 3 x 3 output from (112,18); player from (8,117). */
public class DeconstructorMenu extends ProcessingMenu {
    public DeconstructorMenu(int id, Inventory playerInventory, DeconstructorBlockEntity machine) {
        super(GCMenus.DECONSTRUCTOR.get(), id, playerInventory, machine);
    }

    public DeconstructorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.DECONSTRUCTOR.get(), id, playerInventory, buf, DeconstructorBlockEntity.SLOTS);
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 55, 75);
        addMachineSlot(1, 26, 36);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addOutputSlot(2 + row * 3 + column, 112 + column * 18, 18 + row * 18);
            }
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[] {8, 117};
    }
}
