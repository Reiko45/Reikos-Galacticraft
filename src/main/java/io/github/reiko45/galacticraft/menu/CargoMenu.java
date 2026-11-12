package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.registry.GCMenus;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Cargo of a rocket, lander or para-chest.
 * <ul>
 *     <li>{@link Layout#ROCKET} is 1.12.2 ContainerRocketInventory: the rocket's 18n cargo slots (not the two slots that
 *     carry the launch pads and the rocket itself) above the player's inventory, with the fuel level.</li>
 *     <li>{@link Layout#CHEST} is 1.12.2 ContainerParaChest, used by the lander and the para-chest: 18n cargo slots, the
 *     two rocket slots, a canister slot that is filled from the fuel tank, and the player's inventory.</li>
 *     <li>{@link Layout#CARGO_ROCKET} is the rocket layout opened from the ground by a Cargo Rocket (1.12.2
 *     GuiCargoRocket), with its Launch button (button 0) and launch status.</li>
 * </ul>
 */
public class CargoMenu extends AbstractContainerMenu {
    public enum Layout {
        ROCKET, CHEST, BUGGY, CARGO_ROCKET
    }

    private final Layout layout;
    private final Container cargo;
    private final ContainerData data;
    private final Predicate<Player> valid;
    private final int cargoSlots;
    @Nullable
    private final Consumer<Player> onButton;

    /** Values: fuel, capacity, then for a Cargo Rocket its status, the status's countdown, whether it passed, and its flight phase. */
    public static final int DATA_COUNT = 6;

    private CargoMenu(int id, Inventory playerInventory, Layout layout, Container cargo, ContainerData data, Predicate<Player> valid,
                      @Nullable Consumer<Player> onButton) {
        super(GCMenus.CARGO.get(), id);
        this.layout = layout;
        this.cargo = cargo;
        this.data = data;
        this.valid = valid;
        this.onButton = onButton;
        final int size = cargo.getContainerSize();
        final int playerY;
        if (layout == Layout.BUGGY) {
            // 1.12.2 ContainerBuggy: the storage rows, then the player's inventory below them
            for (int row = 0; row < size / 9; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(cargo, col + row * 9, 8 + col * 18, 50 + row * 18));
                }
            }
            playerY = 63 + size * 2;
        } else if (layout == Layout.ROCKET || layout == Layout.CARGO_ROCKET) {
            if (size - 2 <= 0) {
                playerY = 50;
            } else {
                final int rows = size / 9;
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < 9; col++) {
                        addSlot(new Slot(cargo, col + row * 9, 8 + col * 18, 50 + row * 18));
                    }
                }
                playerY = 145 + (size - 2) * 2 - 82;
            }
        } else {
            final int rows = (size - 3) / 9;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(cargo, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }
            final int extraY = (rows == 0 ? 24 : 26) + rows * 18;
            addSlot(new Slot(cargo, size - 3, 125, extraY));
            addSlot(new Slot(cargo, size - 2, 143, extraY));
            addSlot(new Slot(cargo, size - 1, 75, extraY));
            playerY = (rows == 0 ? 116 : 118) + (rows - 4) * 18 + 19;
        }
        this.cargoSlots = this.slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, playerY + 58));
        }
        addDataSlots(data);
        cargo.startOpen(playerInventory.player);
    }

    /** Client side: {@code buf} carries the layout and the cargo size. */
    public static CargoMenu client(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        final Layout layout = buf.readEnum(Layout.class);
        final int size = buf.readVarInt();
        return new CargoMenu(id, playerInventory, layout, new SimpleContainer(size), new SimpleContainerData(DATA_COUNT), player -> true, null);
    }

    public static void open(ServerPlayer player, Component title, Layout layout, Container cargo, IntSupplier fuel, int capacity,
                            Predicate<Player> valid) {
        open(player, title, layout, cargo, index -> index == 0 ? fuel.getAsInt() : index == 1 ? capacity : 0, valid, null);
    }

    private static void open(ServerPlayer player, Component title, Layout layout, Container cargo, IntUnaryOperator values, Predicate<Player> valid,
                             @Nullable Consumer<Player> onButton) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return values.applyAsInt(index);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CargoMenu(id, inventory, layout, cargo, data, valid, onButton), title), buf -> {
            buf.writeEnum(layout);
            buf.writeVarInt(cargo.getContainerSize());
        });
    }

    /** The inventory key while riding a rocket (1.12.2 GuiRocketInventory). */
    public static void openRocket(ServerPlayer player, RocketEntity rocket) {
        open(player, rocket.getDisplayName(), Layout.ROCKET, rocket.cargo(), rocket::fuel, rocket.fuelCapacity(),
                p -> rocket.isAlive() && p.getVehicle() == rocket);
    }

    /** Right-clicking a Cargo Rocket (1.12.2 MarsUtil#openCargoRocketInventory). */
    public static void openCargoRocket(ServerPlayer player, io.github.reiko45.galacticraft.entity.CargoRocketEntity rocket) {
        open(player, rocket.getDisplayName(), Layout.CARGO_ROCKET, rocket.cargo(), index -> switch (index) {
            case 0 -> rocket.fuel();
            case 1 -> rocket.fuelCapacity();
            case 2 -> rocket.statusCode();
            case 3 -> rocket.statusCooldown();
            case 4 -> rocket.statusValid() ? 1 : 0;
            default -> rocket.phase().ordinal();
        }, p -> rocket.isAlive() && p.distanceToSqr(rocket) <= 64.0D, p -> rocket.pressLaunch());
    }

    /** The inventory key while driving a buggy (1.12.2 GuiBuggy). */
    public static void openBuggy(ServerPlayer player, io.github.reiko45.galacticraft.entity.BuggyEntity buggy) {
        open(player, buggy.getDisplayName(), Layout.BUGGY, buggy.cargo(), buggy::fuel, io.github.reiko45.galacticraft.entity.BuggyEntity.FUEL_CAPACITY,
                p -> buggy.isAlive() && p.getVehicle() == buggy);
    }

    /** Using a landed lander (1.12.2 GCCoreUtil#openParachestInv with the lander). */
    public static void openLander(ServerPlayer player, LanderEntity lander) {
        open(player, Component.translatable("container.galacticraftcore.lander"), Layout.CHEST, lander.cargo(), lander::fuel,
                LanderEntity.FUEL_CAPACITY, p -> lander.isAlive() && p.distanceToSqr(lander) <= 64.0D);
    }

    public Layout layout() {
        return this.layout;
    }

    public int cargoSize() {
        return this.cargo.getContainerSize();
    }

    public int fuel() {
        return this.data.get(0);
    }

    public int fuelCapacity() {
        return this.data.get(1);
    }

    public int value(int index) {
        return this.data.get(index);
    }

    /** The Cargo Rocket's Launch button. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.onButton != null) {
            this.onButton.accept(player);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < this.cargoSlots) {
            if (!moveItemStackTo(stack, this.cargoSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, this.cargoSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.valid.test(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.cargo.stopOpen(player);
    }
}
