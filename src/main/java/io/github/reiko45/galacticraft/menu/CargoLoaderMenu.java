/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.inventory.ContainerCargoLoader and
 * ContainerCargoUnloader). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.CargoMachineBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Cargo Loader / Unloader menu: battery at (10, 27), two rows of seven item slots, the player's inventory from y 124. */
public class CargoLoaderMenu extends MachineMenu {
    public static final int STATUS = EXTRA;
    public static final int DISABLED = EXTRA + 1;
    public static final int COOLDOWN = EXTRA + 2;
    public static final int LOCKED = EXTRA + 3;
    public static final int DATA_COUNT = EXTRA + 4;
    public static final int BUTTON_TOGGLE = 0;

    private final boolean loads;
    @Nullable
    private final CargoMachineBlockEntity machine;

    public CargoLoaderMenu(boolean loads, int id, Inventory playerInventory, CargoMachineBlockEntity machine) {
        super(loads ? GCMenus.CARGO_LOADER.get() : GCMenus.CARGO_UNLOADER.get(), id, playerInventory, machine);
        this.loads = loads;
        this.machine = machine;
    }

    public CargoLoaderMenu(boolean loads, int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(loads ? GCMenus.CARGO_LOADER.get() : GCMenus.CARGO_UNLOADER.get(), id, playerInventory, buf, CargoMachineBlockEntity.SLOTS, DATA_COUNT);
        this.loads = loads;
        this.machine = null;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 10, 27);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 7; col++) {
                addMachineSlot(1 + col + row * 7, 38 + col * 18, 27 + row * 18);
            }
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 124};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && this.machine != null) {
            this.machine.toggleDisabled();
            return true;
        }
        return false;
    }

    /** 1.12.2 ContainerCargoLoader#slotClick: a locked loader's item slots can't be clicked. */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (isLocked() && slotId > 0 && slotId < CargoMachineBlockEntity.SLOTS) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** 1.12.2 transferStackInSlot: nor shift-clicked out. */
    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        if (isLocked() && index > 0 && index < CargoMachineBlockEntity.SLOTS) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    public boolean isLocked() {
        return this.machine != null ? this.machine.isLocked() : value(LOCKED) != 0;
    }

    public boolean loads() {
        return this.loads;
    }

    public MachineStatus status() {
        return MachineStatus.byOrdinal(value(STATUS));
    }

    public boolean isDisabled() {
        return value(DISABLED) != 0;
    }

    public boolean canToggle() {
        return value(COOLDOWN) == 0;
    }
}
