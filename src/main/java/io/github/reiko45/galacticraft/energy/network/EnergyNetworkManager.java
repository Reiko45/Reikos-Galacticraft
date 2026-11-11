package io.github.reiko45.galacticraft.energy.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.AluminumWireBlock;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks wire networks per level (1.12.2 NetworkFinder + EnergyNetwork bookkeeping). Wires report themselves
 * when loaded or removed; affected networks are dropped and rebuilt lazily by flood fill at the end of the tick.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class EnergyNetworkManager {
    private static final Map<ServerLevel, EnergyNetworkManager> MANAGERS = new WeakHashMap<>();

    private final ServerLevel level;
    private final Map<BlockPos, EnergyNetwork> networkByWire = new HashMap<>();
    private final Set<BlockPos> dirty = new LinkedHashSet<>();
    private final Set<EnergyNetwork> networks = new LinkedHashSet<>();

    private EnergyNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static EnergyNetworkManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, EnergyNetworkManager::new);
    }

    public void wireAdded(BlockPos pos) {
        final BlockPos wire = pos.immutable();
        this.dirty.add(wire);
        for (Direction dir : Direction.values()) {
            invalidate(this.networkByWire.get(wire.relative(dir)));
        }
    }

    public void wireRemoved(BlockPos pos) {
        final BlockPos wire = pos.immutable();
        invalidate(this.networkByWire.remove(wire));
        this.dirty.remove(wire);
    }

    @Nullable
    public EnergyNetwork networkAt(BlockPos pos) {
        rebuild();
        return this.networkByWire.get(pos);
    }

    private void invalidate(@Nullable EnergyNetwork network) {
        if (network == null || !network.isValid()) {
            return;
        }
        network.invalidate();
        this.networks.remove(network);
        for (BlockPos wire : network.wires()) {
            this.networkByWire.remove(wire);
            this.dirty.add(wire);
        }
    }

    private void rebuild() {
        while (!this.dirty.isEmpty()) {
            final BlockPos start = this.dirty.iterator().next();
            this.dirty.remove(start);
            if (this.networkByWire.containsKey(start) || !isLoadedWire(start)) {
                continue;
            }
            final Set<BlockPos> wires = new HashSet<>();
            final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            wires.add(start);
            while (!queue.isEmpty()) {
                final BlockPos current = queue.poll();
                for (Direction dir : Direction.values()) {
                    final BlockPos next = current.relative(dir);
                    if (!wires.contains(next) && isLoadedWire(next)) {
                        wires.add(next);
                        queue.add(next);
                    }
                }
            }
            for (BlockPos wire : wires) {
                invalidate(this.networkByWire.get(wire));
            }
            final EnergyNetwork network = new EnergyNetwork(this.level, wires);
            this.networks.add(network);
            for (BlockPos wire : wires) {
                this.networkByWire.put(wire, network);
                this.dirty.remove(wire);
            }
        }
    }

    private boolean isLoadedWire(BlockPos pos) {
        if (!this.level.isLoaded(pos)) {
            return false;
        }
        final net.minecraft.world.level.block.state.BlockState state = this.level.getBlockState(pos);
        return state.getBlock() instanceof AluminumWireBlock wire && wire.isConductive(state);
    }

    private void tick() {
        rebuild();
        for (EnergyNetwork network : new ArrayList<>(this.networks)) {
            network.tick();
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            final EnergyNetworkManager manager = MANAGERS.get(serverLevel);
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
