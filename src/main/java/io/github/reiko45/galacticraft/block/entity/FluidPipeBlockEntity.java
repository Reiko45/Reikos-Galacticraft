package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.fluid.network.FluidNetwork;
import io.github.reiko45.galacticraft.fluid.network.FluidNetworkManager;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A pipe's share of its network's buffer (1.12.2 TileEntityFluidPipe), saved with the chunk so that fluid in transit
 * survives reloads and network rebuilds.
 */
public class FluidPipeBlockEntity extends BlockEntity {
    /** Steps of the in-pipe liquid level shown by the client (1.12.2 synced the network's fluid scale). */
    public static final int DISPLAY_STEPS = 20;

    private FluidStack stored = FluidStack.EMPTY;
    /** What the client draws inside the glass: the fluid (amount 1) and its level in 0..DISPLAY_STEPS. */
    private FluidStack displayFluid = FluidStack.EMPTY;
    private int displayLevel;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    public FluidStack stored() {
        return this.stored;
    }

    public void setStored(FluidStack stored) {
        if (!FluidStack.matches(this.stored, stored)) {
            this.stored = stored.copy();
            setChanged();
        }
    }

    public FluidStack displayFluid() {
        return this.displayFluid;
    }

    public int displayLevel() {
        return this.displayLevel;
    }

    /** Server: update what the client shows, sending a block update only when the shown level or fluid changes. */
    public void setDisplay(FluidStack fluid, int level) {
        final FluidStack shown = level <= 0 || fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
        final int shownLevel = shown.isEmpty() ? 0 : level;
        if (shownLevel != this.displayLevel || !FluidStack.isSameFluidSameComponents(shown, this.displayFluid)) {
            this.displayFluid = shown;
            this.displayLevel = shownLevel;
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    public void networkChanged() {
        if (this.level instanceof ServerLevel serverLevel) {
            FluidNetworkManager.get(serverLevel).pipeRemoved(this.worldPosition);
            FluidNetworkManager.get(serverLevel).pipeAdded(this.worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel serverLevel) {
            FluidNetworkManager.get(serverLevel).pipeAdded(this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level instanceof ServerLevel serverLevel) {
            FluidNetworkManager.get(serverLevel).pipeRemoved(this.worldPosition);
        }
    }

    /** Capability: pipes accept fluid into their network but cannot be drained (1.12.2). */
    @Nullable
    public IFluidHandler fluidHandler() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return null;
        }
        final FluidNetwork network = FluidNetworkManager.get(serverLevel).networkAt(this.worldPosition);
        return network != null ? network.handler() : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.stored.isEmpty()) {
            tag.put("Fluid", this.stored.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Display")) {
            // Client update packet
            this.displayFluid = FluidStack.parseOptional(registries, tag.getCompound("Display"));
            this.displayLevel = tag.getByte("DisplayLevel");
            return;
        }
        this.stored = tag.contains("Fluid") ? FluidStack.parseOptional(registries, tag.getCompound("Fluid")) : FluidStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.put("Display", this.displayFluid.saveOptional(registries));
        tag.putByte("DisplayLevel", (byte) this.displayLevel);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
