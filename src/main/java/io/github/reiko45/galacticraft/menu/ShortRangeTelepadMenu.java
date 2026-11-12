/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.inventory.ContainerShortRangeTelepad
 * and the S_UPDATE_ADVANCED_GUI / S_UPDATE_DISABLEABLE_BUTTON packets). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Short Range Telepad menu (209 px tall): the battery at (152, 105), the player's inventory from y 127. Anyone may
 * switch it on and off; only the owner may set the addresses (packed into button ids).
 */
public class ShortRangeTelepadMenu extends MachineMenu {
    public static final int ADDRESS = EXTRA;
    public static final int TARGET = EXTRA + 2;
    public static final int FLAGS = EXTRA + 4;
    public static final int RESULT = EXTRA + 5;
    public static final int TELEPORT_TIME = EXTRA + 6;
    public static final int COOLDOWN = EXTRA + 7;
    public static final int DATA_COUNT = EXTRA + 8;

    public static final int BUTTON_ENABLE = 0;
    /** A base + (address + 1), for -1 (unset) up to 999999. */
    public static final int SET_ADDRESS = 10_000_000;
    public static final int SET_TARGET = 20_000_000;

    @Nullable
    private final ShortRangeTelepadBlockEntity telepad;
    private final String ownerName;
    private final boolean owner;

    public ShortRangeTelepadMenu(int id, Inventory playerInventory, ShortRangeTelepadBlockEntity telepad) {
        super(GCMenus.SHORT_RANGE_TELEPAD.get(), id, playerInventory, telepad);
        this.telepad = telepad;
        this.ownerName = telepad.ownerName();
        this.owner = telepad.isOwner(playerInventory.player);
    }

    public ShortRangeTelepadMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.SHORT_RANGE_TELEPAD.get(), id, playerInventory, buf, 1, DATA_COUNT);
        this.telepad = null;
        this.ownerName = buf.readUtf();
        this.owner = buf.readBoolean();
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 152, 105);
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 127};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.telepad == null) {
            return false;
        }
        if (id == BUTTON_ENABLE) {
            this.telepad.toggleDisabled();
            return true;
        }
        if (!this.telepad.isOwner(player)) {
            return false;
        }
        if (id >= SET_TARGET) {
            this.telepad.setTargetAddress(Math.min(ShortRangeTelepadBlockEntity.MAX_ADDRESS, id - SET_TARGET - 1));
        } else if (id >= SET_ADDRESS) {
            this.telepad.setAddress(Math.min(ShortRangeTelepadBlockEntity.MAX_ADDRESS, id - SET_ADDRESS - 1));
        } else {
            return false;
        }
        return true;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public boolean isOwner() {
        return this.owner;
    }

    public int address() {
        return intValue(ADDRESS);
    }

    public int targetAddress() {
        return intValue(TARGET);
    }

    public boolean addressValid() {
        return (value(FLAGS) & 1) != 0;
    }

    public boolean isDisabled() {
        return (value(FLAGS) & 2) != 0;
    }

    public ShortRangeTelepadBlockEntity.SearchResult targetResult() {
        final ShortRangeTelepadBlockEntity.SearchResult[] values = ShortRangeTelepadBlockEntity.SearchResult.values();
        return values[Math.max(0, Math.min(values.length - 1, value(RESULT)))];
    }

    public boolean canToggle() {
        return value(COOLDOWN) == 0;
    }
}
