package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.SolarPanelBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 1.12.2 ContainerSolar (201 px tall): battery slot (152, 83), player inventory from y = 119. */
public class SolarPanelMenu extends MachineMenu {
    public static final int GENERATING = EXTRA;
    public static final int STRENGTH = EXTRA + 1;
    public static final int DISABLED = EXTRA + 2;
    public static final int BUTTON_TOGGLE = 0;

    @Nullable
    private final SolarPanelBlockEntity panel;

    public SolarPanelMenu(int id, Inventory playerInventory, SolarPanelBlockEntity panel) {
        super(GCMenus.SOLAR_PANEL.get(), id, playerInventory, panel);
        this.panel = panel;
    }

    public SolarPanelMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.SOLAR_PANEL.get(), id, playerInventory, buf, 1, EXTRA + 3);
        this.panel = null;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 152, 83);
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 119};
    }

    /** The Enable/Disable button, sent with vanilla's container button packet. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && this.panel != null) {
            this.panel.toggleDisabled();
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

    public boolean isDisabled() {
        return value(DISABLED) != 0;
    }
}
