/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.tile.TileEntityCrashedProbe). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.CrashedProbeMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * Crashed Probe (1.12.2 TileEntityCrashedProbe): six slots of salvage, filled from {@link #LOOT} the first time anyone
 * looks in. A probe found in the world still holds its Radioisotope Core, which drops when it is broken. Hoppers can't
 * reach its slots.
 */
public class CrashedProbeBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    /** 1.12.2 LootHandlerGC.TABLE_CRASHED_PROBE. */
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, GalacticraftCore.id("chests/crashed_probe"));
    private static final int[] NO_SLOTS = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private boolean hasCoreToDrop;

    public CrashedProbeBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.CRASHED_PROBE.get(), pos, state);
    }

    public boolean hasCoreToDrop() {
        return this.hasCoreToDrop;
    }

    /** 1.12.2 setDropCore: a probe that crashed here (not one placed by a player) has its core. */
    public void setDropCore() {
        this.hasCoreToDrop = true;
        setChanged();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 6;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.galacticraftcore.crashed_probe");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return new CrashedProbeMenu(id, playerInventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.hasCoreToDrop = tag.getBoolean("ctd");
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items, registries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ctd", this.hasCoreToDrop);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items, registries);
        }
    }
}
