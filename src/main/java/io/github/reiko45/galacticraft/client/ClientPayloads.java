package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.player.GearData;
import java.util.HashMap;
import java.util.Map;

/** Client-side state received from the server: the HUD oxygen values and every visible player's gear. */
public final class ClientPayloads {
    private static final Map<Integer, GearData> GEAR = new HashMap<>();
    private static int air1 = 90;
    private static int air2 = 90;
    private static boolean oxygenValid = true;
    private static int thermalLevel;
    private static boolean thermalNormalising = true;

    private ClientPayloads() {
    }

    public static void oxygenState(GCNetwork.OxygenState payload) {
        air1 = payload.air1();
        air2 = payload.air2();
        oxygenValid = payload.valid();
        thermalLevel = payload.thermalLevel();
        thermalNormalising = payload.thermalNormalising();
    }

    public static int thermalLevel() {
        return thermalLevel;
    }

    public static boolean thermalNormalising() {
        return thermalNormalising;
    }

    public static void openCelestialSelection(GCNetwork.OpenCelestialSelection payload) {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.screen instanceof io.github.reiko45.galacticraft.client.screen.CelestialSelectionScreen screen) {
            screen.update(payload);
        } else {
            minecraft.setScreen(new io.github.reiko45.galacticraft.client.screen.CelestialSelectionScreen(payload));
        }
    }

    public static void gearSync(GCNetwork.GearSync payload) {
        if (payload.gear().equals(GearData.NONE)) {
            GEAR.remove(payload.entityId());
        } else {
            GEAR.put(payload.entityId(), payload.gear());
        }
    }

    public static GearData gear(int entityId) {
        return GEAR.getOrDefault(entityId, GearData.NONE);
    }

    public static int air1() {
        return air1;
    }

    public static int air2() {
        return air2;
    }

    public static boolean oxygenValid() {
        return oxygenValid;
    }

    /** Forget everything when leaving a world. */
    /** The slimeling's panel or, after a meal, its feeding screen. */
    public static void openSlimeling(GCNetwork.OpenSlimelingScreen payload) {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.entity()) instanceof io.github.reiko45.galacticraft.entity.SlimelingEntity slimeling) {
            minecraft.setScreen(payload.screen() == io.github.reiko45.galacticraft.entity.SlimelingEntity.SCREEN_FEED
                    ? new io.github.reiko45.galacticraft.client.screen.SlimelingFeedScreen(slimeling)
                    : new io.github.reiko45.galacticraft.client.screen.SlimelingScreen(slimeling));
        }
    }

    public static void reset() {
        io.github.reiko45.galacticraft.world.station.StationSpin.Client.reset();
        GEAR.clear();
        air1 = 90;
        air2 = 90;
        oxygenValid = true;
        thermalLevel = 0;
        thermalNormalising = true;
    }
}
