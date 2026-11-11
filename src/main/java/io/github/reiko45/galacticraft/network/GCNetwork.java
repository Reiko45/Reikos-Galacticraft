package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.ExtendedInventoryMenu;
import io.github.reiko45.galacticraft.player.GearData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Custom packets (1.12.2 PacketSimple entries C_AIR_REMAINING, C_UPDATE_OXYGEN_VALIDITY, C_UPDATE_GEAR_SLOT and the
 * inventory tab button). Client handlers live in client.ClientPayloads and are only reached through lambdas, so a
 * dedicated server never loads them.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GCNetwork {
    private GCNetwork() {
    }

    /** Client asks to open the Galacticraft inventory tab. */
    public record OpenExtendedInventory() implements CustomPacketPayload {
        public static final Type<OpenExtendedInventory> TYPE = new Type<>(GalacticraftCore.id("open_extended_inventory"));
        public static final StreamCodec<ByteBuf, OpenExtendedInventory> STREAM_CODEC = StreamCodec.unit(new OpenExtendedInventory());

        @Override
        public Type<OpenExtendedInventory> type() {
            return TYPE;
        }
    }

    /** The player's tank levels scaled to 0..90 for the HUD, and whether their oxygen setup works. */
    public record OxygenState(int air1, int air2, boolean valid) implements CustomPacketPayload {
        public static final Type<OxygenState> TYPE = new Type<>(GalacticraftCore.id("oxygen_state"));
        public static final StreamCodec<ByteBuf, OxygenState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OxygenState::air1, ByteBufCodecs.VAR_INT, OxygenState::air2, ByteBufCodecs.BOOL, OxygenState::valid,
                OxygenState::new);

        @Override
        public Type<OxygenState> type() {
            return TYPE;
        }
    }

    /** The gear a player wears, for drawing them. */
    public record GearSync(int entityId, GearData gear) implements CustomPacketPayload {
        public static final Type<GearSync> TYPE = new Type<>(GalacticraftCore.id("gear_sync"));
        public static final StreamCodec<ByteBuf, GearSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, GearSync::entityId, GearData.STREAM_CODEC, GearSync::gear, GearSync::new);

        @Override
        public Type<GearSync> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(OpenExtendedInventory.TYPE, OpenExtendedInventory.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ExtendedInventoryMenu.open(context.player())));
        registrar.playToClient(OxygenState.TYPE, OxygenState.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.ClientPayloads.oxygenState(payload)));
        registrar.playToClient(GearSync.TYPE, GearSync.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.ClientPayloads.gearSync(payload)));
    }
}
