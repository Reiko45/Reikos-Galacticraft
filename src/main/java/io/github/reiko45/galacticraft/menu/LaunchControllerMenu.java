/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.inventory.ContainerLaunchController and
 * the S_UPDATE_ADVANCED_GUI / S_UPDATE_DISABLEABLE_BUTTON packets). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Launch Controller menu (209 px tall): the battery at (152, 105), the player's inventory from y 127. Settings change
 * through button ids: toggles, an auto-launch choice, or a frequency packed into the id. Only the owner may change them.
 */
public class LaunchControllerMenu extends MachineMenu {
    public static final int FREQUENCY = EXTRA;
    public static final int DEST_FREQUENCY = EXTRA + 2;
    public static final int FLAGS = EXTRA + 4;
    public static final int SELECTION = EXTRA + 5;
    public static final int COOLDOWN = EXTRA + 6;
    public static final int STATUS = EXTRA + 7;
    public static final int DATA_COUNT = EXTRA + 8;

    public static final int BUTTON_ENABLE = 0;
    public static final int BUTTON_PAD_REMOVAL = 1;
    public static final int BUTTON_SCHEDULING = 2;
    public static final int BUTTON_HIDE_DESTINATION = 3;
    /** 10 + the auto-launch choice. */
    public static final int BUTTON_SELECTION = 10;
    /** A base + (frequency + 1), for -1 (unset) up to 999999. */
    public static final int SET_FREQUENCY = 10_000_000;
    public static final int SET_DEST_FREQUENCY = 20_000_000;

    @Nullable
    private final LaunchControllerBlockEntity controller;
    private final String ownerName;
    private final boolean owner;

    public LaunchControllerMenu(int id, Inventory playerInventory, LaunchControllerBlockEntity controller) {
        super(GCMenus.LAUNCH_CONTROLLER.get(), id, playerInventory, controller);
        this.controller = controller;
        this.ownerName = controller.ownerName();
        this.owner = controller.isOwner(playerInventory.player);
    }

    /** Client: after the position, {@code buf} carries the owner's name and whether this player is the owner. */
    public LaunchControllerMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.LAUNCH_CONTROLLER.get(), id, playerInventory, buf, 1, DATA_COUNT);
        this.controller = null;
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
        if (this.controller == null || !this.controller.isOwner(player)) {
            return false;
        }
        if (id >= SET_DEST_FREQUENCY) {
            this.controller.setDestFrequency(id - SET_DEST_FREQUENCY - 1);
        } else if (id >= SET_FREQUENCY) {
            this.controller.setFrequency(id - SET_FREQUENCY - 1);
        } else if (id >= BUTTON_SELECTION && id < BUTTON_SELECTION + LaunchControllerBlockEntity.AutoLaunch.values().length) {
            this.controller.setLaunchSelection(LaunchControllerBlockEntity.AutoLaunch.values()[id - BUTTON_SELECTION]);
        } else {
            switch (id) {
                case BUTTON_ENABLE -> this.controller.toggleDisabled();
                case BUTTON_PAD_REMOVAL -> this.controller.togglePadRemoval();
                case BUTTON_SCHEDULING -> this.controller.setLaunchSchedulingEnabled(!scheduling());
                case BUTTON_HIDE_DESTINATION -> this.controller.toggleHideDestination();
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public boolean isOwner() {
        return this.owner;
    }

    public int frequency() {
        return intValue(FREQUENCY);
    }

    public int destFrequency() {
        return intValue(DEST_FREQUENCY);
    }

    private boolean flag(int bit) {
        return (value(FLAGS) & bit) != 0;
    }

    public boolean frequencyValid() {
        return flag(1);
    }

    public boolean destFrequencyValid() {
        return flag(2);
    }

    public boolean isDisabled() {
        return flag(4);
    }

    public boolean scheduling() {
        return flag(8);
    }

    public boolean padRemovalDisabled() {
        return flag(16);
    }

    public boolean hideDestination() {
        return flag(32);
    }

    public LaunchControllerBlockEntity.AutoLaunch selection() {
        final LaunchControllerBlockEntity.AutoLaunch[] values = LaunchControllerBlockEntity.AutoLaunch.values();
        return values[Math.max(0, Math.min(values.length - 1, value(SELECTION)))];
    }

    public boolean canToggle() {
        return value(COOLDOWN) == 0;
    }

    public MachineStatus status() {
        return MachineStatus.byOrdinal(value(STATUS));
    }
}
