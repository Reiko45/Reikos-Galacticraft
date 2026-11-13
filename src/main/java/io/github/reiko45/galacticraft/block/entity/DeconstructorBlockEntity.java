/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityDeconstructor). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.DeconstructorMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2 TileEntityDeconstructor: takes apart the item put in (250 ticks at 75 gJ/t), following crafting recipes and the
 * NASA Workbench pages back down to the metals and parts it knows (the salvage list: ingots, compressed metals, plates,
 * diamonds...), up to 10 steps deep; each piece found comes back three times in four. Slots: 0 battery, 1 input, 2-10
 * output (what doesn't fit is dropped).
 */
public class DeconstructorBlockEntity extends ProcessingMachineBlockEntity {
    public static final int SLOTS = 11;
    public static final int PROCESS_TIME = 250;
    /** 1.12.2 SALVAGE_CHANCE. */
    public static final float SALVAGE_CHANCE = 0.75F;

    /** 1.12.2 initialiseItemList. */
    private static final List<Supplier<? extends Item>> SALVAGE = List.of(GCItems.HEAVY_DUTY_PLATE_TIER_3, GCItems.COMPRESSED_TITANIUM,
            GCItems.TITANIUM_INGOT, GCItems.HEAVY_DUTY_PLATE, GCItems.COMPRESSED_DESH, GCItems.DESH_INGOT, GCItems.STEEL_POLE, GCItems.HEAVY_PLATING,
            GCItems.COMPRESSED_METEORIC_IRON, GCItems.METEORIC_IRON_INGOT, GCItems.COMPRESSED_STEEL, GCItems.COMPRESSED_BRONZE,
            GCItems.COMPRESSED_COPPER, GCItems.COMPRESSED_TIN, GCItems.COMPRESSED_ALUMINUM, GCItems.COMPRESSED_IRON, () -> Items.COPPER_INGOT,
            GCItems.TIN_INGOT, GCItems.ALUMINUM_INGOT, () -> Items.IRON_INGOT, () -> Items.GOLD_INGOT, () -> Items.GOLD_NUGGET, () -> Items.DIAMOND);

    /**
     * 1.12.2 knownRecipes: the NASA Workbench pages (each without its storage chests), checked before crafting recipes:
     * the Tier 1 Rocket, Moon Buggy, Tier 2 Rocket, Cargo Rocket, Tier 3 Rocket and Astro Miner.
     */
    private static Map<Item, List<ItemStack>> benchRecipes() {
        return Map.of(
                GCItems.TIER_1_ROCKET.get(), List.of(stack(GCItems.NOSE_CONE.get(), 1), stack(GCItems.HEAVY_PLATING.get(), 8),
                        stack(GCItems.ROCKET_FINS.get(), 4), stack(GCItems.TIER_1_ROCKET_ENGINE.get(), 1)),
                GCItems.BUGGY.get(), List.of(stack(GCItems.BUGGY_SEAT.get(), 1), stack(GCItems.HEAVY_PLATING.get(), 12), stack(GCItems.BUGGY_WHEEL.get(), 4)),
                GCItems.TIER_2_ROCKET.get(), List.of(stack(GCItems.NOSE_CONE.get(), 1), stack(GCItems.HEAVY_DUTY_PLATE.get(), 10),
                        stack(GCItems.TIER_1_BOOSTER.get(), 2), stack(GCItems.ROCKET_FINS.get(), 4), stack(GCItems.TIER_1_ROCKET_ENGINE.get(), 1)),
                GCItems.CARGO_ROCKET.get(), List.of(stack(GCItems.NOSE_CONE.get(), 1), stack(GCItems.ADVANCED_WAFER.get(), 1),
                        stack(GCItems.HEAVY_DUTY_PLATE.get(), 6), stack(GCItems.ROCKET_FINS.get(), 4), stack(GCItems.TIER_1_ROCKET_ENGINE.get(), 1)),
                GCItems.TIER_3_ROCKET.get(), List.of(stack(GCItems.HEAVY_NOSE_CONE.get(), 1), stack(GCItems.HEAVY_DUTY_PLATE_TIER_3.get(), 10),
                        stack(GCItems.TIER_1_BOOSTER.get(), 2), stack(GCItems.HEAVY_ROCKET_FINS.get(), 4), stack(GCItems.HEAVY_ROCKET_ENGINE.get(), 1)),
                GCItems.ASTRO_MINER.get(), List.of(stack(GCItems.HEAVY_PLATING.get(), 4), stack(GCItems.ORION_DRIVE.get(), 5),
                        stack(GCItems.ADVANCED_WAFER.get(), 1), stack(GCItems.BEAM_CORE.get(), 1), stack(GCItems.STEEL_POLE.get(), 1)));
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private int depth;

    public DeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.DECONSTRUCTOR.get(), pos, state, SLOTS, 16_000.0F, 75.0F);
    }

    public static boolean isSalvage(ItemStack stack) {
        for (Supplier<? extends Item> item : SALVAGE) {
            if (stack.is(item.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected float energyPerTick() {
        return io.github.reiko45.galacticraft.GCConfig.hardMode() ? 90.0F : 75.0F;
    }

    @Override
    public int processTime() {
        return PROCESS_TIME;
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    @Override
    protected void tick() {
        super.tick();
        // 1.12.2: the grinding sound five times a run
        if (isWorking() && this.progress % 50 == 1 && this.level != null) {
            this.level.playSound(null, this.worldPosition, GCSounds.DECONSTRUCTOR.get(), SoundSource.BLOCKS, 0.25F, this.level.random.nextFloat() * 0.04F + 0.38F);
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 ? stack.getItem() instanceof ElectricItem : slot == 1;
    }

    @Override
    protected boolean canProcess() {
        return !this.inventory.getStackInSlot(1).isEmpty();
    }

    /** 1.12.2 deconstruct. */
    @Override
    protected void process() {
        final List<ItemStack> start = new ArrayList<>();
        start.add(this.inventory.getStackInSlot(1).copyWithCount(1));
        this.depth = 0;
        final List<ItemStack> salvaged = randomChance(squash(salvage(start, null)));
        for (ItemStack output : salvaged) {
            addToOutput(output);
        }
        this.inventory.extractItem(1, 1, false);
    }

    /** What {@code stack} comes apart into, salvage only, without the chance (for JEI and tests). */
    public List<ItemStack> salvageOf(ItemStack stack) {
        this.depth = 0;
        final List<ItemStack> start = new ArrayList<>();
        start.add(stack.copyWithCount(1));
        return squash(salvage(start, null));
    }

    /** 1.12.2 getSalvageable: salvage stays as it is, anything else is broken down into its recipe's ingredients. */
    private List<ItemStack> salvage(List<ItemStack> ingredients, ItemStack done) {
        final List<ItemStack> result = new ArrayList<>();
        if (ingredients == null || ingredients.isEmpty() || this.depth++ > 10) {
            return result;
        }
        for (ItemStack stack : ingredients) {
            if (isSalvage(stack)) {
                result.add(stack);
            } else {
                final List<ItemStack> parts = ingredients(stack);
                if (parts != null) {
                    if (done != null) {
                        parts.removeIf(part -> ItemStack.matches(part, done)); // A -> B -> A
                    }
                    result.addAll(salvage(parts, stack));
                }
            }
        }
        this.depth--;
        return result;
    }

    /** 1.12.2 squashList: one stack of each item. */
    private static List<ItemStack> squash(List<ItemStack> stacks) {
        final List<ItemStack> result = new ArrayList<>();
        outer:
        for (ItemStack stack : stacks) {
            for (ItemStack existing : result) {
                if (ItemStack.isSameItem(stack, existing)) {
                    existing.grow(stack.getCount());
                    continue outer;
                }
            }
            result.add(stack.copy());
        }
        return result;
    }

    /** 1.12.2 randomChanceList: each piece comes back three times in four. */
    private List<ItemStack> randomChance(List<ItemStack> stacks) {
        final List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            int kept = 0;
            for (int i = 0; i < stack.getCount(); i++) {
                if (this.level.random.nextFloat() < SALVAGE_CHANCE) {
                    kept++;
                }
            }
            if (kept > 0) {
                result.add(stack.copyWithCount(kept));
            }
        }
        return result;
    }

    /** 1.12.2 getIngredients: a workbench page, else the first crafting recipe (by id) making exactly one. */
    private List<ItemStack> ingredients(ItemStack stack) {
        final List<ItemStack> bench = benchRecipes().get(stack.getItem());
        if (bench != null) {
            final List<ItemStack> copy = new ArrayList<>();
            for (ItemStack part : bench) {
                for (int i = 0; i < part.getCount(); i++) {
                    copy.add(part.copyWithCount(1));
                }
            }
            return copy;
        }
        final List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>(this.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING));
        recipes.sort(Comparator.comparing(holder -> holder.id().toString()));
        for (RecipeHolder<CraftingRecipe> holder : recipes) {
            final ItemStack made = holder.value().getResultItem(this.level.registryAccess());
            if (made.getCount() == 1 && ItemStack.isSameItem(made, stack)) {
                final List<ItemStack> parts = new ArrayList<>();
                for (Ingredient ingredient : holder.value().getIngredients()) {
                    final ItemStack part = parse(ingredient);
                    if (!part.isEmpty()) {
                        parts.add(part);
                    }
                }
                return parts;
            }
        }
        return null;
    }

    /** 1.12.2 parseRecipeInput: of what an ingredient takes, salvage if any (it chose so for ore names), else the first. */
    private static ItemStack parse(Ingredient ingredient) {
        final ItemStack[] options = ingredient.getItems();
        for (ItemStack option : options) {
            if (isSalvage(option)) {
                return option.copyWithCount(1);
            }
        }
        return options.length == 0 ? ItemStack.EMPTY : options[0].copyWithCount(1);
    }

    /** 1.12.2 addToOutputMatrix: onto matching stacks or into the first empty slot, the rest dropped. */
    private void addToOutput(ItemStack stack) {
        for (int slot = 2; slot < SLOTS && !stack.isEmpty(); slot++) {
            final ItemStack current = this.inventory.getStackInSlot(slot);
            if (current.isEmpty()) {
                output(slot, stack);
                stack = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(current, stack)) {
                final int room = Math.min(current.getMaxStackSize(), this.inventory.getSlotLimit(slot)) - current.getCount();
                final int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    output(slot, stack.copyWithCount(moved));
                    stack.shrink(moved);
                }
            }
        }
        if (!stack.isEmpty() && this.level != null) {
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D, stack);
        }
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    /** 1.12.2 getSlotsForFace: items in from any side but the bottom, out from the bottom. */
    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return slot == 1 && side != RelativeSide.BOTTOM;
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return slot >= 2 && side == RelativeSide.BOTTOM;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new DeconstructorMenu(id, playerInventory, this);
    }
}
