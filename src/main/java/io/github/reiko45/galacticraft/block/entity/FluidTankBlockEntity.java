package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 TileEntityFluidTank. Each block holds 16,000 mB of any liquid or gas. Through the capability a stack of tanks
 * behaves as one: fills start at the bottom tank and carry upwards, drains come from the bottom tank, and every tick
 * each tank pours what it can into the tank below.
 */
public class FluidTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            FluidTankBlockEntity.this.changed = true;
            setChanged();
        }
    };
    private boolean changed;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.FLUID_TANK.get(), pos, state);
    }

    public FluidTank tank() {
        return this.tank;
    }

    @Nullable
    private FluidTankBlockEntity neighbour(int dy) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition.above(dy)) instanceof FluidTankBlockEntity other ? other : null;
    }

    public FluidTankBlockEntity bottom() {
        FluidTankBlockEntity current = this;
        FluidTankBlockEntity below;
        while ((below = current.neighbour(-1)) != null) {
            current = below;
        }
        return current;
    }

    @Nullable
    public FluidTankBlockEntity above() {
        return neighbour(1);
    }

    @Nullable
    public FluidTankBlockEntity below() {
        return neighbour(-1);
    }

    public void serverTick() {
        final FluidTankBlockEntity below = below();
        if (below != null && !this.tank.isEmpty()) {
            final int moved = below.tank.fill(this.tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) {
                this.tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        if (this.changed && this.level != null) {
            this.changed = false;
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The capability: one handler for the whole stack. */
    public IFluidHandler stackHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int index) {
                // 1.12.2 getTankInfo: the bottom tank's fluid, summed upwards while it is the same fluid.
                FluidTankBlockEntity current = bottom();
                final FluidStack total = current.tank.getFluid().copy();
                while ((current = current.above()) != null && !total.isEmpty()
                        && FluidStack.isSameFluidSameComponents(total, current.tank.getFluid())) {
                    total.grow(current.tank.getFluidAmount());
                }
                return total;
            }

            @Override
            public int getTankCapacity(int index) {
                int capacity = 0;
                for (FluidTankBlockEntity current = bottom(); current != null; current = current.above()) {
                    capacity += current.tank.getCapacity();
                }
                return capacity;
            }

            @Override
            public boolean isFluidValid(int index, FluidStack stack) {
                return true;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                FluidTankBlockEntity current = bottom();
                final FluidStack bottomFluid = current.tank.getFluid();
                if (!bottomFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(bottomFluid, resource)) {
                    return 0;
                }
                int filled = 0;
                while (current != null && filled < resource.getAmount()) {
                    filled += current.tank.fill(resource.copyWithAmount(resource.getAmount() - filled), action);
                    current = current.above();
                }
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return bottom().tank.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return bottom().tank.drain(maxDrain, action);
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
