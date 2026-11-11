package io.github.reiko45.galacticraft.world.station;

import io.github.reiko45.galacticraft.data.GCTags;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a space station costs (1.12.2 RecipeManagerGC spaceStationRequirements and SpaceStationRecipe): 32 tin ingots,
 * 16 aluminum ingots, an advanced wafer and 24 iron ingots, taken from the player's inventory. Creative players build
 * for free.
 */
public final class StationRecipe {
    /** One required ingredient: what matches, how many, and what to show. */
    public record Requirement(Predicate<ItemStack> matches, int count, Supplier<ItemStack> icon) {
        public int countIn(Inventory inventory) {
            int found = 0;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                final ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && this.matches.test(stack)) {
                    found += stack.getCount();
                }
            }
            return found;
        }
    }

    public static final List<Requirement> REQUIREMENTS = List.of(
            new Requirement(stack -> stack.is(GCTags.item("ingots/tin")), 32, () -> new ItemStack(GCItems.TIN_INGOT.get())),
            new Requirement(stack -> stack.is(GCTags.item("ingots/aluminum")), 16, () -> new ItemStack(GCItems.ALUMINUM_INGOT.get())),
            new Requirement(stack -> stack.is(GCItems.ADVANCED_WAFER.get()), 1, () -> new ItemStack(GCItems.ADVANCED_WAFER.get())),
            new Requirement(stack -> stack.is(Items.IRON_INGOT), 24, () -> new ItemStack(Items.IRON_INGOT)));

    private StationRecipe() {
    }

    public static boolean matches(Player player) {
        for (Requirement requirement : REQUIREMENTS) {
            if (requirement.countIn(player.getInventory()) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    /** 1.12.2 SpaceStationRecipe#matches(player, true): check, then take everything. */
    public static boolean consume(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (!matches(player)) {
            return false;
        }
        final Inventory inventory = player.getInventory();
        for (Requirement requirement : REQUIREMENTS) {
            int left = requirement.count();
            for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
                final ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && requirement.matches().test(stack)) {
                    final int taken = Math.min(left, stack.getCount());
                    stack.shrink(taken);
                    left -= taken;
                }
            }
        }
        inventory.setChanged();
        return true;
    }
}
