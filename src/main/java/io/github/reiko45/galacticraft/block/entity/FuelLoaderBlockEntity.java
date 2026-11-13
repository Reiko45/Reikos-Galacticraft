package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.FuelLoaderMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Fuel Loader (1.12.2 TileEntityFuelLoader): a 12,000 mB fuel tank, filled from fuel canisters or a fuel pipe on the
 * right face, that pumps 2 mB per tick into the rocket on an adjacent launch pad. Uses 30 gJ per tick while loading,
 * powered through the left face. Slots: 0 battery, 1 fuel container.
 */
public class FuelLoaderBlockEntity extends MachineBlockEntity {
    public static final int TANK_CAPACITY = 12_000;
    public static final float ENERGY_USE = 30.0F;
    /** 1.12.2: new FluidStack(fuel, 2) every tick. */
    public static final int FUEL_PER_TICK = 2;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.is(GCFluidTags.FUEL) || stack.is(GCFluids.FUEL.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private boolean disabled;
    private int disableCooldown;
    private boolean hasEnoughEnergyToRun;
    private boolean loadedFuelLastTick;
    @Nullable
    private BlockPos attachedPad;
    private int ticks;

    public FuelLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.FUEL_LOADER.get(), pos, state, 2, 16_000.0F, 2.5F * ENERGY_USE, 0.0F);
    }

    public FluidTank fuelTank() {
        return this.tank;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    /** 1.12.2 setDisabled: 10 tick cooldown between presses. */
    public void toggleDisabled() {
        if (this.disableCooldown == 0) {
            this.disabled = !this.disabled;
            this.disableCooldown = 10;
            setChanged();
        }
    }

    public boolean canToggle() {
        return this.disableCooldown == 0;
    }

    @Override
    protected void tick() {
        this.ticks++;
        if (this.disableCooldown > 0) {
            this.disableCooldown--;
        }
        dischargeBattery(0);
        // 1.12.2 TileBaseElectricBlock#update
        final boolean redstoneOff = this.level != null && this.level.hasNeighborSignal(this.worldPosition);
        if (this.energy.getStoredGJ() > ENERGY_USE && !redstoneOff) {
            this.hasEnoughEnergyToRun = true;
            if (shouldUseEnergy()) {
                this.energy.consume(ENERGY_USE);
            } else {
                slowDischarge();
            }
        } else {
            this.hasEnoughEnergyToRun = false;
            slowDischarge();
        }
        this.loadedFuelLastTick = false;
        emptyFuelContainer();
        if (this.ticks % 100 == 1) {
            this.attachedPad = findPad();
        }
        if (!this.tank.isEmpty() && this.hasEnoughEnergyToRun && !this.disabled) {
            final io.github.reiko45.galacticraft.entity.Refuelable vehicle = dockedVehicle();
            if (vehicle != null) {
                final int filled = vehicle.addFuel(Math.min(FUEL_PER_TICK, this.tank.getFluidAmount()), true);
                this.loadedFuelLastTick = filled > 0;
                this.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private void slowDischarge() {
        if (this.level != null && this.level.getGameTime() % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
    }

    /** 1.12.2 shouldUseEnergy: only while fuel actually went into a rocket last tick. */
    private boolean shouldUseEnergy() {
        return !this.tank.isEmpty() && !this.disabled && this.loadedFuelLastTick;
    }

    private void emptyFuelContainer() {
        final ItemStack stack = this.inventory.getStackInSlot(1);
        if (!stack.isEmpty() && isFuelSource(stack)) {
            final FluidActionResult result = FluidUtil.tryEmptyContainer(stack, this.tank, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                this.inventory.setStackInSlot(1, result.getResult());
            }
        }
    }

    /** 1.12.2: the first launch pad found on any side (any part of a joined pad). */
    @Nullable
    private BlockPos findPad() {
        if (this.level == null) {
            return null;
        }
        for (Direction side : Direction.values()) {
            final BlockPos centre = LandingPadBlock.findCentre(this.level, this.worldPosition.relative(side));
            if (centre != null) {
                return centre;
            }
        }
        return null;
    }

    /**
     * The rocket standing on the attached launch pad (1.12.2 TileEntityLandingPad#getDockedEntity), or the buggy parked
     * on the attached fueling pad (1.12.2 TileEntityBuggyFueler: within 1.5 blocks of the centre, up to 1 block above).
     */
    @Nullable
    public io.github.reiko45.galacticraft.entity.Refuelable dockedVehicle() {
        if (this.level == null || this.attachedPad == null) {
            return null;
        }
        if (LandingPadBlock.findCentre(this.level, this.attachedPad) == null) {
            this.attachedPad = null;
            return null;
        }
        if (this.level.getBlockState(this.attachedPad).getBlock() instanceof io.github.reiko45.galacticraft.block.BuggyFuelingPadBlock) {
            final List<io.github.reiko45.galacticraft.entity.BuggyEntity> buggies = this.level.getEntitiesOfClass(
                    io.github.reiko45.galacticraft.entity.BuggyEntity.class, new AABB(this.attachedPad).inflate(1.5D, 0.0D, 1.5D).expandTowards(0.0D, 1.0D, 0.0D),
                    io.github.reiko45.galacticraft.entity.BuggyEntity::isAlive);
            return buggies.isEmpty() ? null : buggies.get(0);
        }
        return dockedRocket();
    }

    /** The rocket standing on the attached launch pad. */
    @Nullable
    public RocketEntity dockedRocket() {
        if (this.level == null || this.attachedPad == null || LandingPadBlock.findLaunchPadCentre(this.level, this.attachedPad) == null) {
            return null;
        }
        final List<RocketEntity> rockets = this.level.getEntitiesOfClass(RocketEntity.class,
                new AABB(this.attachedPad).inflate(0.5D, 0.0D, 0.5D).expandTowards(0.0D, 2.0D, 0.0D),
                rocket -> !rocket.launched() && rocket.isAlive());
        return rockets.isEmpty() ? null : rockets.get(0);
    }

    public static boolean isFuelSource(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).filter(fluid -> fluid.is(GCFluidTags.FUEL) || fluid.is(GCFluids.FUEL.get())).isPresent();
    }

    /** 1.12.2 GuiFuelLoader#getStatus over TileBaseElectricBlock#getGUIstatus. */
    public MachineStatus status() {
        if (this.tank.isEmpty()) {
            return MachineStatus.NO_FUEL;
        }
        if (this.level != null && this.level.hasNeighborSignal(this.worldPosition)) {
            return MachineStatus.OFF;
        }
        if (this.energy.getStoredGJ() <= 0.0F) {
            return MachineStatus.MISSING_POWER;
        }
        if (this.disabled) {
            return MachineStatus.READY;
        }
        if (this.energy.getStoredGJ() < ENERGY_USE) {
            return MachineStatus.LOW_MISSING_POWER;
        }
        return MachineStatus.ACTIVE;
    }

    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(() -> this.tank.getFluidAmount());
        data.addShort(() -> status().ordinal());
        data.addShort(() -> this.disabled ? 1 : 0);
        data.addShort(() -> this.disableCooldown);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem;
            case 1 -> isFuelSource(stack) || FluidUtil.getFluidHandler(stack).isPresent();
            default -> false;
        };
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return side == RelativeSide.LEFT ? EnergyAccess.INPUT : EnergyAccess.NONE;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0, 1};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return slot == 0 ? stack.getItem() instanceof ElectricItem : isFuelSource(stack);
    }

    /** 1.12.2 canExtractItem: only emptied containers come out of slot 1. */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return slot == 1 && !isFuelSource(this.inventory.getStackInSlot(1));
    }

    /** Fuel goes in through the right face (1.12.2 default PIPE_IN), never out. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side != null && RelativeSide.of(facing(), side) != RelativeSide.RIGHT) {
            return null;
        }
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int index) {
                return FuelLoaderBlockEntity.this.tank.getFluid();
            }

            @Override
            public int getTankCapacity(int index) {
                return TANK_CAPACITY;
            }

            @Override
            public boolean isFluidValid(int index, FluidStack stack) {
                return FuelLoaderBlockEntity.this.tank.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return FuelLoaderBlockEntity.this.tank.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new FuelLoaderMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelTank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("Disabled", this.disabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank.readFromNBT(registries, tag.getCompound("FuelTank"));
        this.disabled = tag.getBoolean("Disabled");
    }

    @Override
    public float powerDraw() {
        return ENERGY_USE;
    }
}
