/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.inventory.InventorySlimeling). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A Slimeling's 30 slots (1.12.2 InventorySlimeling): slot 1 holds the Slimeling Inventory Bag, slots 2-28 are the
 * bag's 27 stacks (slots 0 and 29 are unused, as in 1.12.2). Taking the bag out spills what was in it.
 */
public class SlimelingInventory extends SimpleContainer {
    public static final int SIZE = 30;
    public static final int BAG_SLOT = 1;
    public static final int CARGO_START = 2;
    public static final int CARGO_SLOTS = 27;

    @Nullable
    private final SlimelingEntity slimeling;

    /** {@code slimeling} is null for the client copy behind a menu. */
    public SlimelingInventory(@Nullable SlimelingEntity slimeling) {
        super(SIZE);
        this.slimeling = slimeling;
    }

    public NonNullList<ItemStack> items() {
        return getItems();
    }

    public static boolean isBag(ItemStack stack) {
        return stack.is(GCItems.SLIMELING_CARGO.get());
    }

    public boolean hasBag() {
        return isBag(getItem(BAG_SLOT));
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        if (index == BAG_SLOT) {
            spillIfBagless();
        }
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        final ItemStack removed = super.removeItem(index, count);
        if (index == BAG_SLOT) {
            spillIfBagless();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        final ItemStack removed = super.removeItemNoUpdate(index);
        if (index == BAG_SLOT) {
            spillIfBagless();
        }
        return removed;
    }

    /** 1.12.2 removeInventoryBagContents. */
    private void spillIfBagless() {
        if (this.slimeling == null || this.slimeling.level().isClientSide || hasBag()) {
            return;
        }
        for (int i = CARGO_START; i < CARGO_START + CARGO_SLOTS; i++) {
            final ItemStack stack = super.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                this.slimeling.spawnAtLocation(stack, 0.5F);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.slimeling == null || this.slimeling.isAlive() && player.distanceToSqr(this.slimeling) <= 64.0D;
    }
}
