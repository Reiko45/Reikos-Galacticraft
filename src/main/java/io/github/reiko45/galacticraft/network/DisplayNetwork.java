/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.network.PacketSimple C_UPDATE_TELEMETRY).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.TelemetryBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Telemetry Units tell nearby clients what they see, for Display Screens. */
public final class DisplayNetwork {
    private DisplayNetwork() {
    }

    /** 1.12.2 C_UPDATE_TELEMETRY: the unit, the linked entity's name ("" none, "$name" a player) and five numbers. */
    public record TelemetryData(BlockPos pos, String name, int data0, int data1, int data2, int data3, int data4, String uuid)
            implements CustomPacketPayload {
        public static final Type<TelemetryData> TYPE = new Type<>(GalacticraftCore.id("telemetry_data"));
        public static final StreamCodec<ByteBuf, TelemetryData> STREAM_CODEC = StreamCodec.of((buf, data) -> {
            final FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
            friendly.writeBlockPos(data.pos());
            friendly.writeUtf(data.name(), 256);
            friendly.writeVarInt(data.data0());
            friendly.writeVarInt(data.data1());
            friendly.writeVarInt(data.data2());
            friendly.writeVarInt(data.data3());
            friendly.writeVarInt(data.data4());
            friendly.writeUtf(data.uuid(), 64);
        }, buf -> {
            final FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
            return new TelemetryData(friendly.readBlockPos(), friendly.readUtf(256), friendly.readVarInt(), friendly.readVarInt(), friendly.readVarInt(),
                    friendly.readVarInt(), friendly.readVarInt(), friendly.readUtf(64));
        });

        @Override
        public Type<TelemetryData> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(TelemetryData.TYPE, TelemetryData.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player().level().isLoaded(payload.pos())
                    && context.player().level().getBlockEntity(payload.pos()) instanceof TelemetryBlockEntity telemetry) {
                telemetry.receive(payload);
            }
        }));
    }
}
