/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.network.PacketSimpleVenus:
 * S_UPDATE_ADVANCED_GUI, S_OPEN_LASER_TURRET_GUI, S_MODIFY_LASER_TARGET). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The Laser Turret's settings, sent by its owner (1.12.2 trusted any player who sent them). */
public final class TurretNetwork {
    public static final int BLACKLIST = 0;
    public static final int METEORS = 1;
    public static final int IGNORE_TEAM = 2;

    public static final int ADD_PLAYER = 0;
    public static final int ADD_ENTITY = 1;
    public static final int REMOVE_PLAYER = 2;
    public static final int REMOVE_ENTITY = 3;

    private TurretNetwork() {
    }

    /** 1.12.2 S_UPDATE_ADVANCED_GUI 0-2: blacklist mode, target meteors, never fire at the team. */
    public record Setting(BlockPos pos, int setting, boolean value) implements CustomPacketPayload {
        public static final Type<Setting> TYPE = new Type<>(GalacticraftCore.id("turret_setting"));
        public static final StreamCodec<ByteBuf, Setting> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Setting::pos, ByteBufCodecs.VAR_INT, Setting::setting, ByteBufCodecs.BOOL, Setting::value, Setting::new);

        @Override
        public Type<Setting> type() {
            return TYPE;
        }
    }

    /** 1.12.2 S_UPDATE_ADVANCED_GUI 3-5: the three priorities together. */
    public record Priorities(BlockPos pos, int closest, int lowestHealth, int highestHealth) implements CustomPacketPayload {
        public static final Type<Priorities> TYPE = new Type<>(GalacticraftCore.id("turret_priorities"));
        public static final StreamCodec<ByteBuf, Priorities> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Priorities::pos, ByteBufCodecs.VAR_INT, Priorities::closest, ByteBufCodecs.VAR_INT, Priorities::lowestHealth,
                ByteBufCodecs.VAR_INT, Priorities::highestHealth, Priorities::new);

        @Override
        public Type<Priorities> type() {
            return TYPE;
        }
    }

    /** 1.12.2 S_MODIFY_LASER_TARGET: add or remove a player name or an entity id. */
    public record Target(BlockPos pos, int operation, String name) implements CustomPacketPayload {
        public static final Type<Target> TYPE = new Type<>(GalacticraftCore.id("turret_target"));
        public static final StreamCodec<ByteBuf, Target> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Target::pos, ByteBufCodecs.VAR_INT, Target::operation, ByteBufCodecs.stringUtf8(LaserTurretBlockEntity.MAX_NAME),
                Target::name, Target::new);

        @Override
        public Type<Target> type() {
            return TYPE;
        }
    }

    /** 1.12.2 S_OPEN_LASER_TURRET_GUI: back from the list or priority screen to the turret's own. */
    public record Open(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(GalacticraftCore.id("turret_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Open::pos, Open::new);

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /** The turret at {@code pos} if {@code player} can reach it (and, to change it, owns it). */
    @Nullable
    private static LaserTurretBlockEntity turret(Player player, BlockPos pos, boolean owner) {
        if (!player.level().isLoaded(pos) || !player.canInteractWithBlock(pos, 4.0D)
                || !(player.level().getBlockEntity(pos) instanceof LaserTurretBlockEntity turret)) {
            return null;
        }
        return owner && !turret.isOwner(player) ? null : turret;
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Setting.TYPE, Setting.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            final LaserTurretBlockEntity turret = turret(context.player(), payload.pos(), true);
            if (turret != null) {
                switch (payload.setting()) {
                    case BLACKLIST -> turret.setBlacklistMode(payload.value());
                    case METEORS -> turret.setTargetMeteors(payload.value());
                    case IGNORE_TEAM -> turret.setAlwaysIgnoreSpaceRace(payload.value());
                    default -> {
                    }
                }
            }
        }));
        registrar.playToServer(Priorities.TYPE, Priorities.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            final LaserTurretBlockEntity turret = turret(context.player(), payload.pos(), true);
            if (turret != null && payload.closest() >= 0 && payload.lowestHealth() >= 0 && payload.highestHealth() >= 0) {
                turret.setPriorities(payload.closest(), payload.lowestHealth(), payload.highestHealth());
            }
        }));
        registrar.playToServer(Target.TYPE, Target.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            final LaserTurretBlockEntity turret = turret(context.player(), payload.pos(), true);
            final String name = payload.name().trim();
            if (turret == null || name.isEmpty()) {
                return;
            }
            switch (payload.operation()) {
                case ADD_PLAYER -> turret.addPlayer(name);
                case REMOVE_PLAYER -> turret.removePlayer(name);
                case ADD_ENTITY, REMOVE_ENTITY -> {
                    final ResourceLocation type = ResourceLocation.tryParse(name);
                    if (type != null && payload.operation() == REMOVE_ENTITY) {
                        turret.removeEntity(type);
                    } else if (type != null && BuiltInRegistries.ENTITY_TYPE.containsKey(type)) {
                        turret.addEntity(type);
                    }
                }
                default -> {
                }
            }
        }));
        registrar.playToServer(Open.TYPE, Open.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            final LaserTurretBlockEntity turret = turret(context.player(), payload.pos(), false);
            if (turret != null && context.player() instanceof ServerPlayer player) {
                turret.openMenu(player);
            }
        }));
    }
}
