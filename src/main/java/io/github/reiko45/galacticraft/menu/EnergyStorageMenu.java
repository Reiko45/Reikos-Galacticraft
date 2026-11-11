package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 ContainerEnergyStorageModule: charge slot (33, 24), discharge slot (33, 48). */
public class EnergyStorageMenu extends MachineMenu {
    public static final int MAX_OUTPUT = EXTRA;

    public EnergyStorageMenu(int id, Inventory playerInventory, EnergyStorageBlockEntity machine) {
        super(GCMenus.ENERGY_STORAGE.get(), id, playerInventory, machine);
    }

    public EnergyStorageMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.ENERGY_STORAGE.get(), id, playerInventory, buf, 2, EXTRA + 2);
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new net.neoforged.neoforge.items.SlotItemHandler(this.machineInventory, 0, 33, 24) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new net.neoforged.neoforge.items.SlotItemHandler(this.machineInventory, 1, 33, 48) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    public float maxOutput() {
        return intValue(MAX_OUTPUT);
    }
}
