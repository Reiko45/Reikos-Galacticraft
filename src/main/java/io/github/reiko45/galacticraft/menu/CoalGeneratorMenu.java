package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** 1.12.2 ContainerCoalGenerator: fuel slot at (33, 34). */
public class CoalGeneratorMenu extends MachineMenu {
    public static final int HEAT_X10 = EXTRA;
    public static final int BURN_TIME = EXTRA + 1;

    public CoalGeneratorMenu(int id, Inventory playerInventory, CoalGeneratorBlockEntity machine) {
        super(GCMenus.COAL_GENERATOR.get(), id, playerInventory, machine);
    }

    public CoalGeneratorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(GCMenus.COAL_GENERATOR.get(), id, playerInventory, buf, 1, EXTRA + 2);
    }

    @Override
    protected void addMachineSlots() {
        addMachineSlot(0, 33, 34);
    }

    public float heat() {
        return value(HEAT_X10) / 10.0F;
    }
}
