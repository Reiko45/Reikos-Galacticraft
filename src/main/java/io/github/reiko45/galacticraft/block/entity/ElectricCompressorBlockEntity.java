package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.CompressorGrid;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.ElectricCompressorMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electric Compressor and Advanced Compressor (1.12.2 TileEntityElectricIngotCompressor): 75 gJ/t, process time
 * base / (1 + 2 * powered tier) with base 200 (advanced 120), i.e. 66/40 ticks (advanced 40/24). Each cycle fills
 * both output slots, emptier first, so it can make two crafts at once.
 * Slots: 0 battery, 1-2 outputs, 3-11 grid.
 */
public class ElectricCompressorBlockEntity extends ProcessingMachineBlockEntity {
    public static final int GRID_START = 3;

    private final boolean advanced;
    private final CompressorGrid grid = new CompressorGrid(this.inventory, GRID_START);

    public ElectricCompressorBlockEntity(BlockPos pos, BlockState state, boolean advanced) {
        super(advanced ? GCBlockEntities.ADVANCED_COMPRESSOR.get() : GCBlockEntities.ELECTRIC_COMPRESSOR.get(), pos, state, 12,
                16_000.0F, 75.0F);
        this.advanced = advanced;
    }

    public static ElectricCompressorBlockEntity normal(BlockPos pos, BlockState state) {
        return new ElectricCompressorBlockEntity(pos, state, false);
    }

    public static ElectricCompressorBlockEntity advanced(BlockPos pos, BlockState state) {
        return new ElectricCompressorBlockEntity(pos, state, true);
    }

    @Override
    protected float energyPerTick() {
        return 75.0F;
    }

    @Override
    public int processTime() {
        return (this.advanced ? 120 : 200) / (1 + 2 * Math.max(1, poweredTier()));
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 ? stack.getItem() instanceof ElectricItem : this.grid.contains(slot);
    }

    @Override
    protected boolean canProcess() {
        final Optional<ItemStack> result = this.grid.result(this.level);
        return result.isPresent() && (fits(1, result.get()) || fits(2, result.get()));
    }

    @Override
    protected void process() {
        final int first = this.inventory.getStackInSlot(1).getCount() <= this.inventory.getStackInSlot(2).getCount() ? 1 : 2;
        for (int slot : new int[]{first, 3 - first}) {
            final Optional<ItemStack> result = this.grid.result(this.level);
            if (result.isPresent() && fits(slot, result.get())) {
                output(slot, result.get());
                this.grid.consume();
            }
        }
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F,
                    this.level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return side == RelativeSide.BOTTOM ? new int[]{1, 2} : new int[]{0, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return slot == 0 ? stack.getItem() instanceof ElectricItem : this.grid.canAutomationInsert(this.level, slot, stack);
    }

    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return slot == 1 || slot == 2;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ElectricCompressorMenu(id, playerInventory, this, this.advanced);
    }
}
