package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.12.2 ContainerCircuitFabricator (192 px tall): battery (6, 69), diamond (15, 17), silicon (74, 46) and
 * (74, 64), redstone (122, 46), recipe item (145, 20), output (152, 86).
 */
public class CircuitFabricatorMenu extends ProcessingMenu {
    public CircuitFabricatorMenu(int id, Inventory playerInventory, CircuitFabricatorBlockEntity machine) {
        super(GCMenus.CIRCUIT_FABRICATOR.get(), id, playerInventory, machine);
    }

    public CircuitFabricatorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.CIRCUIT_FABRICATOR.get(), id, playerInventory, buf, 7);
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 6, 69);
        input(1, 15, 17);
        input(2, 74, 46);
        input(3, 74, 64);
        input(4, 122, 46);
        input(5, 145, 20);
        addOutputSlot(CircuitFabricatorBlockEntity.OUTPUT, 152, 86);
    }

    private void input(int slot, int x, int y) {
        addMachineSlot(slot, x, y, stack -> CircuitFabricatorBlockEntity.isValidInput(this.level, slot, stack));
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 110};
    }
}
