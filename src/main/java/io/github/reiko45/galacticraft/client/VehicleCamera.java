/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.player.PlayerClient,
 * core.TransformerHooks#getCameraZoom and api.entity.ICameraZoomEntity). Copyright (c) 2023 Team Galacticraft. Licensed
 * under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 1.12.2 ICameraZoomEntity: getting into a rocket, lander, landing balloons or entry pod switches to the third-person view
 * once, and while riding one the third-person camera stands 15 blocks back instead of 4. Neither happens with the client's
 * disableVehicleCameraChanges (for Vivecraft).
 */
public final class VehicleCamera {
    /** 1.12.2 getCameraZoom of every rocket and lander. */
    public static final float ZOOM = 15.0F;
    private static boolean lastRiding;

    private VehicleCamera() {
    }

    public static boolean zooms(Entity vehicle) {
        return vehicle instanceof RocketEntity || vehicle instanceof LanderEntity;
    }

    private static boolean enabled() {
        return !GCConfig.client(GCConfig.DISABLE_VEHICLE_CAMERA_CHANGES);
    }

    /** PlayerClient: third person on getting in (and only then, so the view can still be changed while riding). */
    public static void onClientTick(ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final boolean riding = minecraft.player != null && zooms(minecraft.player.getVehicle());
        if (riding && !lastRiding && enabled()) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        lastRiding = riding;
    }

    /** TransformerHooks#getCameraZoom: the vehicle's camera distance. */
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        final Entity viewer = event.getCamera().getEntity();
        if (viewer != null && zooms(viewer.getVehicle()) && enabled()) {
            event.setDistance(ZOOM);
        }
    }
}
