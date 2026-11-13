/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.inventory.ContainerLaserTurret).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.registry.GCMenus;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * 1.12.2 ContainerLaserTurret: the battery slot and the player's inventory. The menu carries the disabled and redstone
 * flags and the Enable/Disable cooldown; the turret's lists and settings come with its block updates.
 */
public class LaserTurretMenu extends MachineMenu {
    public static final int FLAGS = EXTRA;
    public static final int COOLDOWN = EXTRA + 1;
    public static final int DATA_COUNT = EXTRA + 2;
    public static final int BUTTON_ENABLE = 0;

    @Nullable
    private final LaserTurretBlockEntity turret;
    private final BlockPos pos;

    public LaserTurretMenu(int id, Inventory playerInventory, LaserTurretBlockEntity turret) {
        super(GCMenus.LASER_TURRET.get(), id, playerInventory, turret);
        this.turret = turret;
        this.pos = turret.getBlockPos();
    }

    /** Client: the position comes twice, once for the menu base and once for the screens. */
    public LaserTurretMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.LASER_TURRET.get(), id, playerInventory, buf, 1, DATA_COUNT);
        this.turret = null;
        this.pos = buf.readBlockPos();
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 82, 103);
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 137};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.turret == null || !this.turret.isOwner(player) || id != BUTTON_ENABLE) {
            return false;
        }
        this.turret.toggleDisabled();
        return true;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public boolean isDisabled() {
        return (value(FLAGS) & 1) != 0;
    }

    public boolean redstone() {
        return (value(FLAGS) & 2) != 0;
    }

    public int cooldown() {
        return value(COOLDOWN);
    }

    public MachineStatus status() {
        return LaserTurretBlockEntity.status(isDisabled(), redstone(), energy());
    }
}
