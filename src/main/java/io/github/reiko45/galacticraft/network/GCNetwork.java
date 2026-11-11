package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.footprint.Footprint;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.menu.CargoMenu;
import io.github.reiko45.galacticraft.menu.ExtendedInventoryMenu;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import io.github.reiko45.galacticraft.player.GearData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Custom packets (1.12.2 PacketSimple entries C_AIR_REMAINING, C_UPDATE_OXYGEN_VALIDITY, C_UPDATE_GEAR_SLOT and the
 * inventory tab button). Client handlers live in client.ClientPayloads and are only reached through lambdas, so a
 * dedicated server never loads them.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GCNetwork {
    /** Send to one player, skipping connections that never agreed on our channels (GameTest mock players). */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Send to the player and everyone who can see them. */
    public static void sendToTrackingAndSelf(ServerPlayer player, CustomPacketPayload payload) {
        sendToPlayer(player, payload);
        for (ServerPlayer watcher : player.serverLevel().getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player) {
                sendToPlayer(watcher, payload);
            }
        }
    }

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

    /** A space station on the destination screen. */
    public record StationInfo(int id, String name, String owner, boolean owned) {
        public static final StreamCodec<ByteBuf, StationInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StationInfo::id, ByteBufCodecs.STRING_UTF8, StationInfo::name, ByteBufCodecs.STRING_UTF8, StationInfo::owner,
                ByteBufCodecs.BOOL, StationInfo::owned, StationInfo::new);
    }

    /**
     * Server: open (or refresh) the destination screen for a rocket of this tier, with the space stations the player
     * may visit and whether they may build one now.
     */
    public record OpenCelestialSelection(int tier, java.util.List<StationInfo> stations, boolean canCreateStation) implements CustomPacketPayload {
        public static final Type<OpenCelestialSelection> TYPE = new Type<>(GalacticraftCore.id("open_celestial_selection"));
        public static final StreamCodec<ByteBuf, OpenCelestialSelection> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenCelestialSelection::tier, StationInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenCelestialSelection::stations,
                ByteBufCodecs.BOOL, OpenCelestialSelection::canCreateStation, OpenCelestialSelection::new);

        @Override
        public Type<OpenCelestialSelection> type() {
            return TYPE;
        }
    }

    /** Client: fly to this dimension (and in orbit, to this space station; -1 for none). */
    public record ChooseDestination(ResourceLocation dimension, int station) implements CustomPacketPayload {
        public static final Type<ChooseDestination> TYPE = new Type<>(GalacticraftCore.id("choose_destination"));
        public static final StreamCodec<ByteBuf, ChooseDestination> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, ChooseDestination::dimension, ByteBufCodecs.VAR_INT, ChooseDestination::station, ChooseDestination::new);

        public ChooseDestination(ResourceLocation dimension) {
            this(dimension, -1);
        }

        @Override
        public Type<ChooseDestination> type() {
            return TYPE;
        }
    }

    /** Client: the rider pressed jump in a rocket. */
    public record IgniteRocket() implements CustomPacketPayload {
        public static final Type<IgniteRocket> TYPE = new Type<>(GalacticraftCore.id("ignite_rocket"));
        public static final StreamCodec<ByteBuf, IgniteRocket> STREAM_CODEC = StreamCodec.unit(new IgniteRocket());

        @Override
        public Type<IgniteRocket> type() {
            return TYPE;
        }
    }

    /** Client: lander controls held this tick (LanderEntity.KEY_* bits). */
    public record LanderKeys(int keys) implements CustomPacketPayload {
        public static final Type<LanderKeys> TYPE = new Type<>(GalacticraftCore.id("lander_keys"));
        public static final StreamCodec<ByteBuf, LanderKeys> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, LanderKeys::keys, LanderKeys::new);

        @Override
        public Type<LanderKeys> type() {
            return TYPE;
        }
    }

    /** Client: open the cargo of the rocket being ridden (1.12.2 S_OPEN_FUEL_GUI, the inventory key in a rocket). */
    public record OpenRocketCargo() implements CustomPacketPayload {
        public static final Type<OpenRocketCargo> TYPE = new Type<>(GalacticraftCore.id("open_rocket_cargo"));
        public static final StreamCodec<ByteBuf, OpenRocketCargo> STREAM_CODEC = StreamCodec.unit(new OpenRocketCargo());

        @Override
        public Type<OpenRocketCargo> type() {
            return TYPE;
        }
    }

    /** A new footprint in a chunk the player can see. */
    public record FootprintAdded(long chunk, Footprint print) implements CustomPacketPayload {
        public static final Type<FootprintAdded> TYPE = new Type<>(GalacticraftCore.id("footprint_added"));
        public static final StreamCodec<ByteBuf, FootprintAdded> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, FootprintAdded::chunk, Footprint.STREAM_CODEC, FootprintAdded::print, FootprintAdded::new);

        @Override
        public Type<FootprintAdded> type() {
            return TYPE;
        }
    }

    /** All footprints of a chunk the player starts to see (1.12.2 C_UPDATE_FOOTPRINT_LIST). */
    public record FootprintChunk(long chunk, java.util.List<Footprint> prints) implements CustomPacketPayload {
        public static final Type<FootprintChunk> TYPE = new Type<>(GalacticraftCore.id("footprint_chunk"));
        public static final StreamCodec<ByteBuf, FootprintChunk> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, FootprintChunk::chunk, Footprint.STREAM_CODEC.apply(ByteBufCodecs.list()), FootprintChunk::prints,
                FootprintChunk::new);

        @Override
        public Type<FootprintChunk> type() {
            return TYPE;
        }
    }

    /** A space station's spin (1.12.2 C_UPDATE_STATION_SPIN, C_UPDATE_STATION_DATA and C_UPDATE_STATION_BOX). */
    public record StationSpinSync(int station, float angularVelocity, boolean firing, double centreX, double centreZ, int[] bounds)
            implements CustomPacketPayload {
        public static final Type<StationSpinSync> TYPE = new Type<>(GalacticraftCore.id("station_spin"));
        public static final StreamCodec<ByteBuf, StationSpinSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StationSpinSync::station, ByteBufCodecs.FLOAT, StationSpinSync::angularVelocity,
                ByteBufCodecs.BOOL, StationSpinSync::firing, ByteBufCodecs.DOUBLE, StationSpinSync::centreX, ByteBufCodecs.DOUBLE, StationSpinSync::centreZ,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).map(list -> list.stream().mapToInt(Integer::intValue).toArray(),
                        array -> java.util.Arrays.stream(array).boxed().toList()), StationSpinSync::bounds, StationSpinSync::new);

        @Override
        public Type<StationSpinSync> type() {
            return TYPE;
        }
    }

    /** Client: build a space station around Earth (1.12.2 S_BIND_SPACE_STATION_ID). */
    public record CreateStation() implements CustomPacketPayload {
        public static final Type<CreateStation> TYPE = new Type<>(GalacticraftCore.id("create_station"));
        public static final StreamCodec<ByteBuf, CreateStation> STREAM_CODEC = StreamCodec.unit(new CreateStation());

        @Override
        public Type<CreateStation> type() {
            return TYPE;
        }
    }

    /** Client: rename an owned station (1.12.2 S_RENAME_SPACE_STATION). */
    public record RenameStation(int station, String name) implements CustomPacketPayload {
        public static final Type<RenameStation> TYPE = new Type<>(GalacticraftCore.id("rename_station"));
        public static final StreamCodec<ByteBuf, RenameStation> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RenameStation::station, ByteBufCodecs.stringUtf8(32), RenameStation::name, RenameStation::new);

        @Override
        public Type<RenameStation> type() {
            return TYPE;
        }
    }

    /** Client: a NASA Workbench page button (Schematics.ACTION_*). */
    public record SchematicAction(int action) implements CustomPacketPayload {
        public static final Type<SchematicAction> TYPE = new Type<>(GalacticraftCore.id("schematic_action"));
        public static final StreamCodec<ByteBuf, SchematicAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SchematicAction::action, SchematicAction::new);

        @Override
        public Type<SchematicAction> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(OpenExtendedInventory.TYPE, OpenExtendedInventory.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ExtendedInventoryMenu.open(context.player())));
        registrar.playToClient(OpenCelestialSelection.TYPE, OpenCelestialSelection.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.ClientPayloads.openCelestialSelection(payload)));
        registrar.playToServer(ChooseDestination.TYPE, ChooseDestination.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceTravel.choose(player, ResourceKey.create(Registries.DIMENSION, payload.dimension()), payload.station());
            }
        }));
        registrar.playToServer(CreateStation.TYPE, CreateStation.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceTravel.createStation(player);
            }
        }));
        registrar.playToServer(RenameStation.TYPE, RenameStation.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceTravel.renameStation(player, payload.station(), payload.name());
            }
        }));
        registrar.playToServer(IgniteRocket.TYPE, IgniteRocket.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceTravel.ignite(player);
            }
        }));
        registrar.playToServer(LanderKeys.TYPE, LanderKeys.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player().getVehicle() instanceof LanderEntity lander) {
                lander.setKeys(payload.keys());
            }
        }));
        registrar.playToServer(OpenRocketCargo.TYPE, OpenRocketCargo.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof RocketEntity rocket) {
                CargoMenu.openRocket(player, rocket);
            } else if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof io.github.reiko45.galacticraft.entity.BuggyEntity buggy) {
                CargoMenu.openBuggy(player, buggy);
            }
        }));
        registrar.playToServer(SchematicAction.TYPE, SchematicAction.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                io.github.reiko45.galacticraft.menu.Schematics.action(player, payload.action());
            }
        }));
        registrar.playToClient(StationSpinSync.TYPE, StationSpinSync.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.world.station.StationSpin.Client.update(payload)));
        registrar.playToClient(OxygenState.TYPE, OxygenState.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.ClientPayloads.oxygenState(payload)));
        registrar.playToClient(FootprintAdded.TYPE, FootprintAdded.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.render.FootprintRenderer.add(payload.chunk(), payload.print())));
        registrar.playToClient(FootprintChunk.TYPE, FootprintChunk.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.render.FootprintRenderer.set(payload.chunk(), payload.prints())));
        registrar.playToClient(GearSync.TYPE, GearSync.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.ClientPayloads.gearSync(payload)));
    }
}
