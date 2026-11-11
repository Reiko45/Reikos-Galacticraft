package io.github.reiko45.galacticraft.fluid.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/** Fluid pipe networks per level, rebuilt by flood fill when pipes are added, removed or recoloured. */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class FluidNetworkManager {
    private static final Map<ServerLevel, FluidNetworkManager> MANAGERS = new WeakHashMap<>();

    private final ServerLevel level;
    private final Map<BlockPos, FluidNetwork> networkByPipe = new HashMap<>();
    private final Set<FluidNetwork> networks = new LinkedHashSet<>();
    private final Set<BlockPos> dirty = new LinkedHashSet<>();

    private FluidNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static FluidNetworkManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, FluidNetworkManager::new);
    }

    public void pipeAdded(BlockPos pos) {
        final BlockPos pipe = pos.immutable();
        this.dirty.add(pipe);
        for (Direction dir : Direction.values()) {
            invalidate(this.networkByPipe.get(pipe.relative(dir)));
        }
    }

    public void pipeRemoved(BlockPos pos) {
        final BlockPos pipe = pos.immutable();
        invalidate(this.networkByPipe.remove(pipe));
        this.dirty.remove(pipe);
    }

    @Nullable
    public FluidNetwork networkAt(BlockPos pos) {
        rebuild();
        return this.networkByPipe.get(pos);
    }

    private void invalidate(@Nullable FluidNetwork network) {
        if (network == null || !network.isValid()) {
            return;
        }
        network.invalidate();
        this.networks.remove(network);
        for (BlockPos pipe : network.pipes()) {
            this.networkByPipe.remove(pipe);
            this.dirty.add(pipe);
        }
    }

    private void rebuild() {
        while (!this.dirty.isEmpty()) {
            final BlockPos start = this.dirty.iterator().next();
            this.dirty.remove(start);
            if (this.networkByPipe.containsKey(start) || FluidNetwork.stateIfPipe(this.level, start) == null) {
                continue;
            }
            final Set<BlockPos> pipes = new HashSet<>();
            final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            pipes.add(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                final BlockPos current = queue.poll();
                final BlockState currentState = FluidNetwork.stateIfPipe(this.level, current);
                for (Direction dir : Direction.values()) {
                    final BlockPos next = current.relative(dir);
                    final BlockState nextState = FluidNetwork.stateIfPipe(this.level, next);
                    if (nextState != null && !pipes.contains(next) && FluidPipeBlock.colorsConnect(
                            currentState.getValue(FluidPipeBlock.COLOR), nextState.getValue(FluidPipeBlock.COLOR))) {
                        pipes.add(next);
                        queue.add(next);
                    }
                }
            }
            for (BlockPos pipe : pipes) {
                invalidate(this.networkByPipe.get(pipe));
            }
            final FluidNetwork network = new FluidNetwork(this.level, pipes);
            this.networks.add(network);
            for (BlockPos pipe : pipes) {
                this.networkByPipe.put(pipe, network);
                this.dirty.remove(pipe);
            }
        }
    }

    private void tick() {
        rebuild();
        for (FluidNetwork network : new ArrayList<>(this.networks)) {
            // A capability lookup from another mod's pipe can rebuild networks mid-tick; skip the replaced ones.
            if (network.isValid()) {
                network.tick();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            final FluidNetworkManager manager = MANAGERS.get(serverLevel);
            if (manager != null) {
                manager.tick();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            MANAGERS.remove(serverLevel);
        }
    }
}
