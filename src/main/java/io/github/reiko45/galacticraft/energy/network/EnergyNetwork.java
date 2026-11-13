package io.github.reiko45.galacticraft.energy.network;

import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.energy.GCEnergy;
import io.github.reiko45.galacticraft.energy.IGCEnergyHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

/**
 * A connected set of wires and the energy handlers touching it (1.12.2 EnergyNetwork, simplified).
 *
 * <p>Each tick every source offers what it can extract and every sink asks for what it can accept; energy then
 * moves from sources to sinks, never from a block into itself (a storage module whose input and output faces
 * touch the same network). Like 1.12.2 there is no throughput limit on the wires, but energy is delivered at the
 * lower of the source's tier and the weakest wire's tier, which sets how fast tiered machines run. The start index rotates
 * every tick so that several machines on a weak generator share fairly over time.
 */
public final class EnergyNetwork {
    private static final float LARGE = 1.0E9F;

    private final Set<BlockPos> wires;
    private final List<Endpoint> endpoints = new ArrayList<>();
    private final int tier;
    private boolean valid = true;
    private int rotation;

    private record Endpoint(BlockPos pos, BlockCapabilityCache<IGCEnergyHandler, Direction> gc,
                            List<Supplier<@Nullable IGCEnergyHandler>> foreign) {
        @Nullable
        IGCEnergyHandler handler() {
            final IGCEnergyHandler native_ = this.gc.getCapability();
            if (native_ != null) {
                return native_;
            }
            IGCEnergyHandler inert = null;
            for (Supplier<@Nullable IGCEnergyHandler> system : this.foreign) {
                final IGCEnergyHandler handler = system.get();
                if (handler != null) {
                    if (handler.canReceive() || handler.canExtract()) {
                        return handler;
                    }
                    if (inert == null) {
                        inert = handler;
                    }
                }
            }
            return inert;
        }
    }

    EnergyNetwork(ServerLevel level, Set<BlockPos> wires) {
        this.wires = wires;
        int lowest = Integer.MAX_VALUE;
        for (BlockPos wire : wires) {
            if (level.getBlockState(wire).getBlock() instanceof AluminumWireBlock block) {
                lowest = Math.min(lowest, block.tier());
            }
        }
        this.tier = lowest == Integer.MAX_VALUE ? 1 : lowest;
        for (BlockPos wire : wires) {
            for (Direction dir : Direction.values()) {
                final BlockPos neighbor = wire.relative(dir);
                if (!wires.contains(neighbor)) {
                    final Direction face = dir.getOpposite();
                    this.endpoints.add(new Endpoint(neighbor,
                            BlockCapabilityCache.create(GCEnergy.BLOCK, level, neighbor, face, () -> this.valid, () -> { }),
                            io.github.reiko45.galacticraft.energy.ForeignEnergy.caches(level, neighbor, face, () -> this.valid)));
                }
            }
        }
    }

    /** The weakest wire's tier. */
    public int tier() {
        return this.tier;
    }

    public Set<BlockPos> wires() {
        return this.wires;
    }

    boolean isValid() {
        return this.valid;
    }

    void invalidate() {
        this.valid = false;
    }

    /**
     * What the network's receivers could take now, for power arriving from outside on a wire (1.12.2 TileEntityAluminumWire
     * as a BuildCraft IMjReceiver).
     */
    public float demand() {
        float total = 0.0F;
        final Set<BlockPos> seen = new HashSet<>();
        for (Endpoint endpoint : this.endpoints) {
            final IGCEnergyHandler handler = endpoint.handler();
            if (handler != null && handler.canReceive() && seen.add(endpoint.pos())) {
                total += handler.receiveGJ(LARGE, true);
            }
        }
        return total;
    }

    /** Power arriving from outside on a wire, shared out to the receivers in turn. @return gJ they took */
    public float deliver(float amount, boolean simulate) {
        float left = amount;
        final Set<BlockPos> seen = new HashSet<>();
        final int count = this.endpoints.size();
        for (int a = 0; a < count && left > 0.0F; a++) {
            final Endpoint endpoint = this.endpoints.get(Math.floorMod(a + this.rotation, count));
            final IGCEnergyHandler handler = endpoint.handler();
            if (handler == null || !handler.canReceive() || !seen.add(endpoint.pos())) {
                continue;
            }
            final float accepted = handler.receiveGJ(left, simulate);
            if (accepted > 0.0F) {
                left -= accepted;
                if (!simulate) {
                    handler.setPoweredTier(1);
                }
            }
        }
        return amount - left;
    }

    /** @return total gJ moved this tick */
    float tick() {
        final List<BlockPos> sourcePos = new ArrayList<>();
        final List<IGCEnergyHandler> sources = new ArrayList<>();
        final List<Float> budgets = new ArrayList<>();
        final List<BlockPos> sinkPos = new ArrayList<>();
        final List<IGCEnergyHandler> sinks = new ArrayList<>();
        final List<Float> needs = new ArrayList<>();
        final Set<BlockPos> seenSources = new HashSet<>();
        final Set<BlockPos> seenSinks = new HashSet<>();

        for (Endpoint endpoint : this.endpoints) {
            final IGCEnergyHandler handler = endpoint.handler();
            if (handler == null) {
                continue;
            }
            // Budgets are taken once per block per tick so per-call limits act as per-tick limits, as in 1.12.2.
            if (handler.canExtract() && !seenSources.contains(endpoint.pos())) {
                final float offer = handler.extractGJ(LARGE, true);
                if (offer > 0.0F) {
                    seenSources.add(endpoint.pos());
                    sourcePos.add(endpoint.pos());
                    sources.add(handler);
                    budgets.add(offer);
                }
            }
            if (handler.canReceive() && !seenSinks.contains(endpoint.pos())) {
                final float need = handler.receiveGJ(LARGE, true);
                if (need > 0.0F) {
                    seenSinks.add(endpoint.pos());
                    sinkPos.add(endpoint.pos());
                    sinks.add(handler);
                    needs.add(need);
                }
            }
        }
        if (sources.isEmpty() || sinks.isEmpty()) {
            return 0.0F;
        }

        float moved = 0.0F;
        this.rotation++;
        for (int a = 0; a < sinks.size(); a++) {
            final int i = Math.floorMod(a + this.rotation, sinks.size());
            int deliveredTier = 0;
            for (int b = 0; b < sources.size() && needs.get(i) > 0.0F; b++) {
                final int j = Math.floorMod(b + this.rotation, sources.size());
                if (sourcePos.get(j).equals(sinkPos.get(i))) {
                    continue;
                }
                final float amount = Math.min(needs.get(i), budgets.get(j));
                if (amount <= 0.0F) {
                    continue;
                }
                final float taken = sources.get(j).extractGJ(amount, false);
                final float accepted = sinks.get(i).receiveGJ(taken, false);
                budgets.set(j, budgets.get(j) - taken);
                needs.set(i, needs.get(i) - accepted);
                moved += accepted;
                if (accepted > 0.0F) {
                    deliveredTier = Math.max(deliveredTier, Math.min(sources.get(j).tier(), this.tier));
                }
            }
            if (deliveredTier > 0) {
                sinks.get(i).setPoweredTier(deliveredTier);
            }
        }
        return moved;
    }
}
