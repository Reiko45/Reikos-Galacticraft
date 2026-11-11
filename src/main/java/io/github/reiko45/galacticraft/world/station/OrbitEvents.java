package io.github.reiko45.galacticraft.world.station;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 1.12.2 TickHandlerServer + IOrbitDimension#getYCoordToTeleportToPlanet: whatever drops below y 30 in Earth orbit
 * falls back to Earth. Players come down at y 250 above where they last launched from (under their parachute, if worn,
 * as after a rocket flight); anything else at y 250 above the same spot relative to its station.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class OrbitEvents {
    public static final int FALL_TO_PLANET_Y = 30;

    private OrbitEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel orbit) || !orbit.dimension().equals(GCDimensions.ORBIT)) {
            return;
        }
        for (SpaceStations.Station station : SpaceStations.get(orbit.getServer()).all()) {
            station.spin().tick(orbit, station);
        }
        final ServerLevel earth = orbit.getServer().overworld();
        final List<Entity> falling = new ArrayList<>();
        for (Entity entity : orbit.getAllEntities()) {
            if (entity.getY() <= FALL_TO_PLANET_Y && entity.isAlive() && !entity.isPassenger()) {
                falling.add(entity);
            }
        }
        for (Entity entity : falling) {
            if (entity instanceof ServerPlayer player) {
                fallToEarth(player, earth);
            } else {
                final SpaceStations.Station station = SpaceStations.get(orbit.getServer()).at(entity.blockPosition());
                final BlockPos base = station == null ? BlockPos.ZERO : station.base();
                entity.changeDimension(new DimensionTransition(earth, new Vec3(entity.getX() - base.getX(), 250.0D, entity.getZ() - base.getZ()),
                        Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
            }
        }
    }

    /** Floating players in orbit must not be kicked for flying. */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.connection != null && player.level().dimension().equals(GCDimensions.ORBIT)) {
            final var connection = (io.github.reiko45.galacticraft.mixin.ServerGamePacketListenerImplAccessor) player.connection;
            connection.galacticraftcore$setAboveGroundTickCount(0);
            connection.galacticraftcore$setAboveGroundVehicleTickCount(0);
        }
    }

    /** Arriving in orbit: learn how every station spins. */
    @SubscribeEvent
    public static void onChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo().equals(GCDimensions.ORBIT) && event.getEntity() instanceof ServerPlayer player) {
            StationSpin.syncAll(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(GCDimensions.ORBIT)) {
            StationSpin.syncAll(player);
        }
    }

    /** 1.12.2 TeleportTypeOverworld: arrive at y 250 above the last launch point, parachute open if worn. */
    public static void fallToEarth(ServerPlayer player, ServerLevel earth) {
        final GCPlayerData data = GCPlayerData.get(player);
        player.teleportTo(earth, data.launchedFromX, 250.0D, data.launchedFromZ, player.getYRot(), player.getXRot());
        data.usingParachute = !data.gear(GearSlot.PARACHUTE).isEmpty();
        data.markGearChanged();
        player.resetFallDistance();
    }
}
