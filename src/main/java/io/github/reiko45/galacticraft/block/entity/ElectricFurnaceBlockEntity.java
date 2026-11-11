package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.ElectricFurnaceMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * Electric Furnace (tier 1: 45 gJ/t, 130 ticks) and Electric Arc Furnace (tier 2: 60 gJ/t, 65 ticks on tier-1
 * power and 32 on tier-2, doubles ore-to-ingot smelting), from 1.12.2 TileEntityElectricFurnace.
 * Slots: 0 battery, 1 input, 2 output, 3 second output (arc only).
 */
public class ElectricFurnaceBlockEntity extends ProcessingMachineBlockEntity {
    private final boolean arc;
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes = RecipeManager.createCheck(RecipeType.SMELTING);

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state, boolean arc) {
        super(arc ? GCBlockEntities.ELECTRIC_ARC_FURNACE.get() : GCBlockEntities.ELECTRIC_FURNACE.get(), pos, state, 4,
                arc ? 25_000.0F : 16_000.0F, arc ? 60.0F : 45.0F);
        this.arc = arc;
    }

    public static ElectricFurnaceBlockEntity furnace(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state, false);
    }

    public static ElectricFurnaceBlockEntity arcFurnace(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state, true);
    }

    public boolean isArc() {
        return this.arc;
    }

    @Override
    protected float energyPerTick() {
        return this.arc ? 60.0F : 45.0F;
    }

    @Override
    public int processTime() {
        return this.arc ? 130 / 2 / Math.max(1, poweredTier()) : 130;
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    public static boolean isSmeltable(Level level, ItemStack stack) {
        return level != null && level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem;
            case 1 -> isSmeltable(this.level, stack);
            default -> false;
        };
    }

    private Optional<ItemStack> result() {
        final ItemStack input = this.inventory.getStackInSlot(1);
        if (input.isEmpty() || this.level == null) {
            return Optional.empty();
        }
        return this.recipes.getRecipeFor(new SingleRecipeInput(input), this.level)
                .map(RecipeHolder::value)
                .map(recipe -> recipe.assemble(new SingleRecipeInput(input), this.level.registryAccess()))
                .filter(result -> !result.isEmpty())
                .map(result -> doubled(input, result) ? result.copyWithCount(result.getCount() * 2) : result);
    }

    /**
     * 1.12.2: the arc furnace doubles results whose name contains "ingot" (or quartz) from inputs whose name
     * contains "ore", "raw", "moon", "mars" or "shard". Here the same idea is expressed with tags: ingots or
     * quartz from ores or raw materials.
     */
    private boolean doubled(ItemStack input, ItemStack result) {
        return this.arc && (result.is(Tags.Items.INGOTS) || result.is(Items.QUARTZ))
                && (input.is(Tags.Items.ORES) || input.is(Tags.Items.RAW_MATERIALS));
    }

    @Override
    protected boolean canProcess() {
        return result().filter(this::outputFits).isPresent();
    }

    /** The arc furnace may split a doubled result over both output slots. */
    private boolean outputFits(ItemStack result) {
        return roomFor(2, result) + (this.arc ? roomFor(3, result) : 0) >= result.getCount();
    }

    private int roomFor(int slot, ItemStack result) {
        final ItemStack current = this.inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            return result.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(current, result) ? current.getMaxStackSize() - current.getCount() : 0;
    }

    @Override
    protected void process() {
        result().ifPresent(result -> {
            this.inventory.extractItem(1, 1, false);
            int remaining = result.getCount();
            for (int slot : this.arc ? new int[]{2, 3} : new int[]{2}) {
                final int put = Math.min(remaining, roomFor(slot, result));
                if (put > 0) {
                    output(slot, result.copyWithCount(put));
                    remaining -= put;
                }
            }
        });
    }

    /** 1.12.2: without power a running smelt slowly loses progress (1 in 4 ticks) instead of restarting. */
    @Override
    protected void onUnpowered() {
        if (this.progress > 0 && this.level != null && this.level.random.nextInt(4) == 0) {
            this.progress--;
            setChanged();
        }
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return this.arc ? new int[]{0, 1, 2, 3} : new int[]{0, 1, 2};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return slot <= 1;
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return slot >= 2;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(id, playerInventory, this);
    }
}
