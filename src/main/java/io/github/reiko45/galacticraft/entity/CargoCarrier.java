/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.api.entity.ICargoEntity and
 * api.prefab.entity.EntityAutoRocket#addCargo / #removeCargo). Copyright (c) 2023 Team Galacticraft. Licensed under the
 * MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** A vehicle Cargo Loaders and Unloaders can fill and empty one item at a time (1.12.2 ICargoEntity). */
public interface CargoCarrier {
    /** 1.12.2 EnumCargoLoadingState. */
    enum LoadState {
        SUCCESS, FULL, EMPTY, NO_INVENTORY, NO_TARGET
    }

    /** 1.12.2 RemovalResult: one item taken out, or why none was. */
    record Removal(LoadState state, ItemStack stack) {
    }

    LoadState addCargo(ItemStack stack, boolean doAdd);

    Removal removeCargo(boolean doRemove);

    /** 1.12.2 addCargo over the first {@code slots} slots: top up a matching stack, else take an empty slot. */
    static LoadState add(Container container, int slots, ItemStack stack, boolean doAdd) {
        if (slots <= 0) {
            return LoadState.NO_INVENTORY;
        }
        for (int i = 0; i < slots; i++) {
            final ItemStack at = container.getItem(i);
            if (!at.isEmpty() && ItemStack.isSameItemSameComponents(at, stack) && at.getCount() + stack.getCount() <= at.getMaxStackSize()) {
                if (doAdd) {
                    at.grow(stack.getCount());
                    container.setChanged();
                }
                return LoadState.SUCCESS;
            }
        }
        for (int i = 0; i < slots; i++) {
            if (container.getItem(i).isEmpty()) {
                if (doAdd) {
                    container.setItem(i, stack.copy());
                }
                return LoadState.SUCCESS;
            }
        }
        return LoadState.FULL;
    }

    /** 1.12.2 removeCargo: one item from the first non-empty slot. */
    static Removal remove(Container container, int slots, boolean doRemove) {
        if (slots <= 0) {
            return new Removal(LoadState.NO_INVENTORY, ItemStack.EMPTY);
        }
        for (int i = 0; i < slots; i++) {
            final ItemStack at = container.getItem(i);
            if (!at.isEmpty()) {
                final ItemStack one = at.copyWithCount(1);
                if (doRemove) {
                    at.shrink(1);
                    container.setChanged();
                }
                return new Removal(LoadState.SUCCESS, one);
            }
        }
        return new Removal(LoadState.EMPTY, ItemStack.EMPTY);
    }
}
