package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 1.12.2 ContainerFuelLoader: battery (51, 55), fuel container (7, 12), inventory from (8, 89). */
public class FuelLoaderMenu extends MachineMenu {
    public static final int FUEL = EXTRA;
    public static final int STATUS = EXTRA + 1;
    public static final int DISABLED = EXTRA + 2;
    public static final int COOLDOWN = EXTRA + 3;
    public static final int DATA_COUNT = EXTRA + 4;
    public static final int BUTTON_TOGGLE = 0;

    @Nullable
    private final FuelLoaderBlockEntity loader;

    public FuelLoaderMenu(int id, Inventory playerInventory, FuelLoaderBlockEntity loader) {
        super(GCMenus.FUEL_LOADER.get(), id, playerInventory, loader);
        this.loader = loader;
    }

    public FuelLoaderMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.FUEL_LOADER.get(), id, playerInventory, buf, 2, DATA_COUNT);
        this.loader = null;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 51, 55);
        addMachineSlot(1, 7, 12, FuelLoaderBlockEntity::isFuelSource);
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 89};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && this.loader != null) {
            this.loader.toggleDisabled();
            return true;
        }
        return false;
    }

    public int fuel() {
        return value(FUEL);
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
