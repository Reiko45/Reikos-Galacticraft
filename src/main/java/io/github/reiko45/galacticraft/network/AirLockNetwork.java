package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.airlock.AirLockControllerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The Air Lock Controller's settings screen: opened by the server with the settings, changed one at a time by the owner. */
public final class AirLockNetwork {
    private AirLockNetwork() {
    }

    /** Server: open a controller's screen (1.12.2 opened it on the client, GUI id -1). */
    public record OpenAirLock(BlockPos pos, CompoundTag settings) implements CustomPacketPayload {
        public static final Type<OpenAirLock> TYPE = new Type<>(GalacticraftCore.id("open_air_lock"));
        public static final StreamCodec<ByteBuf, OpenAirLock> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenAirLock::pos, ByteBufCodecs.COMPOUND_TAG, OpenAirLock::settings, OpenAirLock::new);

        @Override
        public Type<OpenAirLock> type() {
            return TYPE;
        }
    }

    /** Client: one setting changed (AirLockControllerBlockEntity.SET_*). */
    public record AirLockSetting(BlockPos pos, int setting, int value, String text) implements CustomPacketPayload {
        public static final Type<AirLockSetting> TYPE = new Type<>(GalacticraftCore.id("air_lock_setting"));
        public static final StreamCodec<ByteBuf, AirLockSetting> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, AirLockSetting::pos, ByteBufCodecs.VAR_INT, AirLockSetting::setting, ByteBufCodecs.VAR_INT, AirLockSetting::value,
                ByteBufCodecs.stringUtf8(64), AirLockSetting::text, AirLockSetting::new);

        @Override
        public Type<AirLockSetting> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(OpenAirLock.TYPE, OpenAirLock.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.ClientPayloads.openAirLock(payload)));
        registrar.playToServer(AirLockSetting.TYPE, AirLockSetting.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.level().isLoaded(payload.pos())
                    && player.level().getBlockEntity(payload.pos()) instanceof AirLockControllerBlockEntity controller) {
                controller.applySetting(player, payload.setting(), payload.value(), payload.text());
            }
        }));
    }
}
