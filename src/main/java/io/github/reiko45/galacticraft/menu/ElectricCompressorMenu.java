package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * 1.12.2 ContainerElectricIngotCompressor (199 px tall): grid at (19 + 18c, 18 + 18r), battery (55, 75),
 * outputs (138, 30) and (138, 48), player inventory from y = 117.
 */
public class ElectricCompressorMenu extends ProcessingMenu {
    public ElectricCompressorMenu(int id, Inventory playerInventory, ElectricCompressorBlockEntity machine, boolean advanced) {
        super(type(advanced), id, playerInventory, machine);
    }

    private ElectricCompressorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf, boolean advanced) {
        super(type(advanced), id, playerInventory, buf, 12);
    }

    public static ElectricCompressorMenu normal(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ElectricCompressorMenu(id, playerInventory, buf, false);
    }

    public static ElectricCompressorMenu advanced(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ElectricCompressorMenu(id, playerInventory, buf, true);
    }

    private static MenuType<?> type(boolean advanced) {
        return advanced ? GCMenus.ADVANCED_COMPRESSOR.get() : GCMenus.ELECTRIC_COMPRESSOR.get();
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 55, 75);
        addOutputSlot(1, 138, 30);
        addOutputSlot(2, 138, 48);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addMachineSlot(ElectricCompressorBlockEntity.GRID_START + col + row * 3, 19 + col * 18, 18 + row * 18);
            }
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 117};
    }
}
