package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.CircuitFabricatorMenu;
import io.github.reiko45.galacticraft.recipe.CircuitFabricatorRecipe;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Circuit Fabricator (1.12.2 TileEntityCircuitFabricator): 20 gJ/t, 300 ticks on tier-1 power, 200 on tier 2.
 * Slots: 0 battery, 1 diamond, 2-3 silicon, 4 redstone, 5 recipe-specific item, 6 output.
 */
public class CircuitFabricatorBlockEntity extends ProcessingMachineBlockEntity {
    public static final int OUTPUT = 6;

    public CircuitFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.CIRCUIT_FABRICATOR.get(), pos, state, 7, 16_000.0F, 20.0F);
    }

    @Override
    protected float energyPerTick() {
        return 20.0F;
    }

    @Override
    public int processTime() {
        return 300 * 2 / (1 + Math.max(1, poweredTier()));
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    /** 1.12.2 CircuitFabricatorRecipes.slotValidItems: whatever some recipe accepts in that position. */
    public static boolean isValidInput(Level level, int slot, ItemStack stack) {
        if (level == null || slot < 1 || slot > 5) {
            return false;
        }
        for (RecipeHolder<CircuitFabricatorRecipe> holder : level.getRecipeManager().getAllRecipesFor(GCRecipes.CIRCUIT_FABRICATING.get())) {
            if (holder.value().getIngredients().get(slot - 1).test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == 0) {
            return stack.getItem() instanceof ElectricItem;
        }
        return slot < OUTPUT && isValidInput(this.level, slot, stack);
    }

    private CircuitFabricatorRecipe.Input input() {
        final List<ItemStack> items = new ArrayList<>(5);
        for (int slot = 1; slot <= 5; slot++) {
            items.add(this.inventory.getStackInSlot(slot));
        }
        return new CircuitFabricatorRecipe.Input(items);
    }

    private Optional<ItemStack> result() {
        if (this.level == null) {
            return Optional.empty();
        }
        final CircuitFabricatorRecipe.Input input = input();
        return this.level.getRecipeManager().getRecipeFor(GCRecipes.CIRCUIT_FABRICATING.get(), input, this.level)
                .map(holder -> zeroGravityBonus(this.level, holder.value().assemble(input, this.level.registryAccess())));
    }

    /**
     * 1.12.2 TileEntityCircuitFabricator#smeltItem in a zero-g world (Earth orbit): basic wafers come out 5 at a time,
     * solar wafers 15, and the other wafers and the frequency module double.
     */
    public static ItemStack zeroGravityBonus(Level level, ItemStack result) {
        if (!level.dimension().equals(io.github.reiko45.galacticraft.world.GCDimensions.ORBIT)) {
            return result;
        }
        if (result.is(io.github.reiko45.galacticraft.registry.GCItems.BASIC_WAFER.get())) {
            result.setCount(5);
        } else if (result.is(io.github.reiko45.galacticraft.registry.GCItems.SOLAR_WAFER.get())) {
            result.setCount(15);
        } else if (result.is(io.github.reiko45.galacticraft.registry.GCItems.ADVANCED_WAFER.get())
                || result.is(io.github.reiko45.galacticraft.registry.GCItems.FREQUENCY_MODULE.get())) {
            result.setCount(result.getCount() * 2);
        }
        return result;
    }

    @Override
    protected boolean canProcess() {
        return result().filter(result -> fits(OUTPUT, result)).isPresent();
    }

    @Override
    protected void process() {
        result().ifPresent(result -> {
            for (int slot = 1; slot <= 5; slot++) {
                this.inventory.extractItem(slot, 1, false);
            }
            output(OUTPUT, result);
        });
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return side == RelativeSide.BOTTOM ? new int[]{OUTPUT} : new int[]{0, 1, 2, 3, 4, 5};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        // 1.12.2 balanced the two silicon slots by exposing only the emptier one.
        final int silicon2 = this.inventory.getStackInSlot(2).getCount();
        final int silicon3 = this.inventory.getStackInSlot(3).getCount();
        if (slot == 2 && silicon2 > silicon3 || slot == 3 && silicon3 >= silicon2) {
            return false;
        }
        return isItemValid(slot, stack);
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return slot == OUTPUT;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CircuitFabricatorMenu(id, playerInventory, this);
    }
}
