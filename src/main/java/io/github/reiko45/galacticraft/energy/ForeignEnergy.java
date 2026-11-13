package io.github.reiko45.galacticraft.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' energy, as Galacticraft handlers, for machines pushing power out and for the wire networks (1.12.2
 * EnergyUtil#otherModCanReceive / otherModCanProduce). Forge Energy is always there; further systems (BuildCraft
 * Reborn's MJ) are added in front of it when their mod is, as 1.12.2 tried MJ before RF.
 */
public final class ForeignEnergy {
    /** One foreign energy system. */
    public interface System {
        /** A handler for a block's face, or null if the block there has none of this system's energy. */
        @Nullable
        IGCEnergyHandler find(Level level, BlockPos pos, Direction side);

        /** The same, cached, for a wire network's neighbour (valid while the network is). */
        Supplier<@Nullable IGCEnergyHandler> cache(ServerLevel level, BlockPos pos, Direction side, BooleanSupplier valid);
    }

    /** NeoForge's Forge Energy (1 FE = 1.6 gJ). */
    public static final System FORGE_ENERGY = new System() {
        @Override
        public @Nullable IGCEnergyHandler find(Level level, BlockPos pos, Direction side) {
            final IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            return storage != null ? GCEnergy.fromFE(storage) : null;
        }

        @Override
        public Supplier<@Nullable IGCEnergyHandler> cache(ServerLevel level, BlockPos pos, Direction side, BooleanSupplier valid) {
            final BlockCapabilityCache<IEnergyStorage, Direction> cache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, pos,
                    side, valid, () -> { });
            return () -> {
                final IEnergyStorage storage = cache.getCapability();
                return storage != null ? GCEnergy.fromFE(storage) : null;
            };
        }
    };

    private static final List<System> SYSTEMS = new CopyOnWriteArrayList<>(List.of(FORGE_ENERGY));

    private ForeignEnergy() {
    }

    /** Tried before the systems already there (BuildCraft's MJ goes in front of Forge Energy). */
    public static void addFirst(System system) {
        SYSTEMS.add(0, system);
    }

    /**
     * The first foreign handler on a block's face that takes or gives power; failing that, one that only joins up (a
     * BuildCraft engine, which wires still meet); or null.
     */
    @Nullable
    public static IGCEnergyHandler find(Level level, BlockPos pos, Direction side) {
        IGCEnergyHandler inert = null;
        for (System system : SYSTEMS) {
            final IGCEnergyHandler handler = system.find(level, pos, side);
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

    /** Cached lookups for every system, in order, for a wire network's neighbour. */
    public static List<Supplier<@Nullable IGCEnergyHandler>> caches(ServerLevel level, BlockPos pos, Direction side, BooleanSupplier valid) {
        final List<Supplier<@Nullable IGCEnergyHandler>> caches = new ArrayList<>(SYSTEMS.size());
        for (System system : SYSTEMS) {
            caches.add(system.cache(level, pos, side, valid));
        }
        return caches;
    }
}
