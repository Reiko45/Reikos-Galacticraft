package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.machine.CompressorGrid;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.CompressorMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fuel-burning Compressor (1.12.2 TileEntityIngotCompressor): any furnace fuel, 200 ticks per operation. Fuel
 * is only burnt while a valid recipe has room in the output; progress restarts when the fuel or recipe runs out.
 * Slots: 0 fuel, 1 output, 2-10 grid.
 */
public class CompressorBlockEntity extends MachineBlockEntity {
    public static final int PROCESS_TIME = 200;
    public static final int GRID_START = 2;

    private final CompressorGrid grid = new CompressorGrid(this.inventory, GRID_START);
    private int progress;
    private int burnTime;
    private int currentBurnTime;

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.COMPRESSOR.get(), pos, state, 11, 0.0F, 0.0F, 0.0F);
    }

    public static int burnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 ? burnTime(stack) > 0 : this.grid.contains(slot);
    }

    @Override
    protected void tick() {
        final boolean wasBurning = this.burnTime > 0;
        if (this.burnTime > 0) {
            this.burnTime--;
        }
        final Optional<ItemStack> result = this.grid.result(this.level);
        final boolean canWork = result.isPresent() && fits(result.get());
        if (canWork && this.burnTime <= 0) {
            final ItemStack fuel = this.inventory.getStackInSlot(0);
            final int time = burnTime(fuel);
            if (time > 0) {
                this.burnTime = this.currentBurnTime = time;
                final ItemStack remainder = fuel.getCraftingRemainingItem();
                this.inventory.extractItem(0, 1, false);
                if (this.inventory.getStackInSlot(0).isEmpty() && !remainder.isEmpty()) {
                    this.inventory.setStackInSlot(0, remainder);
                }
            }
        }
        if (canWork && this.burnTime > 0) {
            this.progress++;
            if (this.progress > PROCESS_TIME / 2 && this.progress % 40 == 0 && this.level != null) {
                this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F,
                        this.level.random.nextFloat() * 0.1F + 0.9F);
            }
            if (this.progress >= PROCESS_TIME) {
                this.progress = 0;
                output(result.get());
                this.grid.consume();
            }
        } else {
            this.progress = 0;
        }
        if (wasBurning || this.burnTime > 0 || this.progress > 0) {
            setChanged();
        }
    }

    private boolean fits(ItemStack result) {
        final ItemStack current = this.inventory.getStackInSlot(1);
        return current.isEmpty() || ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void output(ItemStack result) {
        final ItemStack current = this.inventory.getStackInSlot(1);
        if (current.isEmpty()) {
            this.inventory.setStackInSlot(1, result.copy());
        } else {
            this.inventory.setStackInSlot(1, current.copyWithCount(current.getCount() + result.getCount()));
        }
    }

    public boolean isWorking() {
        return this.progress > 0;
    }

    /** Synced: progress, burn time, current item burn time. */
    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> this.progress);
        data.addShort(() -> this.burnTime);
        data.addShort(() -> this.currentBurnTime);
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return EnergyAccess.NONE;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return side == RelativeSide.BOTTOM ? new int[]{1} : new int[]{0, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return slot == 0 ? burnTime(stack) > 0 : this.grid.canAutomationInsert(this.level, slot, stack);
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return slot == 1;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CompressorMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", this.progress);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("CurrentBurnTime", this.currentBurnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("Progress");
        this.burnTime = tag.getInt("BurnTime");
        this.currentBurnTime = tag.getInt("CurrentBurnTime");
    }
}
