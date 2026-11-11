package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.EnergyStorageBlock;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.EnergyStorageMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Energy Storage Module (tier 1: 500,000 gJ, 300 gJ/t out) and Energy Storage Cluster (tier 2: 2,500,000 gJ,
 * 1,800 gJ/t out), from 1.12.2 TileEntityEnergyStorageModule. Input on the right face, output on the left face
 * (as seen from the front). Slot 0 charges a battery, slot 1 drains one into the module.
 */
public class EnergyStorageBlockEntity extends MachineBlockEntity {
    public static final float MODULE_CAPACITY = 500_000.0F;
    public static final float MODULE_OUTPUT = 300.0F;
    public static final float CLUSTER_CAPACITY = 2_500_000.0F;
    public static final float CLUSTER_OUTPUT = 1_800.0F;

    private final boolean cluster;
    private float lastEnergy;

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state, boolean cluster) {
        super(cluster ? GCBlockEntities.ENERGY_STORAGE_CLUSTER.get() : GCBlockEntities.ENERGY_STORAGE_MODULE.get(), pos, state, 2,
                cluster ? CLUSTER_CAPACITY : MODULE_CAPACITY,
                // 1.12.2 EnergyStorage#setMaxExtract also sets maxReceive to 2.5x.
                2.5F * (cluster ? CLUSTER_OUTPUT : MODULE_OUTPUT),
                cluster ? CLUSTER_OUTPUT : MODULE_OUTPUT);
        this.cluster = cluster;
        this.energy.setTier(cluster ? 2 : 1);
    }

    public static EnergyStorageBlockEntity module(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state, false);
    }

    public static EnergyStorageBlockEntity cluster(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state, true);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ElectricItem;
    }

    @Override
    protected void tick() {
        final float energy = this.energy.getStoredGJ();
        // 1.12.2: a tier-1 module drained at full output loses an extra 25 gJ/t.
        if (!this.cluster && this.lastEnergy - energy > MODULE_OUTPUT - 1) {
            this.energy.consume(Math.min(25.0F, this.energy.getStoredGJ()));
        }
        this.lastEnergy = this.energy.getStoredGJ();
        chargeBattery(0);
        dischargeBattery(1);
        pushEnergy();

        final int level = scaledLevel();
        final BlockState state = getBlockState();
        if (state.hasProperty(EnergyStorageBlock.LEVEL) && state.getValue(EnergyStorageBlock.LEVEL) != level && this.level != null) {
            this.level.setBlock(this.worldPosition, state.setValue(EnergyStorageBlock.LEVEL, level), 3);
        }
    }

    /** 1.12.2 scaledEnergyLevel: floor((E + 49) * 16 / capacity). */
    public int scaledLevel() {
        return Math.min(16, (int) Math.floor((this.energy.getStoredGJ() + 49.0F) * 16.0F / this.energy.getCapacityGJ()));
    }

    public float maxOutput() {
        return this.cluster ? CLUSTER_OUTPUT : MODULE_OUTPUT;
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addInt(() -> (int) maxOutput());
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return switch (side) {
            case LEFT -> EnergyAccess.OUTPUT;
            case RIGHT -> EnergyAccess.INPUT;
            default -> EnergyAccess.NONE;
        };
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new EnergyStorageMenu(id, playerInventory, this);
    }
}
