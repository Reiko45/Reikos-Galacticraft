/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.inventory.ContainerPainter). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.ChromaticApplicatorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCMenus;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 1.12.2 ContainerPainter: the colour source (40,25) and the thing to paint (122,25), the player's inventory from (8,104);
 * the buttons arrive as menu button clicks (1.12.2 S_UPDATE_DISABLEABLE_BUTTON).
 */
public class ChromaticApplicatorMenu extends AbstractContainerMenu {
    @Nullable
    private final ChromaticApplicatorBlockEntity applicator;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client. */
    public ChromaticApplicatorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, null, new SimpleContainerData(2), new ItemStackHandler(2));
    }

    public ChromaticApplicatorMenu(int id, Inventory playerInventory, ChromaticApplicatorBlockEntity applicator, ContainerData data) {
        this(id, playerInventory, applicator, data, applicator.inventory());
    }

    private ChromaticApplicatorMenu(int id, Inventory playerInventory, @Nullable ChromaticApplicatorBlockEntity applicator, ContainerData data,
                                    IItemHandler inventory) {
        super(GCMenus.CHROMATIC_APPLICATOR.get(), id);
        this.applicator = applicator;
        this.data = data;
        this.access = applicator == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(applicator.getLevel(), applicator.getBlockPos());
        addSlot(new SlotItemHandler(inventory, 0, 40, 25));
        addSlot(new SlotItemHandler(inventory, 1, 122, 25));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 104 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 162));
        }
        addDataSlots(data);
    }

    /** The colour being mixed. */
    public int color() {
        return this.data.get(0) & 0xFFFF | (this.data.get(1) & 0xFF) << 16;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.applicator != null && id >= 0 && id <= 2) {
            this.applicator.buttonPressed(id, player);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, GCBlocks.CHROMATIC_APPLICATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
