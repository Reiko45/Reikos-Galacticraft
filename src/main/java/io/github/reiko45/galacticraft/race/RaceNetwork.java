/*
 * Derived from Galacticraft Legacy (the space race messages of micdoodle8.mods.galacticraft.core.network.PacketSimple:
 * S_START_NEW_SPACE_RACE, S_INVITE_RACE_PLAYER, S_REMOVE_RACE_PLAYER, S_ADD_RACE_PLAYER, C_UPDATE_SPACE_RACE_DATA,
 * C_OPEN_JOIN_RACE_GUI and C_OPEN_SPACE_RACE_GUI). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.race;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The Space Race messages: every team is sent to every player, and the owner's screens send their changes. */
public final class RaceNetwork {
    private RaceNetwork() {
    }

    /** 1.12.2 C_UPDATE_SPACE_RACE_DATA: one team as the clients see it. */
    public record RaceData(int id, String name, FlagData flag, int color, List<UUID> memberIds, List<String> members, int ticksSpent)
            implements CustomPacketPayload {
        public static final Type<RaceData> TYPE = new Type<>(GalacticraftCore.id("space_race_data"));
        public static final StreamCodec<ByteBuf, RaceData> STREAM_CODEC = StreamCodec.of((buf, data) -> {
            ByteBufCodecs.VAR_INT.encode(buf, data.id);
            ByteBufCodecs.STRING_UTF8.encode(buf, data.name);
            FlagData.STREAM_CODEC.encode(buf, data.flag);
            ByteBufCodecs.INT.encode(buf, data.color);
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(256)).encode(buf, data.memberIds);
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)).encode(buf, data.members);
            ByteBufCodecs.VAR_INT.encode(buf, data.ticksSpent);
        }, buf -> new RaceData(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), FlagData.STREAM_CODEC.decode(buf),
                ByteBufCodecs.INT.decode(buf), UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(256)).decode(buf),
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)).decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

        public static RaceData of(SpaceRace race) {
            final List<UUID> ids = new ArrayList<>();
            final List<String> names = new ArrayList<>();
            for (SpaceRace.Member member : race.members()) {
                ids.add(member.id());
                names.add(member.name());
            }
            return new RaceData(race.id(), race.name(), race.flag().copy(), race.color(), ids, names, race.ticksSpent());
        }

        public boolean isOwner(UUID player) {
            return !this.memberIds.isEmpty() && this.memberIds.get(0).equals(player);
        }

        @Override
        public Type<RaceData> type() {
            return TYPE;
        }
    }

    /** A team that no longer exists (its last member joined another). */
    public record RaceRemoved(int id) implements CustomPacketPayload {
        public static final Type<RaceRemoved> TYPE = new Type<>(GalacticraftCore.id("space_race_removed"));
        public static final StreamCodec<ByteBuf, RaceRemoved> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(RaceRemoved::new, RaceRemoved::id);

        @Override
        public Type<RaceRemoved> type() {
            return TYPE;
        }
    }

    /** 1.12.2 S_START_NEW_SPACE_RACE from the manager: the owner's name, flag and colour (a team is made if needed). */
    public record UpdateRace(String name, FlagData flag, int color) implements CustomPacketPayload {
        public static final Type<UpdateRace> TYPE = new Type<>(GalacticraftCore.id("space_race_update"));
        public static final StreamCodec<ByteBuf, UpdateRace> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), UpdateRace::name, FlagData.STREAM_CODEC, UpdateRace::flag, ByteBufCodecs.INT, UpdateRace::color, UpdateRace::new);

        @Override
        public Type<UpdateRace> type() {
            return TYPE;
        }
    }

    /** S_INVITE_RACE_PLAYER (true) and S_REMOVE_RACE_PLAYER (false). */
    public record RacePlayer(String name, boolean invite) implements CustomPacketPayload {
        public static final Type<RacePlayer> TYPE = new Type<>(GalacticraftCore.id("space_race_player"));
        public static final StreamCodec<ByteBuf, RacePlayer> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(16), RacePlayer::name, ByteBufCodecs.BOOL, RacePlayer::invite, RacePlayer::new);

        @Override
        public Type<RacePlayer> type() {
            return TYPE;
        }
    }

    /** S_ADD_RACE_PLAYER from the join screen. */
    public record JoinRace(int id) implements CustomPacketPayload {
        public static final Type<JoinRace> TYPE = new Type<>(GalacticraftCore.id("space_race_join"));
        public static final StreamCodec<ByteBuf, JoinRace> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(JoinRace::new, JoinRace::id);

        @Override
        public Type<JoinRace> type() {
            return TYPE;
        }
    }

    /** C_OPEN_JOIN_RACE_GUI (a team id) or C_OPEN_SPACE_RACE_GUI (0): open the join screen or the manager. */
    public record OpenRaceScreen(int joinId) implements CustomPacketPayload {
        public static final Type<OpenRaceScreen> TYPE = new Type<>(GalacticraftCore.id("space_race_open"));
        public static final StreamCodec<ByteBuf, OpenRaceScreen> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(OpenRaceScreen::new, OpenRaceScreen::joinId);

        @Override
        public Type<OpenRaceScreen> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(RaceData.TYPE, RaceData.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.race.ClientSpaceRaces.update(payload)));
        registrar.playToClient(RaceRemoved.TYPE, RaceRemoved.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.race.ClientSpaceRaces.remove(payload.id())));
        registrar.playToClient(OpenRaceScreen.TYPE, OpenRaceScreen.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> io.github.reiko45.galacticraft.client.race.ClientSpaceRaces.open(payload.joinId())));
        registrar.playToServer(UpdateRace.TYPE, UpdateRace.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceRaces.get(player.server).update(player, payload.name(), payload.flag(), payload.color());
            }
        }));
        registrar.playToServer(RacePlayer.TYPE, RacePlayer.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                final SpaceRaces races = SpaceRaces.get(player.server);
                if (payload.invite()) {
                    races.invite(player, payload.name());
                } else {
                    races.remove(player, payload.name());
                }
            }
        }));
        registrar.playToServer(JoinRace.TYPE, JoinRace.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceRaces.get(player.server).join(player, payload.id());
            }
        }));
    }

    static void send(ServerPlayer player, SpaceRace race) {
        GCNetwork.sendToPlayer(player, RaceData.of(race));
    }

    static void removed(MinecraftServer server, int id) {
        final RaceRemoved payload = new RaceRemoved(id);
        server.getPlayerList().getPlayers().forEach(player -> GCNetwork.sendToPlayer(player, payload));
    }

    public static void openManager(ServerPlayer player) {
        GCNetwork.sendToPlayer(player, new OpenRaceScreen(0));
    }

    public static void openJoin(ServerPlayer player, int id) {
        GCNetwork.sendToPlayer(player, new OpenRaceScreen(id));
    }
}
