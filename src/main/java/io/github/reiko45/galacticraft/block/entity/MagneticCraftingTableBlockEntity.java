/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityCrafting and
 * inventory.PersistantInventoryCrafting). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.menu.MagneticCraftingMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2 TileEntityCrafting: the Magnetic Crafting Table's 3 x 3 grid, which stays when the screen is closed, and its
 * memory of the last recipe crafted (one of each ingredient, by slot). As a container (slots 0-8 the grid, 9 the
 * result) hoppers may only put in items the remembered recipe uses (or, while the grid holds another valid recipe,
 * that one), always into the emptiest matching slot, and may only take the result while the grid matches the memory;
 * taking it crafts once, keeping any extra results made at once for the next take (the hidden output buffer).
 */
public class MagneticCraftingTableBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int SIZE = 9;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final Matrix matrix = new Matrix();
    private final NonNullList<ItemStack> memory = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ItemStack hiddenOutput = ItemStack.EMPTY;
    private ItemStack memoryResult = ItemStack.EMPTY;
    @Nullable
    private Boolean overridden;
    private int changes;

    public MagneticCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.MAGNETIC_CRAFTING_TABLE.get(), pos, state);
    }

    public CraftingContainer matrix() {
        return this.matrix;
    }

    public ItemStack memory(int slot) {
        return this.memory.get(slot);
    }

    /** The result the remembered recipe makes (1.12.2 getMemoryHeld), shown in the screen. */
    public ItemStack memoryResult() {
        return this.memoryResult;
    }

    /** Bumped whenever the grid changes, so open screens can work out the result again. */
    public int changes() {
        return this.changes;
    }

    private void gridChanged() {
        this.changes++;
        this.overridden = null;
        setChanged();
    }

    // ------------------------------------------------------------------ recipes

    private Optional<RecipeHolder<CraftingRecipe>> recipe(CraftingInput input) {
        return this.level == null ? Optional.empty() : this.level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, this.level);
    }

    /** 1.12.2 CraftingManager.findMatchingResult on the grid. */
    public ItemStack result() {
        final CraftingInput input = this.matrix.asCraftInput();
        return recipe(input).map(holder -> holder.value().assemble(input, this.level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** 1.12.2 updateMemory: crafted by hand from a grid holding a recipe; remember one of each, by slot. */
    public void updateMemory(ItemStack crafted) {
        if (result().isEmpty()) {
            return;
        }
        this.memoryResult = crafted.copy();
        for (int i = 0; i < SIZE; i++) {
            this.memory.set(i, this.matrix.getItem(i).copyWithCount(1));
        }
        gridChanged();
    }

    private void updateMemoryResult() {
        final CraftingInput input = CraftingInput.of(3, 3, List.copyOf(this.memory));
        final ItemStack made = recipe(input).map(holder -> holder.value().assemble(input, this.level.registryAccess())).orElse(ItemStack.EMPTY);
        if (!made.isEmpty()) {
            this.memoryResult = made;
        }
    }

    /** 1.12.2 stillMatchesRecipe: every slot holds the remembered item, or is empty where the memory is. */
    protected boolean stillMatchesRecipe() {
        boolean any = false;
        for (int i = 0; i < SIZE; i++) {
            final ItemStack stack = this.matrix.getItem(i);
            final ItemStack target = this.memory.get(i);
            if (target.isEmpty() && stack.isEmpty()) {
                continue;
            }
            if (target.isEmpty() || stack.isEmpty() || !ItemStack.isSameItemSameComponents(target, stack)) {
                return false;
            }
            any = true;
        }
        return any;
    }

    /** 1.12.2 pullOneResultStack: use up one of each ingredient, leaving any containers (buckets...) behind. */
    private void pullOneResultStack() {
        final CraftingInput.Positioned positioned = this.matrix.asPositionedCraftInput();
        final CraftingInput input = positioned.input();
        final NonNullList<ItemStack> remaining = recipe(input).map(holder -> holder.value().getRemainingItems(input))
                .orElseGet(() -> NonNullList.withSize(input.size(), ItemStack.EMPTY));
        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                final int slot = column + positioned.left() + (row + positioned.top()) * 3;
                final ItemStack left = remaining.get(column + row * input.width());
                if (!this.matrix.getItem(slot).isEmpty()) {
                    this.matrix.removeItem(slot, 1);
                }
                if (!left.isEmpty() && this.matrix.getItem(slot).isEmpty()) {
                    this.matrix.setItem(slot, left);
                }
            }
        }
    }

    private void craftOutput() {
        if (stillMatchesRecipe() && !result().isEmpty()) {
            pullOneResultStack();
        }
    }

    /** 1.12.2 dropHiddenOutputBuffer: the results made but not taken. */
    public ItemStack takeHiddenOutput() {
        final ItemStack stack = this.hiddenOutput;
        this.hiddenOutput = ItemStack.EMPTY;
        return stack;
    }

    // ------------------------------------------------------------------ shift-click helpers (1.12.2 overrideMemory)

    private static boolean matching(ItemStack stack, ItemStack target) {
        return !target.isEmpty() && ItemStack.isSameItemSameComponents(stack, target) && target.isStackable() && target.getCount() < target.getMaxStackSize();
    }

    /**
     * 1.12.2 overrideMemory: when the grid holds a valid recipe other than the remembered one, and {@code stack} goes in
     * it, shift-clicking fills to the grid's own layout (copied into {@code memory}).
     */
    public boolean overrideMemory(ItemStack stack, NonNullList<ItemStack> memory) {
        if (this.matrix.isEmpty() || result().isEmpty()) {
            return false;
        }
        for (int i = 0; i < SIZE; i++) {
            if (!this.matrix.getItem(i).isEmpty() && !matching(this.matrix.getItem(i), this.memory.get(i))) {
                for (int j = 0; j < SIZE; j++) {
                    if (matching(stack, this.matrix.getItem(j))) {
                        for (int k = 0; k < SIZE; k++) {
                            memory.set(k, this.matrix.getItem(k).copy());
                        }
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /** 1.12.2 overriddenMemory: the grid holds a valid recipe that isn't the remembered one. */
    public boolean overriddenMemory() {
        if (this.overridden == null) {
            this.overridden = false;
            if (!result().isEmpty()) {
                for (int i = 0; i < SIZE; i++) {
                    if (!this.matrix.getItem(i).isEmpty() && !matching(this.matrix.getItem(i), this.memory.get(i))) {
                        this.overridden = true;
                        break;
                    }
                }
            }
        }
        return this.overridden;
    }

    // ------------------------------------------------------------------ container (for hoppers and pipes)

    @Override
    public int getContainerSize() {
        return SIZE + 1;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.memory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < SIZE) {
            return this.matrix.getItem(slot);
        }
        return this.hiddenOutput.isEmpty() ? result() : this.hiddenOutput;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < SIZE) {
            return this.matrix.removeItem(slot, amount);
        }
        if (slot == SIZE) {
            if (!this.hiddenOutput.isEmpty()) {
                final ItemStack taken = this.hiddenOutput.split(amount);
                setChanged();
                return taken;
            }
            if (stillMatchesRecipe()) {
                final ItemStack made = result();
                if (!made.isEmpty()) {
                    pullOneResultStack();
                    final ItemStack taken = made.split(amount);
                    this.hiddenOutput = made; // anything left over waits for the next take
                    setChanged();
                    return taken;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot >= 0 && slot < SIZE ? this.matrix.removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SIZE) {
            this.matrix.setItem(slot, stack);
        } else if (slot == SIZE) {
            if (stack.isEmpty()) {
                if (this.hiddenOutput.isEmpty()) {
                    craftOutput();
                }
            } else if (!ItemStack.isSameItem(stack, this.hiddenOutput)) {
                if (this.level != null && !this.hiddenOutput.isEmpty()) {
                    Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), this.hiddenOutput);
                }
                craftOutput();
            }
            this.hiddenOutput = stack;
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.matrix.clearContent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    /**
     * 1.12.2 canInsertItem: only into a slot the recipe (remembered, or the grid's own while it overrides the memory)
     * wants this item in, and only if no other such slot holds fewer, so hoppers fill the grid evenly.
     */
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        if (index >= SIZE || stack.isEmpty()) {
            return false;
        }
        final boolean override = overriddenMemory();
        final ItemStack target = override ? this.matrix.getItem(index) : this.memory.get(index);
        if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, stack)) {
            return false;
        }
        final ItemStack current = getItem(index);
        if (current.isEmpty()) {
            return true;
        }
        final int size = current.getCount();
        for (int i = 0; i < SIZE; i++) {
            if (i == index) {
                continue;
            }
            final ItemStack other = override ? this.matrix.getItem(i) : this.memory.get(i);
            if (!other.isEmpty() && ItemStack.isSameItemSameComponents(other, stack)) {
                final ItemStack held = override ? other : this.matrix.getItem(i);
                if (held.isEmpty()) {
                    return false; // an empty slot of the recipe fills first
                }
                if ((override || ItemStack.isSameItemSameComponents(stack, held)) && held.getCount() < size) {
                    return false; // a less full slot fills first
                }
            }
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SIZE && (!this.hiddenOutput.isEmpty() || stillMatchesRecipe());
    }

    // ------------------------------------------------------------------ saving and the screen

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag items = new ListTag();
        for (int i = 0; i < SIZE * 2; i++) {
            final ItemStack stack = i < SIZE ? this.matrix.getItem(i) : this.memory.get(i - SIZE);
            if (!stack.isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                items.add(stack.save(registries, entry));
            }
        }
        tag.put("Items", items);
        if (!this.hiddenOutput.isEmpty()) {
            tag.put("buf", this.hiddenOutput.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.matrix.stacks.replaceAll(stack -> ItemStack.EMPTY);
        this.memory.replaceAll(stack -> ItemStack.EMPTY);
        final ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            final CompoundTag entry = items.getCompound(i);
            final int slot = entry.getByte("Slot") & 255;
            final ItemStack stack = ItemStack.parseOptional(registries, entry);
            if (slot < SIZE) {
                this.matrix.stacks.set(slot, stack);
            } else if (slot < SIZE * 2) {
                this.memory.set(slot - SIZE, stack);
            }
        }
        this.hiddenOutput = ItemStack.parseOptional(registries, tag.getCompound("buf"));
        this.memoryResultDirty = true;
        this.changes++;
        this.overridden = null;
    }

    private boolean memoryResultDirty;

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            refreshMemoryResult();
        }
    }

    /** The remembered recipe's result needs the recipes, which aren't there yet while loading. */
    public void refreshMemoryResult() {
        if (this.memoryResultDirty && this.level != null) {
            this.memoryResultDirty = false;
            updateMemoryResult();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticraftcore.magnetic_crafting");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        refreshMemoryResult();
        return new MagneticCraftingMenu(id, inventory, this);
    }

    /** 1.12.2 PersistantInventoryCrafting: the grid, kept by the table. */
    private final class Matrix implements CraftingContainer {
        private final NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return List.copyOf(this.stacks);
        }

        @Override
        public int getContainerSize() {
            return SIZE;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : this.stacks) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < SIZE ? this.stacks.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            final ItemStack taken = ContainerHelper.removeItem(this.stacks, slot, amount);
            if (!taken.isEmpty()) {
                gridChanged();
            }
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            final ItemStack taken = ContainerHelper.takeItem(this.stacks, slot);
            gridChanged();
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.stacks.set(slot, stack);
            stack.limitSize(getMaxStackSize(stack));
            gridChanged();
        }

        @Override
        public void setChanged() {
            MagneticCraftingTableBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            this.stacks.replaceAll(stack -> ItemStack.EMPTY);
            gridChanged();
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            for (ItemStack stack : this.stacks) {
                contents.accountSimpleStack(stack);
            }
        }
    }
}
