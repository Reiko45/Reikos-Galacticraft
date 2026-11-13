package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.energy.GCEnergy;
import io.github.reiko45.galacticraft.energy.GCEnergyStorage;
import io.github.reiko45.galacticraft.energy.IGCEnergyHandler;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.machine.SidedItemHandler;
import io.github.reiko45.galacticraft.menu.SyncedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Common base of Galacticraft machines (1.12.2 TileBaseElectricBlockWithInventory / TileBaseUniversalElectrical):
 * an item inventory with per-side automation rules, a gJ energy buffer with per-face input/output, a battery slot
 * helper, synced GUI data, and NBT persistence.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    protected final ItemStackHandler inventory;
    protected final GCEnergyStorage energy;
    private final SyncedData data = new SyncedData();

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots,
                                 float capacity, float maxReceive, float maxExtract) {
        super(type, pos, state);
        this.inventory = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return MachineBlockEntity.this.isItemValid(slot, stack);
            }
        };
        this.energy = new GCEnergyStorage(capacity, maxReceive, maxExtract, this::setChanged);
        this.data.addInt(() -> (int) Math.floor(this.energy.getStoredGJ()));
        this.data.addInt(() -> (int) this.energy.getCapacityGJ());
        addSyncedData(this.data);
    }

    // ------------------------------------------------------------------ subclass hooks

    /** Called every server tick. */
    protected abstract void tick();

    /**
     * gJ a tick it uses while it works, for the "Power required" line under its item (1.12.2 ItemBlockDesc showed
     * storage.getMaxExtract() of machines that use power); 0 for machines that make or store it.
     */
    public float powerDraw() {
        return 0.0F;
    }

    /** Extra GUI values after the energy/capacity ints (menu slots 0-3). */
    protected void addSyncedData(SyncedData data) {
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    /** Which faces accept or output energy. */
    protected abstract EnergyAccess energyAccess(RelativeSide side);

    /** Slots reachable by automation from the given face (1.12.2 getSlotsForFace). */
    protected int[] automationSlots(RelativeSide side) {
        return new int[0];
    }

    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return true;
    }

    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return false;
    }

    // ------------------------------------------------------------------ accessors

    public ItemStackHandler inventory() {
        return this.inventory;
    }

    public GCEnergyStorage energy() {
        return this.energy;
    }

    public SyncedData syncedData() {
        return this.data;
    }

    public Direction facing() {
        final BlockState state = getBlockState();
        return state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    /** Capability provider for {@link io.github.reiko45.galacticraft.energy.GCEnergy#BLOCK}. */
    @Nullable
    public IGCEnergyHandler energyHandler(@Nullable Direction side) {
        if (side == null) {
            return this.energy;
        }
        final EnergyAccess access = energyAccess(RelativeSide.of(facing(), side));
        return access == EnergyAccess.NONE ? null : this.energy.view(access.receive, access.extract);
    }

    /** Capability provider for item handlers. */
    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        if (side == null) {
            return this.inventory;
        }
        final RelativeSide relative = RelativeSide.of(facing(), side);
        final int[] slots = automationSlots(relative);
        if (slots.length == 0) {
            return null;
        }
        return new SidedItemHandler(this.inventory, slots,
                (slot, stack) -> canAutomationInsert(slot, stack, relative),
                slot -> canAutomationExtract(slot, relative));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ------------------------------------------------------------------ helpers

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.energy.resetTick();
        machine.tick();
    }

    /**
     * Move energy from a battery in {@code slot} into the machine (1.12.2 discharge(ItemStack)): at most the
     * machine's receive rate and the battery's 200 gJ/t. Battery power counts as tier 1.
     */
    protected void dischargeBattery(int slot) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        if (stack.getItem() instanceof ElectricItem electric) {
            final float wanted = Math.min(this.energy.getCapacityGJ() - this.energy.getStoredGJ(), this.energy.maxReceive());
            if (wanted > 0.0F && this.energy.generate(electric.discharge(stack, wanted, false)) > 0.0F) {
                this.energy.setPoweredTier(electric.tier());
            }
        }
    }

    /** Charge a battery in {@code slot} from this tick's output budget (1.12.2 recharge(ItemStack)). */
    protected void chargeBattery(int slot) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        if (stack.getCount() == 1 && stack.getItem() instanceof ElectricItem electric) {
            final float budget = this.energy.extractGJ(Float.MAX_VALUE, true);
            final float accepted = electric.charge(stack, budget, true);
            if (accepted > 0.0F) {
                electric.charge(stack, this.energy.extractGJ(accepted, false), false);
            }
        }
    }

    /**
     * Push energy out of every output face straight into an adjacent machine (1.12.2 produce() also fed adjacent
     * acceptors without a wire). Faces touching a wire are left to the wire network.
     */
    protected void pushEnergy() {
        if (this.level == null || this.energy.getStoredGJ() <= 0.0F) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (energyAccess(RelativeSide.of(facing(), side)) != EnergyAccess.OUTPUT) {
                continue;
            }
            final BlockPos target = this.worldPosition.relative(side);
            if (this.level.getBlockState(target).getBlock() instanceof AluminumWireBlock) {
                continue;
            }
            IGCEnergyHandler receiver = this.level.getCapability(GCEnergy.BLOCK, target, side.getOpposite());
            if (receiver == null) {
                receiver = io.github.reiko45.galacticraft.energy.ForeignEnergy.find(this.level, target, side.getOpposite());
            }
            if (receiver == null || !receiver.canReceive()) {
                continue;
            }
            final float accepted = receiver.receiveGJ(this.energy.extractGJ(Float.MAX_VALUE, true), true);
            if (accepted > 0.0F && receiver.receiveGJ(this.energy.extractGJ(accepted, false), false) > 0.0F) {
                receiver.setPoweredTier(this.energy.tier());
            }
        }
    }

    /** The block was broken or replaced (not just unloaded): undo effects on the world. */
    public void onBroken() {
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), this.inventory.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
        this.energy.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.energy.load(tag);
    }
}
