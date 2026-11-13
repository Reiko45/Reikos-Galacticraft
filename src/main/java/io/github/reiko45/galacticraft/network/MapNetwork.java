/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.network.PacketSimple S_REQUEST_OVERWORLD_IMAGE,
 * S_REQUEST_MAP_IMAGE and C_SEND_OVERWORLD_IMAGE). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.world.OverworldMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The Overworld pictures: asked for by a client, sent once the server has made them. */
public final class MapNetwork {
    /** The Earth's picture (the strip round 0, 0). */
    public static final int STRIP = 0;
    /** A Display Screen's local map. */
    public static final int LOCAL = 1;

    private MapNetwork() {
    }

    /** 1.12.2 S_REQUEST_OVERWORLD_IMAGE (kind STRIP) and S_REQUEST_MAP_IMAGE (kind LOCAL, round a screen). */
    public record RequestMap(int kind, BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestMap> TYPE = new Type<>(GalacticraftCore.id("request_map"));
        public static final StreamCodec<ByteBuf, RequestMap> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, RequestMap::kind,
                BlockPos.STREAM_CODEC, RequestMap::pos, RequestMap::new);

        @Override
        public Type<RequestMap> type() {
            return TYPE;
        }
    }

    /** 1.12.2 C_SEND_OVERWORLD_IMAGE: biome and height pairs (OverworldMap#sample); empty when there is no picture. */
    public record MapImage(int kind, BlockPos pos, byte[] data) implements CustomPacketPayload {
        public static final Type<MapImage> TYPE = new Type<>(GalacticraftCore.id("map_image"));
        public static final StreamCodec<ByteBuf, MapImage> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, MapImage::kind,
                BlockPos.STREAM_CODEC, MapImage::pos, ByteBufCodecs.BYTE_ARRAY, MapImage::data, MapImage::new);

        @Override
        public Type<MapImage> type() {
            return TYPE;
        }
    }

    public static void send(ServerPlayer player, MapImage image) {
        GCNetwork.sendToPlayer(player, image);
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(RequestMap.TYPE, RequestMap.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.kind() == STRIP) {
                    OverworldMap.requestStrip(player);
                } else if (payload.kind() == LOCAL) {
                    OverworldMap.requestLocal(player, payload.pos());
                }
            }
        }));
        registrar.playToClient(MapImage.TYPE, MapImage.STREAM_CODEC, (payload, context) -> context.enqueueWork(
                () -> io.github.reiko45.galacticraft.client.render.OverworldPictures.receive(payload)));
    }
}
