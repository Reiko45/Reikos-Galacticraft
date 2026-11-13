/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.network.PacketSimple C_RECOLOR_ALL_GLASS and
 * C_UPDATE_STATS' panel colour). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The Chromatic Applicator's colours a player sees: the three kinds of Space Glass and the Lighting Panels. */
public final class PaintNetwork {
    private PaintNetwork() {
    }

    public record PaintColors(int glass1, int glass2, int glass3, int panel) implements CustomPacketPayload {
        public static final Type<PaintColors> TYPE = new Type<>(GalacticraftCore.id("paint_colors"));
        public static final StreamCodec<ByteBuf, PaintColors> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, PaintColors::glass1,
                ByteBufCodecs.INT, PaintColors::glass2, ByteBufCodecs.INT, PaintColors::glass3, ByteBufCodecs.INT, PaintColors::panel, PaintColors::new);

        @Override
        public Type<PaintColors> type() {
            return TYPE;
        }
    }

    /** 1.12.2 dominantToPlayer: tell the player only when the colours change. */
    public static void setGlassColors(ServerPlayer player, int color1, int color2, int color3) {
        if (GCPlayerData.get(player).setGlassColors(color1, color2, color3)) {
            sendColors(player);
        }
    }

    /** 1.12.2 sendUpdatedColorsToPlayer (also on joining). */
    public static void sendColors(ServerPlayer player) {
        final GCPlayerData data = GCPlayerData.get(player);
        GCNetwork.sendToPlayer(player, new PaintColors(data.glassColor(0), data.glassColor(1), data.glassColor(2), data.panelLightingColor()));
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(PaintColors.TYPE, PaintColors.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.PaintColors.set(payload)));
    }
}
