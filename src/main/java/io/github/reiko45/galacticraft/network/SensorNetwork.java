/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.network.PacketSimple S_REQUEST_MACHINE_DATA and
 * C_LEAK_DATA). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.item.SensorGlassesItem;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Sensor Glasses: the client asks the nearest sealer where its air gets out, and is told the way. */
public final class SensorNetwork {
    private SensorNetwork() {
    }

    /** Client: the sealer whose leak to show (1.12.2 S_REQUEST_MACHINE_DATA). */
    public record RequestLeakTrace(BlockPos sealer) implements CustomPacketPayload {
        public static final Type<RequestLeakTrace> TYPE = new Type<>(GalacticraftCore.id("request_leak_trace"));
        public static final StreamCodec<ByteBuf, RequestLeakTrace> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, RequestLeakTrace::sealer, RequestLeakTrace::new);

        @Override
        public Type<RequestLeakTrace> type() {
            return TYPE;
        }
    }

    /** Server: the way from the sealer's vent to the leak, empty when sealed (1.12.2 C_LEAK_DATA). */
    public record LeakTrace(BlockPos sealer, List<BlockPos> trace) implements CustomPacketPayload {
        public static final Type<LeakTrace> TYPE = new Type<>(GalacticraftCore.id("leak_trace"));
        public static final StreamCodec<ByteBuf, LeakTrace> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, LeakTrace::sealer, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(128)), LeakTrace::trace, LeakTrace::new);

        @Override
        public Type<LeakTrace> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(RequestLeakTrace.TYPE, RequestLeakTrace.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            final var player = context.player();
            if (!SensorGlassesItem.isWorn(player) || player.blockPosition().distSqr(payload.sealer()) > 96.0D * 96.0D
                    || !player.level().isLoaded(payload.sealer())
                    || !(player.level().getBlockEntity(payload.sealer()) instanceof OxygenSealerBlockEntity sealer)) {
                return;
            }
            final List<BlockPos> trace = sealer.leakTrace();
            context.reply(new LeakTrace(payload.sealer(), trace.size() > 128 ? trace.subList(0, 128) : trace));
        }));
        registrar.playToClient(LeakTrace.TYPE, LeakTrace.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.SensorGlasses.setLeakTrace(payload.sealer(), payload.trace())));
    }
}
