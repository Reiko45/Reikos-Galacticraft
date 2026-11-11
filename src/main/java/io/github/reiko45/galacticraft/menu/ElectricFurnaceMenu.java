package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.ElectricFurnaceBlockEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/** 1.12.2 ContainerElectricFurnace: battery (8,49), input (56,25), output (109,25), arc second output (127,25). */
public class ElectricFurnaceMenu extends ProcessingMenu {
    private final boolean arc;

    public ElectricFurnaceMenu(int id, Inventory playerInventory, ElectricFurnaceBlockEntity machine) {
        super(type(machine.isArc()), id, playerInventory, machine);
        this.arc = machine.isArc();
    }

    private ElectricFurnaceMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf, boolean arc) {
        super(type(arc), id, playerInventory, buf, 4);
        this.arc = arc;
    }

    public static ElectricFurnaceMenu furnace(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ElectricFurnaceMenu(id, playerInventory, buf, false);
    }

    public static ElectricFurnaceMenu arcFurnace(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ElectricFurnaceMenu(id, playerInventory, buf, true);
    }

    private static MenuType<?> type(boolean arc) {
        return arc ? GCMenus.ELECTRIC_ARC_FURNACE.get() : GCMenus.ELECTRIC_FURNACE.get();
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(0, 8, 49);
        addMachineSlot(1, 56, 25, stack -> ElectricFurnaceBlockEntity.isSmeltable(this.level, stack));
        addOutputSlot(2, 109, 25);
        // The tier-1 furnace has the 4th inventory slot but no GUI slot for it, like 1.12.2.
        if (this.machineInventory.getSlots() > 3 && isArcMenu()) {
            addOutputSlot(3, 127, 25);
        }
    }

    /** Called during super construction, before {@link #arc} is assigned. */
    private boolean isArcMenu() {
        return getType() == GCMenus.ELECTRIC_ARC_FURNACE.get();
    }

    public boolean isArc() {
        return this.arc;
    }
}
