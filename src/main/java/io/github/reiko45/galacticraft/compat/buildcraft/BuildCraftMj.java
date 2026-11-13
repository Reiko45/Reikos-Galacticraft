/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler, EnergyUtil,
 * tile.TileBaseUniversalElectrical, TileBaseUniversalElectricalSource and TileBaseUniversalConductor, which supported
 * BuildCraft's MJ). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.compat.buildcraft;

import buildcraftreborn.api.mj.IMjConnector;
import buildcraftreborn.api.mj.IMjPassiveProvider;
import buildcraftreborn.api.mj.IMjReadable;
import buildcraftreborn.api.mj.IMjReceiver;
import buildcraftreborn.api.mj.MjAPI;
import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.energy.ForeignEnergy;
import io.github.reiko45.galacticraft.energy.GCEnergy;
import io.github.reiko45.galacticraft.energy.IGCEnergyHandler;
import io.github.reiko45.galacticraft.energy.network.EnergyNetwork;
import io.github.reiko45.galacticraft.energy.network.EnergyNetworkManager;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * BuildCraft's MJ, both ways, at 1.12.2's rate of 16 gJ to the MJ (which matches BuildCraft Reborn's 10 FE to the MJ
 * and Galacticraft's 1.6 gJ to the FE). Galacticraft machines take MJ on their energy inputs and offer a connection on
 * their outputs, and wires take it too (1.12.2 TileEntityAluminumWire was an IMjReceiver), so engines and kinesis pipes
 * can power them; machines and wires give power to BuildCraft's receivers (1.12.2 tried MJ before RF). Machines also
 * show their stored power to BuildCraft's readers. The server settings disableBuildCraftInput and
 * disableBuildCraftOutput turn either way off, as in 1.12.2.
 */
final class BuildCraftMj {
    /** Asked of a handler for "as much as it takes". */
    private static final float LARGE = 1.0E9F;

    private BuildCraftMj() {
    }

    static void init(IEventBus modBus) {
        ForeignEnergy.addFirst(new MjSystem());
        modBus.addListener(BuildCraftMj::registerCapabilities);
    }

    static long toMicroMj(float gJ) {
        return Math.max(0L, Math.round((double) gJ / GCEnergy.GJ_PER_MJ * MjAPI.MJ));
    }

    static float toGJ(long microMj) {
        return (float) ((double) microMj * GCEnergy.GJ_PER_MJ / MjAPI.MJ);
    }

    private static boolean inputOff() {
        return GCConfig.server(GCConfig.DISABLE_BUILDCRAFT_INPUT);
    }

    private static boolean outputOff() {
        return GCConfig.server(GCConfig.DISABLE_BUILDCRAFT_OUTPUT);
    }

    // ------------------------------------------------------------------ Galacticraft blocks as BuildCraft's

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type : GCBlockEntities.machineTypes()) {
            registerMachine(event, type.get());
        }
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, GCBlockEntities.TELEPAD_FAKE.get(), (fake, side) -> receiver(fake.energyHandler(side)));
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, GCBlockEntities.TELEPAD_FAKE.get(), (fake, side) -> connector(fake.energyHandler(side)));
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, GCBlockEntities.WIRE.get(),
                (wire, side) -> wire.getLevel() instanceof ServerLevel level ? new WireIntake(level, wire.getBlockPos()) : null);
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, GCBlockEntities.WIRE.get(),
                (wire, side) -> wire.getLevel() instanceof ServerLevel level ? new WireIntake(level, wire.getBlockPos()) : null);
    }

    private static <T extends MachineBlockEntity> void registerMachine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, type, (machine, side) -> receiver(machine.energyHandler(side)));
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, type, (machine, side) -> connector(machine.energyHandler(side)));
        event.registerBlockEntity(MjAPI.CAP_READABLE, type, (machine, side) -> new Readable(machine.energy()));
    }

    @Nullable
    private static IMjReceiver receiver(@Nullable IGCEnergyHandler handler) {
        return handler != null && handler.canReceive() ? new Receiver(handler) : null;
    }

    /** An energy input takes MJ; an output only connects (1.12.2 sources offered CAP_CONNECTOR on their outputs). */
    @Nullable
    private static IMjConnector connector(@Nullable IGCEnergyHandler handler) {
        if (handler == null) {
            return null;
        }
        return handler.canReceive() ? new Receiver(handler) : other -> true;
    }

    /** 1.12.2 TileBaseUniversalElectrical#getPowerRequested / #receivePower. */
    private record Receiver(IGCEnergyHandler handler) implements IMjReceiver, IMjReadable {
        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return inputOff() ? 0L : toMicroMj(this.handler.receiveGJ(LARGE, true));
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (inputOff() || microJoules <= 0L) {
                return microJoules;
            }
            final float accepted = this.handler.receiveGJ(toGJ(microJoules), simulate);
            return Math.max(0L, microJoules - toMicroMj(accepted));
        }

        @Override
        public boolean canReceive() {
            return !inputOff() && this.handler.canReceive();
        }

        @Override
        public long getStored() {
            return toMicroMj(this.handler.getStoredGJ());
        }

        @Override
        public long getCapacity() {
            return toMicroMj(this.handler.getCapacityGJ());
        }
    }

    private record Readable(IGCEnergyHandler handler) implements IMjReadable {
        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getStored() {
            return toMicroMj(this.handler.getStoredGJ());
        }

        @Override
        public long getCapacity() {
            return toMicroMj(this.handler.getCapacityGJ());
        }
    }

    /** 1.12.2 TileEntityAluminumWire as an IMjReceiver: an engine or kinesis pipe on a wire powers its network. */
    private record WireIntake(ServerLevel level, BlockPos pos) implements IMjReceiver {
        @Nullable
        private EnergyNetwork network() {
            return EnergyNetworkManager.get(this.level).networkAt(this.pos);
        }

        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            final EnergyNetwork network = network();
            return inputOff() || network == null ? 0L : toMicroMj(network.demand());
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            final EnergyNetwork network = network();
            if (inputOff() || network == null || microJoules <= 0L) {
                return microJoules;
            }
            return Math.max(0L, microJoules - toMicroMj(network.deliver(toGJ(microJoules), simulate)));
        }

        @Override
        public boolean canReceive() {
            return !inputOff();
        }
    }

    // ------------------------------------------------------------------ BuildCraft's blocks as Galacticraft's

    /** 1.12.2 EnergyUtil: BuildCraft receivers take power from machines and wires, passive providers give it. */
    private static final class MjSystem implements ForeignEnergy.System {
        @Override
        public @Nullable IGCEnergyHandler find(Level level, BlockPos pos, Direction side) {
            if (level.getBlockState(pos).getBlock() instanceof AluminumWireBlock) {
                return null; // Galacticraft's own wires are joined by their networks, not by MJ
            }
            return wrap(level.getCapability(MjAPI.CAP_RECEIVER, pos, side), level.getCapability(MjAPI.CAP_PASSIVE_PROVIDER, pos, side),
                    level.getCapability(MjAPI.CAP_CONNECTOR, pos, side));
        }

        @Override
        public Supplier<@Nullable IGCEnergyHandler> cache(ServerLevel level, BlockPos pos, Direction side, BooleanSupplier valid) {
            final BlockCapabilityCache<IMjReceiver, Direction> receiver = BlockCapabilityCache.create(MjAPI.CAP_RECEIVER, level, pos, side, valid, () -> { });
            final BlockCapabilityCache<IMjPassiveProvider, Direction> provider = BlockCapabilityCache.create(MjAPI.CAP_PASSIVE_PROVIDER, level, pos,
                    side, valid, () -> { });
            final BlockCapabilityCache<IMjConnector, Direction> connector = BlockCapabilityCache.create(MjAPI.CAP_CONNECTOR, level, pos, side, valid,
                    () -> { });
            return () -> level.getBlockState(pos).getBlock() instanceof AluminumWireBlock ? null
                    : wrap(receiver.getCapability(), provider.getCapability(), connector.getCapability());
        }

        @Nullable
        private static IGCEnergyHandler wrap(@Nullable IMjReceiver receiver, @Nullable IMjPassiveProvider provider, @Nullable IMjConnector connector) {
            if (receiver != null) {
                return new ToReceiver(receiver);
            }
            if (provider != null) {
                return new FromProvider(provider);
            }
            return connector != null ? Inert.INSTANCE : null;
        }
    }

    /** 1.12.2 EnergyUtil#otherModsEnergyTransfer: as much as the receiver asks for, MJ. */
    private record ToReceiver(IMjReceiver receiver) implements IGCEnergyHandler {
        @Override
        public float receiveGJ(float amount, boolean simulate) {
            if (outputOff()) {
                return 0.0F;
            }
            final long wanted = Math.min(toMicroMj(amount), this.receiver.getPowerRequested());
            if (wanted <= 0L) {
                return 0.0F;
            }
            return toGJ(wanted - this.receiver.receivePower(wanted, simulate));
        }

        @Override
        public float extractGJ(float amount, boolean simulate) {
            return 0.0F;
        }

        @Override
        public float getStoredGJ() {
            return 0.0F;
        }

        @Override
        public float getCapacityGJ() {
            return 0.0F;
        }

        @Override
        public boolean canReceive() {
            return !outputOff() && this.receiver.canReceive();
        }

        @Override
        public boolean canExtract() {
            return false;
        }
    }

    /** 1.12.2 EnergyUtil#otherModsEnergyExtract: pulled from a BuildCraft passive provider. */
    private record FromProvider(IMjPassiveProvider provider) implements IGCEnergyHandler {
        @Override
        public float receiveGJ(float amount, boolean simulate) {
            return 0.0F;
        }

        @Override
        public float extractGJ(float amount, boolean simulate) {
            return inputOff() ? 0.0F : toGJ(this.provider.extractPower(0L, toMicroMj(amount), simulate));
        }

        @Override
        public float getStoredGJ() {
            return 0.0F;
        }

        @Override
        public float getCapacityGJ() {
            return 0.0F;
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return !inputOff();
        }
    }

    /** A BuildCraft block that joins MJ up but neither takes nor gives here (an engine, a kinesis pipe): wires meet it. */
    private enum Inert implements IGCEnergyHandler {
        INSTANCE;

        @Override
        public float receiveGJ(float amount, boolean simulate) {
            return 0.0F;
        }

        @Override
        public float extractGJ(float amount, boolean simulate) {
            return 0.0F;
        }

        @Override
        public float getStoredGJ() {
            return 0.0F;
        }

        @Override
        public float getCapacityGJ() {
            return 0.0F;
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return false;
        }
    }
}
