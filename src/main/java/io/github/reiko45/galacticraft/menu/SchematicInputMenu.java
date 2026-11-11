package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.item.SchematicItem;
import io.github.reiko45.galacticraft.registry.GCMenus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * NASA Workbench "Add New Schematic" page (1.12.2 ContainerSchematic): one slot for a schematic and the Unlock
 * Schematic button. Positions are those of 1.12.2 moved down 27 pixels, the part of the texture above the 1.12.2 GUI.
 */
public class SchematicInputMenu extends SchematicPageMenu {
    private static final int OFFSET = 27;
    private final SimpleContainer input = new SimpleContainer(1);

    public SchematicInputMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public SchematicInputMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(GCMenus.SCHEMATIC_INPUT.get(), id, inventory, access, Schematics.ADD_SCHEMATIC);
        addSlot(new Slot(this.input, 0, 80, 1 + OFFSET) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SchematicItem;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 111 + row * 18 - 59 + 16 + OFFSET));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 169 - 59 + 16 + OFFSET));
        }
    }

    public ItemStack schematicStack() {
        return this.input.getItem(0);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access().execute((level, pos) -> clearContainer(player, this.input));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, this.slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
