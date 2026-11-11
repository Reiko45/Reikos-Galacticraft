package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.ProcessingMachineBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

/** Menu for {@link ProcessingMachineBlockEntity}: progress, process time and status after the energy values. */
public abstract class ProcessingMenu extends MachineMenu {
    public static final int PROGRESS = EXTRA;
    public static final int PROCESS_TIME = EXTRA + 1;
    public static final int STATUS = EXTRA + 2;
    public static final int DATA_COUNT = EXTRA + 3;

    protected final Level level;

    protected ProcessingMenu(MenuType<?> type, int id, Inventory playerInventory, ProcessingMachineBlockEntity machine) {
        super(type, id, playerInventory, machine);
        this.level = playerInventory.player.level();
    }

    protected ProcessingMenu(MenuType<?> type, int id, Inventory playerInventory, RegistryFriendlyByteBuf buf, int slots) {
        this(type, id, playerInventory, buf, slots, DATA_COUNT);
    }

    /** For subclasses that sync extra values after the processing ones. */
    protected ProcessingMenu(MenuType<?> type, int id, Inventory playerInventory, RegistryFriendlyByteBuf buf, int slots, int dataCount) {
        super(type, id, playerInventory, buf, slots, dataCount);
        this.level = playerInventory.player.level();
    }

    public int progress() {
        return value(PROGRESS);
    }

    public int processTime() {
        return Math.max(1, value(PROCESS_TIME));
    }

    public float progressFraction() {
        return (float) progress() / processTime();
    }

    public MachineStatus status() {
        return MachineStatus.byOrdinal(value(STATUS));
    }
}
