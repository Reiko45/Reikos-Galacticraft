/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.network.PacketSimple S_NOCLIP_PLAYER). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.HydraulicPlatformBlock;
import io.github.reiko45.galacticraft.block.entity.HydraulicPlatformBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Hydraulic Platform lift: the client says when its player passes through a platform, so that the server lets them
 * (1.12.2 made the player noclip, or a spectator, for the ride).
 */
public final class PlatformNetwork {
    private PlatformNetwork() {
    }

    /** The platform (its north-west corner) the lift passes through, starting or done. */
    public record PlatformLift(BlockPos platform, boolean passing) implements CustomPacketPayload {
        public static final Type<PlatformLift> TYPE = new Type<>(GalacticraftCore.id("platform_lift"));
        public static final StreamCodec<ByteBuf, PlatformLift> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, PlatformLift::platform, ByteBufCodecs.BOOL, PlatformLift::passing, PlatformLift::new);

        @Override
        public Type<PlatformLift> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(PlatformLift.TYPE, PlatformLift.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            final var player = context.player();
            if (player.blockPosition().distSqr(payload.platform()) > 24 * 24 || !player.level().isLoaded(payload.platform())
                    || !(player.level().getBlockState(payload.platform()).getBlock() instanceof HydraulicPlatformBlock)
                    || !(player.level().getBlockEntity(payload.platform()) instanceof HydraulicPlatformBlockEntity platform)) {
                return;
            }
            platform.markNoCollide(payload.passing());
            if (!payload.passing()) {
                player.fallDistance = 0.0F;
            }
        }));
    }
}
