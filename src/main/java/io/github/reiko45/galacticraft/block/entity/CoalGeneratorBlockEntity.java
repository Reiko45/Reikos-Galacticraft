package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.data.GCTags;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.CoalGeneratorMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coal Generator (1.12.2 TileEntityCoalGenerator). Burning fuel raises the "hull heat" by
 * max(heat * 0.5%, 0.3) gJ/t per tick up to 150; output is heat - 30 gJ/t, so it needs to warm up and keeps
 * producing for a while after the fuel runs out (heat falls by 0.3 per tick).
 */
public class CoalGeneratorBlockEntity extends MachineBlockEntity {
    public static final int MAX_GENERATE_GJ_PER_TICK = 150;
    public static final int MIN_GENERATE_GJ_PER_TICK = 30;
    private static final float BASE_ACCELERATION = 0.3F;

    private float heat;
    private int burnTime;

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.COAL_GENERATOR.get(), pos, state, 1,
                16000.0F, 0.0F, MAX_GENERATE_GJ_PER_TICK - MIN_GENERATE_GJ_PER_TICK);
    }

    /** Burn time in ticks (1.12.2: coal 320, charcoal 160, coal block 3200, charcoal block 1600, dusts 220/120). */
    public static int fuelTime(ItemStack stack) {
        if (stack.is(Items.COAL)) {
            return 320;
        }
        if (stack.is(Items.CHARCOAL)) {
            return 160;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 3200;
        }
        if (stack.is(GCTags.item("storage_blocks/charcoal"))) {
            return 1600;
        }
        if (stack.is(GCTags.item("dusts/coal"))) {
            return 220;
        }
        if (stack.is(GCTags.item("dusts/charcoal"))) {
            return 120;
        }
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return fuelTime(stack) > 0;
    }

    @Override
    protected void tick() {
        if (this.heat - MIN_GENERATE_GJ_PER_TICK > 0.0F) {
            this.energy.generate(this.heat - MIN_GENERATE_GJ_PER_TICK);
        }
        if (this.burnTime > 0) {
            this.burnTime--;
            this.heat = Math.min(this.heat + Math.max(this.heat * 0.005F, BASE_ACCELERATION), MAX_GENERATE_GJ_PER_TICK);
        }
        if (this.burnTime <= 0) {
            final ItemStack fuel = this.inventory.getStackInSlot(0);
            final int time = fuelTime(fuel);
            if (time > 0) {
                this.burnTime = time;
                this.inventory.extractItem(0, 1, false);
            }
        }
        if (this.burnTime <= 0) {
            this.heat = Math.max(this.heat - 0.3F, 0.0F);
        }
        pushEnergy();
        if (this.heat > 0.0F || this.burnTime > 0) {
            setChanged();
        }
    }

    public float heat() {
        return this.heat;
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> Math.round(this.heat * 10.0F));
        data.addShort(() -> this.burnTime);
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.LEFT ? EnergyAccess.OUTPUT : EnergyAccess.NONE;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0};
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CoalGeneratorMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Heat", this.heat);
        tag.putInt("BurnTime", this.burnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.heat = tag.getFloat("Heat");
        this.burnTime = tag.getInt("BurnTime");
    }
}
