package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.item.ElectricItem;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Base menu for machines (1.12.2 Container* classes). Server side it wraps the block entity's inventory and
 * {@link SyncedData}; client side it uses a local copy that vanilla keeps in sync.
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    public static final int ENERGY = 0;
    public static final int CAPACITY = 2;
    /** First index for machine-specific values. */
    public static final int EXTRA = 4;

    protected final ContainerData data;
    protected final IItemHandler machineInventory;
    private final ContainerLevelAccess access;
    private final Block block;
    private final int machineSlots;

    protected MachineMenu(MenuType<?> type, int id, Inventory playerInventory, MachineBlockEntity machine) {
        this(type, id, playerInventory, machine.inventory(), machine.syncedData(),
                ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()), machine.getBlockState().getBlock());
    }

    /** Client constructor: {@code buf} carries the block position written by {@code openMenu(provider, pos)}. */
    protected MachineMenu(MenuType<?> type, int id, Inventory playerInventory, RegistryFriendlyByteBuf buf, int slots, int dataCount) {
        this(type, id, playerInventory, new ItemStackHandler(slots), SyncedData.client(dataCount), ContainerLevelAccess.NULL,
                playerInventory.player.level().getBlockState(buf.readBlockPos()).getBlock());
    }

    private MachineMenu(MenuType<?> type, int id, Inventory playerInventory, IItemHandler inventory, ContainerData data,
                        ContainerLevelAccess access, Block block) {
        super(type, id);
        this.machineInventory = inventory;
        this.data = data;
        this.access = access;
        this.block = block;
        addMachineSlots();
        this.machineSlots = this.slots.size();
        final int[] origin = playerInventoryOrigin();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, origin[0] + col * 18, origin[1] + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, origin[0] + col * 18, origin[1] + 58));
        }
        addDataSlots(data);
    }

    /** Add the machine's own slots (in inventory index order). */
    protected abstract void addMachineSlots();

    /** Top-left of the player's main inventory grid. */
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 84};
    }

    protected void addMachineSlot(int index, int x, int y) {
        addSlot(new SlotItemHandler(this.machineInventory, index, x, y));
    }

    /** A slot with an item filter that also works on the client (where the inventory is a plain copy). */
    protected void addMachineSlot(int index, int x, int y, Predicate<ItemStack> filter) {
        addSlot(new SlotItemHandler(this.machineInventory, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return filter.test(stack);
            }
        });
    }

    /** Take-only slot. */
    protected void addOutputSlot(int index, int x, int y) {
        addMachineSlot(index, x, y, stack -> false);
    }

    /** Battery slot: electric items only. */
    protected void addBatterySlot(int index, int x, int y) {
        addMachineSlot(index, x, y, stack -> stack.getItem() instanceof ElectricItem);
    }

    public float energy() {
        return SyncedData.readInt(this.data, ENERGY);
    }

    public float capacity() {
        return SyncedData.readInt(this.data, CAPACITY);
    }

    public int value(int index) {
        return SyncedData.readShort(this.data, index);
    }

    public int intValue(int index) {
        return SyncedData.readInt(this.data, index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < this.machineSlots) {
            if (!moveItemStackTo(stack, this.machineSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < this.machineSlots && !stack.isEmpty(); i++) {
                if (this.slots.get(i).mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                final int hotbarStart = this.slots.size() - 9;
                if (index < hotbarStart) {
                    if (!moveItemStackTo(stack, hotbarStart, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, this.machineSlots, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, this.block);
    }
}
