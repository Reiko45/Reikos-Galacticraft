/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.inventory.ContainerElectrolyzer,
 * ContainerMethaneSynthesizer and ContainerGasLiquefier). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.menu;

import io.github.reiko45.galacticraft.block.entity.ElectrolyzerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity;
import io.github.reiko45.galacticraft.block.entity.GasMachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MethaneSynthesizerBlockEntity;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCMenus;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Mars gas machines' menus (168 px tall, inventory from y = 86): the battery, the input and output slots, the
 * tanks' amounts and fluids, and the Process / Stop switch.
 */
public class GasMachineMenu extends ProcessingMenu {
    public static final int TANKS = DATA_COUNT;
    public static final int DISABLED = DATA_COUNT + 6;
    public static final int BUTTON_TOGGLE = 0;

    /** The three machines: their slots, tank capacities and GUI. */
    public enum Kind {
        ELECTROLYZER(4, () -> GCMenus.ELECTROLYZER.get(), new int[]{ElectrolyzerBlockEntity.TANK_CAPACITY, ElectrolyzerBlockEntity.TANK_CAPACITY,
                ElectrolyzerBlockEntity.TANK_CAPACITY}),
        METHANE_SYNTHESIZER(5, () -> GCMenus.METHANE_SYNTHESIZER.get(), new int[]{MethaneSynthesizerBlockEntity.HYDROGEN_CAPACITY,
                MethaneSynthesizerBlockEntity.CO2_CAPACITY, MethaneSynthesizerBlockEntity.METHANE_CAPACITY}),
        GAS_LIQUEFIER(3, () -> GCMenus.GAS_LIQUEFIER.get(), new int[]{GasLiquefierBlockEntity.GAS_CAPACITY, GasLiquefierBlockEntity.LIQUID_CAPACITY});

        private final int slots;
        private final Supplier<MenuType<GasMachineMenu>> type;
        private final int[] capacities;

        Kind(int slots, Supplier<MenuType<GasMachineMenu>> type, int[] capacities) {
            this.slots = slots;
            this.type = type;
            this.capacities = capacities;
        }
    }

    private final Kind kind;
    @Nullable
    private final GasMachineBlockEntity machine;

    public GasMachineMenu(Kind kind, int id, Inventory playerInventory, GasMachineBlockEntity machine) {
        super(kind.type.get(), id, playerInventory, machine);
        this.kind = kind;
        this.machine = machine;
    }

    public GasMachineMenu(Kind kind, int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(kind.type.get(), id, playerInventory, buf, kind.slots, DATA_COUNT + 7);
        this.kind = kind;
        this.machine = null;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected void addMachineSlots() {
        // The kind isn't set yet in this super-constructor call; the slot count tells the machines apart.
        final int count = this.machineInventory.getSlots();
        if (count == 4) {
            addBatterySlot(0, 34, 50);
            addMachineSlot(1, 7, 7);
            addMachineSlot(2, 132, 7, GasMachineBlockEntity::isValve);
            addMachineSlot(3, 153, 7, GasMachineBlockEntity::isValve);
        } else if (count == 5) {
            addBatterySlot(0, 53, 53);
            addMachineSlot(1, 7, 7);
            addMachineSlot(2, 28, 7, GasMachineBlockEntity::isValve);
            addMachineSlot(3, 28, 53, stack -> stack.is(GCItems.CARBON_FRAGMENTS.get()));
            addMachineSlot(4, 153, 7);
        } else {
            addBatterySlot(0, 34, 50);
            addMachineSlot(1, 7, 7);
            addMachineSlot(2, 132, 7);
        }
    }

    @Override
    protected int[] playerInventoryOrigin() {
        return new int[]{8, 86};
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && this.machine != null) {
            this.machine.toggleDisabled();
            return true;
        }
        return false;
    }

    public int tankAmount(int tank) {
        return value(TANKS + tank * 2);
    }

    public Fluid tankFluid(int tank) {
        return GasMachineBlockEntity.shownFluid(value(TANKS + tank * 2 + 1));
    }

    public int tankCapacity(int tank) {
        return tank < this.kind.capacities.length ? this.kind.capacities[tank] : 0;
    }

    public boolean isDisabled() {
        return value(DISABLED) != 0;
    }
}
