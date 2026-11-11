package io.github.reiko45.galacticraft.travel;

import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.ParachestEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.ParachuteItem;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.station.SpaceStations;
import io.github.reiko45.galacticraft.world.station.StationRecipe;
import net.minecraft.core.BlockPos;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Rocket travel between worlds (1.12.2 WorldUtil#toCelestialSelection / #transferEntityToDimension with
 * TeleportTypeOverworld and TeleportTypeMoon, and the travel parts of GCPlayerHandler).
 *
 * <p>A rocket that climbs above y 1200 hands its cargo, fuel, launch pads and itself to the rider, who stays in place
 * while choosing a destination. On the Moon the player arrives at y 900 in a Space Lander holding all of it. On Earth
 * the player arrives at y 250 (falling slowly under their parachute if they wear one), and a Parachest with the cargo
 * floats down from y 230 nearby 20 ticks later.
 */
public final class SpaceTravel {
    /** A world a rocket can fly to. */
    public record Destination(ResourceKey<Level> dimension, String nameKey, int tier) {
    }

    public static final List<Destination> DESTINATIONS = List.of(
            new Destination(Level.OVERWORLD, "planet.galacticraftcore.overworld", 1),
            new Destination(GCDimensions.MOON, "moon.galacticraftcore.moon", 1),
            new Destination(GCDimensions.ORBIT, "satellite.galacticraftcore.overworld_station", 1));

    private SpaceTravel() {
    }

    @Nullable
    public static Destination destination(ResourceKey<Level> dimension) {
        for (Destination destination : DESTINATIONS) {
            if (destination.dimension().equals(dimension)) {
                return destination;
            }
        }
        return null;
    }

    /** 1.12.2 EntityAutoRocket#onLaunch for the rider: remember the take-off point and the launch pad. */
    public static void onLaunch(ServerPlayer player, boolean padTaken) {
        final GCPlayerData data = GCPlayerData.get(player);
        data.launchpadCount = padTaken ? 9 : 0;
        data.launchedFromX = player.getX();
        data.launchedFromZ = player.getZ();
    }

    /** 1.12.2 EntityTier1Rocket#onTeleport + WorldUtil#toCelestialSelection. */
    public static void reachSpace(ServerPlayer player, Tier1RocketEntity rocket) {
        final GCPlayerData data = GCPlayerData.get(player);
        data.rocketCargo = NonNullList.withSize(rocket.cargo().getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < data.rocketCargo.size(); i++) {
            data.rocketCargo.set(i, rocket.cargo().getItem(i).copy());
        }
        rocket.cargo().clearContent();
        data.rocketStorage = rocket.storage();
        data.rocketFuel = rocket.fuel();
        data.inSpace = true;
        data.spacePos = player.position();
        player.setNoGravity(true);
        openSelection(player);
    }

    private static void openSelection(ServerPlayer player) {
        GCNetwork.sendToPlayer(player, selectionPayload(player));
    }

    /** The destination screen's contents for this player: their reachable stations and whether they may build one. */
    public static GCNetwork.OpenCelestialSelection selectionPayload(ServerPlayer player) {
        final SpaceStations stations = SpaceStations.get(player.server);
        final List<GCNetwork.StationInfo> visible = new java.util.ArrayList<>();
        for (SpaceStations.Station station : stations.visitableBy(player)) {
            visible.add(new GCNetwork.StationInfo(station.id(), station.name(), station.ownerName(), station.isOwner(player)));
        }
        return new GCNetwork.OpenCelestialSelection(1, visible, canCreateStation(player, stations));
    }

    /** 1.12.2 GuiCelestialSelection#canCreateSpaceStation: one per player, from a rocket, unless the server forbids it. */
    private static boolean canCreateStation(ServerPlayer player, SpaceStations stations) {
        return GCPlayerData.get(player).inSpace && !io.github.reiko45.galacticraft.GCConfig.DISABLE_SPACE_STATION_CREATION.get()
                && stations.ownedBy(player.getUUID()) == null;
    }

    /** 1.12.2 S_BIND_SPACE_STATION_ID: pay for and build a station around Earth, then show it on the screen. */
    public static void createStation(ServerPlayer player) {
        final SpaceStations stations = SpaceStations.get(player.server);
        final ServerLevel orbit = player.server.getLevel(GCDimensions.ORBIT);
        if (orbit == null || !canCreateStation(player, stations) || !StationRecipe.consume(player)) {
            return;
        }
        final SpaceStations.Station station = stations.create(orbit, player);
        io.github.reiko45.galacticraft.GalacticraftCore.LOGGER.info("{} built space station {} at {}", player.getGameProfile().getName(), station.id(), station.base());
        openSelection(player);
    }

    /** 1.12.2 S_RENAME_SPACE_STATION: the owner names their station. */
    public static void renameStation(ServerPlayer player, int id, String name) {
        final SpaceStations stations = SpaceStations.get(player.server);
        final SpaceStations.Station station = stations.byId(id);
        final String trimmed = name.strip();
        if (station == null || !station.isOwner(player) || trimmed.isEmpty() || trimmed.length() > 32) {
            return;
        }
        stations.rename(station, trimmed);
        if (GCPlayerData.get(player).inSpace) {
            openSelection(player);
        }
    }

    /** The player picked a world on the selection screen. */
    public static void choose(ServerPlayer player, ResourceKey<Level> dimension) {
        choose(player, dimension, -1);
    }

    /** The player picked a world (and in orbit, a station) on the selection screen. */
    public static void choose(ServerPlayer player, ResourceKey<Level> dimension, int stationId) {
        final GCPlayerData data = GCPlayerData.get(player);
        final Destination destination = destination(dimension);
        final ServerLevel target = player.server.getLevel(dimension);
        if (!data.inSpace || destination == null || destination.tier() > 1 || target == null) {
            return;
        }
        final SpaceStations.Station station = dimension.equals(GCDimensions.ORBIT) ? SpaceStations.get(player.server).byId(stationId) : null;
        if (dimension.equals(GCDimensions.ORBIT) && (station == null || !station.canVisit(player))) {
            return;
        }
        data.inSpace = false;
        player.setNoGravity(false);
        // 1.12.2: the two extra cargo slots carry the launch pads and the (empty) rocket
        final NonNullList<ItemStack> cargo = data.rocketCargo.isEmpty() ? NonNullList.withSize(2, ItemStack.EMPTY) : data.rocketCargo;
        if (data.launchpadCount > 0) {
            cargo.set(cargo.size() - 2, new ItemStack(GCBlocks.LANDING_PAD.get(), data.launchpadCount));
        }
        cargo.set(cargo.size() - 1, Tier1RocketItem.create(data.rocketStorage, 0));
        final int fuel = data.rocketFuel;
        data.rocketCargo = NonNullList.create();
        data.rocketFuel = 0;
        data.launchpadCount = 0;
        if (station != null) {
            // TeleportTypeOrbit: step out beside the arrival block; a para-chest with the cargo drifts down into the bay
            final BlockPos arrival = station.arrival();
            player.teleportTo(target, arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D, player.getYRot(), player.getXRot());
            data.rocketCargo = cargo;
            data.rocketFuel = fuel;
            data.chestSpawnPos = Vec3.atLowerCornerOf(station.base()).add(-8.5D, 26.0D, -1.5D);
            data.chestSpawnCooldown = 200;
        } else if (dimension.equals(GCDimensions.MOON)) {
            // TeleportTypeMoon: arrive at y 900 in a lander (within the Moon's world border)
            final double limit = target.getWorldBorder().getSize() / 2.0D - 2.0D;
            final double x = Math.max(-limit, Math.min(limit, data.launchedFromX));
            final double z = Math.max(-limit, Math.min(limit, data.launchedFromZ));
            player.teleportTo(target, x, 900.0D, z, player.getYRot(), player.getXRot());
            final LanderEntity lander = LanderEntity.create(target, x, 900.0D, z, cargo, fuel);
            target.addFreshEntity(lander);
            player.startRiding(lander, true);
        } else {
            // TeleportTypeOverworld: arrive at y 250, parachest from y 230 within 5 blocks
            player.teleportTo(target, data.launchedFromX, 250.0D, data.launchedFromZ, player.getYRot(), player.getXRot());
            data.usingParachute = !data.gear(GearSlot.PARACHUTE).isEmpty();
            data.markGearChanged();
            data.rocketCargo = cargo;
            data.rocketFuel = fuel;
            data.chestSpawnPos = new Vec3(data.launchedFromX + (player.getRandom().nextDouble() * 2 - 1) * 5.0D, 230.0D,
                    data.launchedFromZ + (player.getRandom().nextDouble() * 2 - 1) * 5.0D);
            data.chestSpawnCooldown = 200;
        }
        player.resetFallDistance();
    }

    /** Every server tick for every player. */
    public static void tick(ServerPlayer player, GCPlayerData data) {
        if (data.inSpace) {
            // Hold still in space until a destination is chosen; ask again now and then in case the screen was missed
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
            if (player.position().distanceToSqr(data.spacePos) > 0.01D) {
                player.teleportTo(data.spacePos.x, data.spacePos.y, data.spacePos.z);
            }
            player.resetFallDistance();
            if (player.tickCount % 100 == 0) {
                openSelection(player);
            }
        }
        if (data.usingParachute) {
            player.resetFallDistance();
            if (player.onGround() || player.isInWater()) {
                data.usingParachute = false;
                data.markGearChanged();
            }
        }
        if (data.chestSpawnCooldown > 0) {
            data.chestSpawnCooldown--;
            if (data.chestSpawnCooldown == 180 && data.chestSpawnPos != null) {
                final ItemStack parachute = data.gear(GearSlot.PARACHUTE);
                final ParachestEntity chest = ParachestEntity.create(player.level(), data.chestSpawnPos.x, data.chestSpawnPos.y,
                        data.chestSpawnPos.z, data.rocketCargo, data.rocketFuel,
                        parachute.getItem() instanceof ParachuteItem item ? item.color() : 0);
                player.level().addFreshEntity(chest);
                data.chestSpawnPos = null;
                data.rocketCargo = NonNullList.create();
                data.rocketFuel = 0;
            }
        }
    }

    /** 1.12.2 S_IGNITE_ROCKET: fuel needed; without a parachute the first press only warns. */
    public static void ignite(ServerPlayer player) {
        if (!(player.getVehicle() instanceof Tier1RocketEntity rocket) || rocket.phase() != Tier1RocketEntity.Phase.UNIGNITED) {
            return;
        }
        final GCPlayerData data = GCPlayerData.get(player);
        if (!rocket.hasFuel()) {
            player.displayClientMessage(Component.translatable("gui.galacticraftcore.rocket.warning.no_fuel"), false);
        } else if (data.gear(GearSlot.PARACHUTE).isEmpty() && data.launchAttempts == 0) {
            player.displayClientMessage(Component.translatable("gui.galacticraftcore.rocket.warning.no_parachute"), false);
            data.launchAttempts = 1;
        } else {
            rocket.ignite();
            data.launchAttempts = 0;
        }
    }
}
