package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 1.12.2 ContainerRefinery (168 px tall): battery (38, 51), oil input (7, 7), fuel output (153, 7), inventory from y = 86. */
public class RefineryMenu extends ProcessingMenu {
    public static final int OIL = DATA_COUNT;
    public static final int FUEL = DATA_COUNT + 1;
    public static final int DISABLED = DATA_COUNT + 2;
    public static final int BUTTON_TOGGLE = 0;

    @Nullable
    private final RefineryBlockEntity refinery;

    public RefineryMenu(int id, Inventory playerInventory, RefineryBlockEntity refinery) {
        super(GCMenus.REFINERY.get(), id, playerInventory, refinery);
        this.refinery = refinery;
    }

    public RefineryMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.REFINERY.get(), id, playerInventory, buf, 3, DATA_COUNT + 3);
        this.refinery = null;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 38, 51);
        addMachineSlot(1, 7, 7, RefineryBlockEntity::isOilContainer);
        addMachineSlot(2, 153, 7, RefineryBlockEntity::isFuelContainer);
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 86};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && this.refinery != null) {
            this.refinery.toggleDisabled();
            return true;
        }
        return false;
    }

    public int oil() {
        return value(OIL);
    }

    public int fuel() {
        return value(FUEL);
    }

    public boolean isDisabled() {
        return value(DISABLED) != 0;
    }
}
