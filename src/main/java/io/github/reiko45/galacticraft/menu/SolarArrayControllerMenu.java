/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.inventory.ContainerSolarArrayController).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.SolarArrayControllerBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 1.12.2 ContainerSolarArrayController (209 px tall): battery slot (152, 91), player inventory from y = 127. */
public class SolarArrayControllerMenu extends MachineMenu {
    public static final int GENERATING = EXTRA;
    public static final int STRENGTH = EXTRA + 1;
    public static final int POSSIBLE = EXTRA + 2;
    public static final int DISABLED = EXTRA + 3;
    public static final int COOLDOWN = EXTRA + 4;
    public static final int BOOST_PERMILLE = EXTRA + 5;
    public static final int BUTTON_TOGGLE = 0;

    @Nullable
    private final SolarArrayControllerBlockEntity controller;

    public SolarArrayControllerMenu(int id, Inventory playerInventory, SolarArrayControllerBlockEntity controller) {
        super(GCMenus.SOLAR_ARRAY_CONTROLLER.get(), id, playerInventory, controller);
        this.controller = controller;
    }

    public SolarArrayControllerMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.SOLAR_ARRAY_CONTROLLER.get(), id, playerInventory, buf, 1, EXTRA + 6);
        this.controller = null;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 152, 91);
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 127};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && this.controller != null) {
            this.controller.toggleDisabled();
            return true;
        }
        return false;
    }

    public int generating() {
        return value(GENERATING);
    }

    public int strength() {
        return value(STRENGTH);
    }

    public int possible() {
        return value(POSSIBLE);
    }

    public boolean isDisabled() {
        return value(DISABLED) != 0;
    }

    /** (boost - 1) in tenths of a percent. */
    public int boostPermille() {
        return value(BOOST_PERMILLE);
    }

    public boolean coolingDown() {
        return value(COOLDOWN) != 0;
    }
}
