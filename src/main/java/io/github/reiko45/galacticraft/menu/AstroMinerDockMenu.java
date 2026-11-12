/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.inventory.ContainerAstroMinerDock).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Astro Miner Dock (1.12.2 ContainerAstroMinerDock, 256 x 221): the battery at (230, 108), the 72-slot store in 12
 * columns from (8, 18), the player's inventory from y 139, and the Recall button (button 0).
 */
public class AstroMinerDockMenu extends MachineMenu {
    public static final int STATE = EXTRA;
    public static final int DX = EXTRA + 1;
    public static final int DY = EXTRA + 2;
    public static final int DZ = EXTRA + 3;
    public static final int COUNT = EXTRA + 4;
    public static final int LINKED = EXTRA + 6;
    public static final int DATA_COUNT = EXTRA + 7;
    public static final int BUTTON_RECALL = 0;

    @Nullable
    private final MinerBaseBlockEntity base;

    public AstroMinerDockMenu(int id, Inventory playerInventory, MinerBaseBlockEntity base) {
        super(GCMenus.ASTRO_MINER_DOCK.get(), id, playerInventory, base);
        this.base = base;
    }

    public AstroMinerDockMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.ASTRO_MINER_DOCK.get(), id, playerInventory, buf, MinerBaseBlockEntity.HOLD_SIZE + 1, DATA_COUNT);
        this.base = null;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 230, 108);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 12; j++) {
                addMachineSlot(j + i * 12 + 1, 8 + j * 18, 18 + i * 18);
            }
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 139};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_RECALL && this.base != null) {
            this.base.recall();
            return true;
        }
        return false;
    }

    /** The miner's state for the status line (-3 for none). */
    public int minerState() {
        return (short) value(STATE);
    }

    public int delta(int axis) {
        return (short) value(DX + axis);
    }

    public int mined() {
        return intValue(COUNT);
    }

    public boolean linked() {
        return value(LINKED) != 0;
    }
}
