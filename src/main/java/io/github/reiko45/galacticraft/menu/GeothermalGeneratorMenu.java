/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.inventory.ContainerGeothermal). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.GeothermalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 1.12.2 ContainerGeothermal (201 px tall): battery slot (152, 83), player inventory from y = 119. */
public class GeothermalGeneratorMenu extends MachineMenu {
    public static final int GENERATING = EXTRA;
    public static final int VALID_SPOUT = EXTRA + 1;
    public static final int DISABLED = EXTRA + 2;
    public static final int COOLDOWN = EXTRA + 3;
    public static final int BUTTON_TOGGLE = 0;

    @Nullable
    private final GeothermalGeneratorBlockEntity generator;

    public GeothermalGeneratorMenu(int id, Inventory playerInventory, GeothermalGeneratorBlockEntity generator) {
        super(GCMenus.GEOTHERMAL_GENERATOR.get(), id, playerInventory, generator);
        this.generator = generator;
    }

    public GeothermalGeneratorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.GEOTHERMAL_GENERATOR.get(), id, playerInventory, buf, 1, EXTRA + 4);
        this.generator = null;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 152, 83);
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 119};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && this.generator != null) {
            this.generator.toggleDisabled();
            return true;
        }
        return false;
    }

    public int generating() {
        return value(GENERATING);
    }

    public boolean hasValidSpout() {
        return value(VALID_SPOUT) != 0;
    }

    public boolean isDisabled() {
        return value(DISABLED) != 0;
    }

    public boolean coolingDown() {
        return value(COOLDOWN) != 0;
    }
}
