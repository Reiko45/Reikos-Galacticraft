/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.inventory.ContainerCrafting and
 * SlotCraftingMemory). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.MagneticCraftingTableBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 1.12.2 ContainerCrafting: a crafting table's slots (result 0, grid 1-9, then the player's 27 and 9) over the Magnetic
 * Crafting Table's own grid, plus a slot that only shows the remembered recipe's result. Taking a result remembers the
 * recipe. Shift-clicking an item into the table spreads it evenly over the slots the remembered recipe (or the grid's
 * own recipe, when it holds another one) wants it in.
 */
public class MagneticCraftingMenu extends AbstractContainerMenu {
    public static final int RESULT = 0;
    public static final int MEMORY = 46;

    @Nullable
    private final MagneticCraftingTableBlockEntity table;
    private final CraftingContainer grid;
    private final ResultContainer result = new ResultContainer();
    private final SimpleContainer memoryShown = new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final Player player;
    /** 1.12.2 ContainerCrafting.memory: the layout shift-clicking fills to. */
    private final NonNullList<ItemStack> memory = NonNullList.withSize(9, ItemStack.EMPTY);
    private int lastChanges = -1;

    /** Client. */
    public MagneticCraftingMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public MagneticCraftingMenu(int id, Inventory inventory, @Nullable MagneticCraftingTableBlockEntity table) {
        super(GCMenus.MAGNETIC_CRAFTING.get(), id);
        this.table = table;
        this.player = inventory.player;
        this.grid = table == null ? new TransientCraftingContainer(this, 3, 3) : table.matrix();
        this.access = table == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(table.getLevel(), table.getBlockPos());
        addSlot(new ResultSlot(this.player, this.grid, this.result, 0, 124, 33) {
            @Override
            public void onTake(Player player, ItemStack stack) {
                if (MagneticCraftingMenu.this.table != null && !stack.isEmpty()) {
                    MagneticCraftingMenu.this.table.updateMemory(stack);
                }
                super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(this.grid, column + row * 3, 30 + column * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        // the remembered result, shown where 1.12.2 drew it (124, 59); it can't be taken or filled
        addSlot(new Slot(this.memoryShown, 0, 124, 59) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean isHighlightable() {
                return false;
            }
        });
        if (table != null) {
            updateResult();
        }
    }

    public boolean hasMemory() {
        return !this.memoryShown.getItem(0).isEmpty();
    }

    @Override
    public void slotsChanged(Container container) {
        if (this.table != null && container == this.grid) {
            updateResult();
        }
    }

    /** 1.12.2 onCraftMatrixChanged (vanilla CraftingMenu#slotChangedCraftingGrid on the server). */
    private void updateResult() {
        final Level level = this.table.getLevel();
        if (level == null || level.isClientSide || !(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        this.lastChanges = this.table.changes();
        final CraftingInput input = this.grid.asCraftInput();
        ItemStack made = ItemStack.EMPTY;
        final Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level, (RecipeHolder<CraftingRecipe>) null);
        if (recipe.isPresent() && this.result.setRecipeUsed(level, serverPlayer, recipe.get())) {
            final ItemStack assembled = recipe.get().value().assemble(input, level.registryAccess());
            if (assembled.isItemEnabled(level.enabledFeatures())) {
                made = assembled;
            }
        }
        this.result.setItem(0, made);
        setRemoteSlot(RESULT, made);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, incrementStateId(), RESULT, made));
    }

    @Override
    public void broadcastChanges() {
        if (this.table != null) {
            if (this.table.changes() != this.lastChanges) {
                updateResult(); // a hopper changed the grid while the screen was open
            }
            this.memoryShown.setItem(0, this.table.memoryResult().copyWithCount(1)); // 1.12.2 drew it without a count
        }
        super.broadcastChanges();
    }

    @Override
    public void broadcastFullState() {
        if (this.table != null) {
            if (this.table.changes() != this.lastChanges) {
                updateResult();
            }
            this.memoryShown.setItem(0, this.table.memoryResult().copyWithCount(1)); // 1.12.2 drew it without a count
        }
        super.broadcastFullState();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, GCBlocks.MAGNETIC_CRAFTING_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.result && slot.container != this.memoryShown && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);
        if (index == MEMORY || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        copy = stack.copy();
        if (index == RESULT) {
            this.access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, player));
            if (!moveItemStackTo(stack, 10, 46, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index < 10) {
            if (!moveItemStackTo(stack, 10, 46, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.table != null && matchesCrafting(stack)) {
            if (!mergeToCrafting(stack)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 37) {
            if (!moveItemStackTo(stack, 37, 46, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 10, 37, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (index == RESULT) {
            player.drop(stack, false);
        }
        return copy;
    }

    private static boolean matching(ItemStack stack, ItemStack target) {
        return !target.isEmpty() && ItemStack.isSameItemSameComponents(stack, target) && (!target.isStackable() || target.getCount() < target.getMaxStackSize());
    }

    /** 1.12.2 matchesCrafting: does the remembered (or overriding) recipe want this item somewhere with room? */
    private boolean matchesCrafting(ItemStack stack) {
        if (this.table.overrideMemory(stack, this.memory)) {
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (matching(stack, this.table.memory(i)) && (this.grid.getItem(i).isEmpty() || this.grid.getItem(i).getCount() < stack.getMaxStackSize())) {
                for (int j = 0; j < 9; j++) {
                    this.memory.set(j, this.table.memory(j));
                }
                return true;
            }
        }
        return false;
    }

    /** 1.12.2 mergeToCrafting: spread the stack evenly over the slots that take it, emptiest first. */
    private boolean mergeToCrafting(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final List<Slot> accept = new ArrayList<>();
        final List<Integer> space = new ArrayList<>();
        int minSpace = 64;
        int total = 0;
        for (int i = 1; i < 10; i++) {
            final Slot slot = this.slots.get(i);
            ItemStack target = slot.getItem();
            final ItemStack wanted = this.memory.get(i - 1);
            if (wanted.isEmpty()) {
                continue;
            }
            if (target.isEmpty()) {
                target = wanted.copy();
            }
            if (matching(stack, target)) {
                accept.add(slot);
                final int room = target.getMaxStackSize() - target.getCount();
                space.add(room);
                total += room;
                minSpace = Math.min(minSpace, room);
            }
        }
        // first one into each empty slot of the recipe
        for (Slot slot : accept) {
            if (slot.getItem().isEmpty()) {
                this.grid.setItem(slot.index - 1, this.memory.get(slot.index - 1).copy());
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return false;
                }
            }
        }
        if (stack.getCount() >= total) {
            if (total == 0) {
                return false;
            }
            for (Slot slot : accept) {
                final ItemStack target = slot.getItem();
                stack.shrink(target.getMaxStackSize() - target.getCount());
                target.setCount(target.getMaxStackSize());
                slot.setChanged();
            }
            return true;
        }
        int uneven = 0;
        for (int room : space) {
            uneven += room - minSpace;
        }
        if (stack.getCount() <= uneven) {
            // top up the emptiest, one at a time
            do {
                Slot neediest = null;
                int smallest = 64;
                for (Slot slot : accept) {
                    if (slot.getItem().getCount() < smallest) {
                        smallest = slot.getItem().getCount();
                        neediest = slot;
                    }
                }
                neediest.getItem().grow(1);
                stack.shrink(1);
            } while (!stack.isEmpty());
            for (Slot slot : accept) {
                slot.setChanged();
            }
            return true;
        }
        if (uneven > 0) {
            final int level = stack.getMaxStackSize() - minSpace;
            for (Slot slot : accept) {
                final ItemStack target = slot.getItem();
                stack.shrink(level - target.getCount());
                target.setCount(level);
                slot.setChanged();
            }
        }
        final int average = stack.getCount() / accept.size();
        int remainder = stack.getCount() - average * accept.size();
        for (Slot slot : accept) {
            final ItemStack target = slot.getItem();
            int transfer = average;
            if (remainder > 0) {
                transfer++;
                remainder--;
            }
            transfer = Math.min(transfer, stack.getCount());
            stack.shrink(transfer);
            target.grow(transfer);
            if (target.getCount() > target.getMaxStackSize()) {
                stack.grow(target.getCount() - target.getMaxStackSize());
                target.setCount(target.getMaxStackSize());
            }
            slot.setChanged();
            if (stack.isEmpty()) {
                break;
            }
        }
        return true;
    }
}
