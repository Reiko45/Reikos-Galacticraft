package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.OxygenDistributorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenMachineBlockEntity;
import io.github.reiko45.galacticraft.item.gear.InfiniteOxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCMenus;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

/** Menu for every oxygen machine (1.12.2 ContainerOxygen*); {@link Kind} holds each machine's slot layout. */
public class OxygenMachineMenu extends MachineMenu {
    public static final int OXYGEN = EXTRA;
    public static final int OXYGEN_CAPACITY = EXTRA + 2;
    public static final int STATUS = EXTRA + 4;
    public static final int DISABLED = EXTRA + 5;
    public static final int VALUE_1 = EXTRA + 6;
    public static final int VALUE_2 = EXTRA + 7;
    public static final int DATA_COUNT = EXTRA + 8;
    public static final int BUTTON_DISABLE = 0;
    public static final int BUTTON_BUBBLE = 1;

    /** Slot positions from the 1.12.2 containers; inventory origin is the top-left of the player grid. */
    public enum Kind {
        COLLECTOR(() -> GCMenus.OXYGEN_COLLECTOR.get(), 1, 8, 104),
        COMPRESSOR(() -> GCMenus.OXYGEN_COMPRESSOR.get(), 3, 8, 104),
        DECOMPRESSOR(() -> GCMenus.OXYGEN_DECOMPRESSOR.get(), 2, 8, 104),
        DISTRIBUTOR(() -> GCMenus.OXYGEN_DISTRIBUTOR.get(), 2, 8, 104),
        SEALER(() -> GCMenus.OXYGEN_SEALER.get(), 3, 8, 124),
        STORAGE_MODULE(() -> GCMenus.OXYGEN_STORAGE_MODULE.get(), 1, 8, 84);

        private final Supplier<MenuType<OxygenMachineMenu>> type;
        private final int slots;
        private final int inventoryX;
        private final int inventoryY;

        Kind(Supplier<MenuType<OxygenMachineMenu>> type, int slots, int inventoryX, int inventoryY) {
            this.type = type;
            this.slots = slots;
            this.inventoryX = inventoryX;
            this.inventoryY = inventoryY;
        }

        public MenuType<OxygenMachineMenu> type() {
            return this.type.get();
        }
    }

    private final Kind kind;
    @Nullable
    private final OxygenMachineBlockEntity machine;

    public OxygenMachineMenu(int id, Inventory playerInventory, OxygenMachineBlockEntity machine) {
        super(machine.menuKind().type(), id, playerInventory, machine);
        this.kind = machine.menuKind();
        this.machine = machine;
    }

    public OxygenMachineMenu(Kind kind, int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(kind.type(), id, playerInventory, buf, kind.slots, DATA_COUNT);
        this.kind = kind;
        this.machine = null;
    }

    public Kind kind() {
        return this.kind;
    }

    /** Called from the super constructor, before {@link #kind} is set: the layout is chosen from the menu type. */
    @Override
    protected void addMachineSlots() {
        final Kind kind = kindOf(getType());
        switch (kind) {
            case COLLECTOR -> addBatterySlot(0, 32, 27);
            case COMPRESSOR -> {
                addMachineSlot(0, 133, 71, stack -> stack.getItem() instanceof OxygenTankItem);
                addBatterySlot(1, 47, 27);
                addMachineSlot(2, 17, 27, OxygenMachineMenu::isOxygenSupply);
            }
            case DECOMPRESSOR -> {
                addMachineSlot(0, 133, 71, stack -> stack.getItem() instanceof OxygenTankItem || stack.getItem() instanceof InfiniteOxygenTankItem);
                addBatterySlot(1, 32, 27);
            }
            case DISTRIBUTOR -> {
                addBatterySlot(0, 47, 27);
                addMachineSlot(1, 17, 27, OxygenMachineMenu::isOxygenSupply);
            }
            case SEALER -> {
                addBatterySlot(0, 33, 27);
                addMachineSlot(1, 10, 27, OxygenMachineMenu::isOxygenSupply);
                addMachineSlot(2, 56, 27, stack -> stack.is(GCItems.AMBIENT_THERMAL_CONTROLLER.get()));
            }
            case STORAGE_MODULE -> addMachineSlot(0, 17, 22, OxygenMachineMenu::isOxygenSupply);
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        final Kind kind = kindOf(getType());
        return new int[]{kind.inventoryX, kind.inventoryY};
    }

    private static Kind kindOf(MenuType<?> type) {
        for (Kind kind : Kind.values()) {
            if (kind.type() == type) {
                return kind;
            }
        }
        throw new IllegalStateException("Not an oxygen machine menu: " + type);
    }

    /** 1.12.2 IItemOxygenSupply: anything that can give oxygen. */
    public static boolean isOxygenSupply(ItemStack stack) {
        final IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        return handler != null && !handler.drain(new FluidStack(GCFluids.OXYGEN.get(), 1), IFluidHandler.FluidAction.SIMULATE).isEmpty();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.machine == null) {
            return false;
        }
        if (id == BUTTON_DISABLE) {
            this.machine.toggleDisabled();
            return true;
        }
        if (id == BUTTON_BUBBLE && this.machine instanceof OxygenDistributorBlockEntity distributor) {
            distributor.toggleBubbleVisible();
            return true;
        }
        return false;
    }

    public int oxygen() {
        return intValue(OXYGEN);
    }

    public int oxygenCapacity() {
        return intValue(OXYGEN_CAPACITY);
    }

    public MachineStatus status() {
        return MachineStatus.byOrdinal(value(STATUS));
    }

    public boolean isDisabled() {
        return value(DISABLED) != 0;
    }

    public int value1() {
        return value(VALUE_1);
    }

    public int value2() {
        return value(VALUE_2);
    }
}
