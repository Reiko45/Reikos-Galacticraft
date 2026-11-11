package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.menu.CargoMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * 1.12.2 TileEntityParaChest: 18n cargo slots, the two rocket slots (launch pads, rocket), and a last slot where an
 * empty canister is filled from the 5,000 mB fuel tank holding the rocket's leftover fuel.
 */
public class ParachestBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 5000;

    private SimpleContainer cargo = createContainer(3);
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.is(GCFluids.FUEL.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public ParachestBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.PARACHEST.get(), pos, state);
    }

    private SimpleContainer createContainer(int size) {
        final SimpleContainer container = new SimpleContainer(size);
        container.addListener(c -> setChanged());
        return container;
    }

    /** 1.12.2 readFromNBT: the size is rounded up to 18n + 3. */
    public static int roundedSize(int size) {
        if ((size - 3) % 18 != 0) {
            size += 18 - Math.floorMod(size - 3, 18);
        }
        return Math.max(3, size);
    }

    /** Fill with a rocket's cargo (18n + 2 slots) and fuel. */
    public void fill(NonNullList<ItemStack> items, int fuel) {
        this.cargo = createContainer(roundedSize(items.size()));
        for (int i = 0; i < Math.min(items.size(), this.cargo.getContainerSize() - 1); i++) {
            this.cargo.setItem(i, items.get(i).copy());
        }
        this.tank.setFluid(fuel > 0 ? new FluidStack(GCFluids.FUEL.get(), Math.min(fuel, TANK_CAPACITY)) : FluidStack.EMPTY);
        setChanged();
    }

    public SimpleContainer cargo() {
        return this.cargo;
    }

    public FluidTank fuelTank() {
        return this.tank;
    }

    /** 1.12.2 checkFluidTankTransfer: fill the canister in the last slot. */
    public void serverTick() {
        final int slot = this.cargo.getContainerSize() - 1;
        final ItemStack stack = this.cargo.getItem(slot);
        if (!stack.isEmpty() && !this.tank.isEmpty()) {
            final FluidActionResult result = FluidUtil.tryFillContainer(stack, this.tank, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                this.cargo.setItem(slot, result.getResult());
            }
        }
    }

    public void open(ServerPlayer player) {
        CargoMenu.open(player, Component.translatable("container.galacticraftcore.parachest"), CargoMenu.Layout.CHEST, this.cargo,
                this.tank::getFluidAmount, TANK_CAPACITY,
                p -> !isRemoved() && p.distanceToSqr(this.worldPosition.getCenter()) <= 64.0D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ChestContentLength", this.cargo.getContainerSize());
        ContainerHelper.saveAllItems(tag, this.cargo.getItems(), registries);
        tag.put("FuelTank", this.tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.cargo = createContainer(roundedSize(tag.getInt("ChestContentLength")));
        final NonNullList<ItemStack> items = NonNullList.withSize(this.cargo.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int i = 0; i < items.size(); i++) {
            this.cargo.setItem(i, items.get(i));
        }
        this.tank.readFromNBT(registries, tag.getCompound("FuelTank"));
    }
}
