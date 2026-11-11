package io.github.reiko45.galacticraft.fluid.network;

import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.entity.FluidPipeBlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Connected fluid pipes sharing one buffer (1.12.2 FluidNetwork): 200 mB per pipe, 1,000 mB for a lone pipe. Each
 * tick pull-mode pipes draw up to 100 mB from every adjacent block, then the whole buffer is offered to the blocks
 * next to normal pipes, split evenly in random order.
 */
public final class FluidNetwork {
    public static final int PER_PIPE = 200;
    public static final int LONE_PIPE = 1000;
    public static final int PULL_RATE = 100;

    private final ServerLevel level;
    private final Set<BlockPos> pipes;
    private final List<Endpoint> endpoints = new ArrayList<>();
    private final int capacity;
    private FluidStack buffer;
    private boolean valid = true;

    private record Endpoint(BlockPos pipe, BlockCapabilityCache<IFluidHandler, Direction> neighbor) {
    }

    FluidNetwork(ServerLevel level, Set<BlockPos> pipes) {
        this.level = level;
        this.pipes = pipes;
        this.capacity = pipes.size() == 1 ? LONE_PIPE : PER_PIPE * pipes.size();
        // Merge the pipes' saved shares; with mixed fluids the larger amount wins (1.12.2 merge rule).
        FluidStack merged = FluidStack.EMPTY;
        for (BlockPos pos : pipes) {
            if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe && !pipe.stored().isEmpty()) {
                final FluidStack share = pipe.stored();
                if (merged.isEmpty()) {
                    merged = share.copy();
                } else if (FluidStack.isSameFluidSameComponents(merged, share)) {
                    merged.grow(share.getAmount());
                } else if (share.getAmount() > merged.getAmount()) {
                    merged = share.copy();
                }
            }
        }
        this.buffer = merged.isEmpty() ? FluidStack.EMPTY : merged.copyWithAmount(Math.min(merged.getAmount(), this.capacity));
        for (BlockPos pipe : pipes) {
            for (Direction dir : Direction.values()) {
                final BlockPos neighbor = pipe.relative(dir);
                if (!pipes.contains(neighbor) && !(level.getBlockState(neighbor).getBlock() instanceof FluidPipeBlock)) {
                    this.endpoints.add(new Endpoint(pipe, BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level,
                            neighbor, dir.getOpposite(), () -> this.valid, () -> { })));
                }
            }
        }
    }

    public Set<BlockPos> pipes() {
        return this.pipes;
    }

    public FluidStack buffer() {
        return this.buffer;
    }

    public int capacity() {
        return this.capacity;
    }

    boolean isValid() {
        return this.valid;
    }

    void invalidate() {
        this.valid = false;
        writeShares();
    }

    private boolean isPull(BlockPos pipe) {
        final BlockState state = this.level.getBlockState(pipe);
        return state.getBlock() instanceof FluidPipeBlock && state.getValue(FluidPipeBlock.PULL);
    }

    /** 1.12.2 emitToBuffer: same fluid only, up to the free space. */
    int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        if (resource.isEmpty() || !this.buffer.isEmpty() && !FluidStack.isSameFluidSameComponents(this.buffer, resource)) {
            return 0;
        }
        final int amount = Math.min(resource.getAmount(), this.capacity - this.buffer.getAmount());
        if (amount > 0 && action.execute()) {
            this.buffer = resource.copyWithAmount(this.buffer.getAmount() + amount);
        }
        return amount;
    }

    void tick() {
        for (Endpoint endpoint : this.endpoints) {
            if (!isPull(endpoint.pipe())) {
                continue;
            }
            final IFluidHandler source = endpoint.neighbor().getCapability();
            if (source == null) {
                continue;
            }
            final FluidStack offered = source.drain(PULL_RATE, IFluidHandler.FluidAction.SIMULATE);
            final int accepted = fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                fill(source.drain(offered.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        // What flows through this tick is what the pipes show (the buffer is often emptied again right away).
        final FluidStack passing = this.buffer.copy();
        if (!this.buffer.isEmpty()) {
            final List<IFluidHandler> acceptors = new ArrayList<>();
            for (Endpoint endpoint : this.endpoints) {
                final IFluidHandler handler = endpoint.neighbor().getCapability();
                if (handler != null && !isPull(endpoint.pipe()) && handler.fill(this.buffer.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) > 0) {
                    acceptors.add(handler);
                }
            }
            if (!acceptors.isEmpty()) {
                Collections.shuffle(acceptors);
                final int total = this.buffer.getAmount();
                final int share = total / acceptors.size();
                int remainder = total % acceptors.size();
                int delivered = 0;
                for (IFluidHandler acceptor : acceptors) {
                    final int amount = share + (remainder-- > 0 ? 1 : 0);
                    if (amount > 0) {
                        delivered += acceptor.fill(this.buffer.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
                final int left = total - delivered;
                this.buffer = left > 0 ? this.buffer.copyWithAmount(left) : FluidStack.EMPTY;
            }
        }
        writeShares();
        final int shown = passing.isEmpty() ? 0
                : Math.max(1, (int) Math.ceil(passing.getAmount() * (double) FluidPipeBlockEntity.DISPLAY_STEPS / this.capacity));
        for (BlockPos pos : this.pipes) {
            if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
                pipe.setDisplay(passing, shown);
            }
        }
    }

    /** Spread the buffer over the pipes so it is saved with them. */
    private void writeShares() {
        final int count = this.pipes.size();
        final int each = this.buffer.isEmpty() ? 0 : this.buffer.getAmount() / count;
        int remainder = this.buffer.isEmpty() ? 0 : this.buffer.getAmount() % count;
        for (BlockPos pos : this.pipes) {
            if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
                final int amount = each + (remainder-- > 0 ? 1 : 0);
                pipe.setStored(amount > 0 ? this.buffer.copyWithAmount(amount) : FluidStack.EMPTY);
            }
        }
    }

    /** What other mods (and tanks, machines) see on a pipe: fill-only, with the network buffer as its tank. */
    public IFluidHandler handler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return FluidNetwork.this.buffer.copy();
            }

            @Override
            public int getTankCapacity(int tank) {
                return FluidNetwork.this.capacity;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return true;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return FluidNetwork.this.valid ? FluidNetwork.this.fill(resource, action) : 0;
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

    @Nullable
    static BlockState stateIfPipe(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        final BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof FluidPipeBlock ? state : null;
    }
}
