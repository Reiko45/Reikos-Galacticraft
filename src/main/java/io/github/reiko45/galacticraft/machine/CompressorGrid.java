package io.github.reiko45.galacticraft.machine;

import io.github.reiko45.galacticraft.recipe.CompressingRecipe;
import io.github.reiko45.galacticraft.registry.GCRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The 3x3 compressing grid shared by the Compressor and the Electric Compressor (1.12.2 compressingCraftMatrix). */
public final class CompressorGrid {
    private final ItemStackHandler inventory;
    private final int start;

    public CompressorGrid(ItemStackHandler inventory, int start) {
        this.inventory = inventory;
        this.start = start;
    }

    public boolean contains(int slot) {
        return slot >= this.start && slot < this.start + 9;
    }

    public CraftingInput input() {
        final List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            items.add(this.inventory.getStackInSlot(this.start + i));
        }
        return CraftingInput.of(3, 3, items);
    }

    public Optional<CompressingRecipe> recipe(Level level) {
        if (level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(GCRecipes.COMPRESSING.get(), input(), level).map(RecipeHolder::value);
    }

    public Optional<ItemStack> result(Level level) {
        return recipe(level).map(recipe -> quickMode(recipe.assemble(input(), level.registryAccess()), io.github.reiko45.galacticraft.GCConfig.quickMode()));
    }

    /**
     * 1.12.2 quick mode: a compressor makes twice as many of anything "compressed" (the plates, not the heavy duty ones),
     * in both slots of the Electric Compressor. The result is a copy; the recipe's own stack is left alone.
     */
    public static ItemStack quickMode(ItemStack result, boolean quick) {
        if (quick && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).getPath().contains("compressed")) {
            return result.copyWithCount(result.getCount() * 2);
        }
        return result;
    }

    /** Use up one of each grid item, leaving container items behind (water bucket -> bucket). */
    public void consume() {
        for (int i = 0; i < 9; i++) {
            final int slot = this.start + i;
            final ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                final ItemStack remainder = stack.getCraftingRemainingItem();
                final ItemStack shrunk = stack.copy();
                shrunk.shrink(1);
                this.inventory.setStackInSlot(slot, shrunk.isEmpty() ? remainder : shrunk);
            }
        }
    }

    /**
     * Hopper/pipe input. Once the grid holds a valid recipe only the emptiest slot already holding the item is
     * topped up (1.12.2 balancing); before that, items are spread one per empty slot until there are as many as
     * any recipe needs of them.
     */
    public boolean canAutomationInsert(Level level, int slot, ItemStack stack) {
        if (level == null || !contains(slot)) {
            return false;
        }
        final ItemStack current = this.inventory.getStackInSlot(slot);
        if (recipe(level).isPresent()) {
            if (!ItemStack.isSameItemSameComponents(current, stack)) {
                return false;
            }
            for (int i = 0; i < 9; i++) {
                final ItemStack other = this.inventory.getStackInSlot(this.start + i);
                if (ItemStack.isSameItemSameComponents(other, stack) && other.getCount() < current.getCount()) {
                    return false;
                }
            }
            return true;
        }
        if (!current.isEmpty()) {
            return false;
        }
        int already = 0;
        for (int i = 0; i < 9; i++) {
            if (ItemStack.isSameItemSameComponents(this.inventory.getStackInSlot(this.start + i), stack)) {
                already++;
            }
        }
        int needed = 0;
        for (RecipeHolder<CompressingRecipe> holder : level.getRecipeManager().getAllRecipesFor(GCRecipes.COMPRESSING.get())) {
            int matching = 0;
            for (Ingredient ingredient : holder.value().getIngredients()) {
                if (ingredient.test(stack)) {
                    matching++;
                }
            }
            needed = Math.max(needed, matching);
        }
        return already < needed;
    }
}
