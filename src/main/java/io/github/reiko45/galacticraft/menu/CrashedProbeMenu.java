/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.inventory.ContainerCrashedProbe).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Crashed Probe (176 x 133): its six slots in a row at (35, 18), the player's inventory from y 52. */
public class CrashedProbeMenu extends AbstractContainerMenu {
    private final Container probe;

    public CrashedProbeMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(6));
    }

    public CrashedProbeMenu(int id, Inventory playerInventory, Container probe) {
        super(GCMenus.CRASHED_PROBE.get(), id);
        checkContainerSize(probe, 6);
        this.probe = probe;
        probe.startOpen(playerInventory.player);
        for (int i = 0; i < 6; i++) {
            addSlot(new Slot(probe, i, 35 + i * 18, 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 52 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 110));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < 6) {
            if (!moveItemStackTo(stack, 6, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 6, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.probe.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.probe.stopOpen(player);
    }
}
