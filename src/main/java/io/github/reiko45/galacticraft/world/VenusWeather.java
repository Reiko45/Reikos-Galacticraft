/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.dimension.WorldProviderVenus#updateWeatherOverride).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Venus's own weather (1.12.2 WorldProviderVenus#updateWeatherOverride), in place of vanilla's (which in another
 * dimension only follows the Overworld's): acid rain spells of 1,000-4,600 ticks between dry spells of 1,000-3,000;
 * while it rains, its strength creeps (0.004 a tick) towards a target that changes every 100-300 ticks, between 0.15 and
 * 0.6, and Venus rain is never heavier than 0.6. Players there are told of the changes as vanilla tells them of rain.
 */
public final class VenusWeather {
    public static final float MAX_STRENGTH = 0.6F;
    private static final Map<ServerLevel, VenusWeather> STATES = new WeakHashMap<>();

    boolean raining;
    int rainTime = 100;
    int rainChange = 100;
    float targetRain;

    private VenusWeather() {
    }

    /** Runs Venus's weather for a tick; false (vanilla's runs) for any other level. */
    public static boolean tick(ServerLevel level) {
        if (!level.dimension().equals(GCDimensions.VENUS)) {
            return false;
        }
        run(level);
        return true;
    }

    /** One tick of Venus weather on a level (any level: tests run it where they are). */
    public static void run(ServerLevel level) {
        final VenusWeather state;
        synchronized (STATES) {
            state = STATES.computeIfAbsent(level, l -> new VenusWeather());
        }
        state.advance(level);
    }

    /** For tests: start (or stop) a spell of rain now, aiming for this strength. */
    public static void force(ServerLevel level, boolean raining, float target) {
        synchronized (STATES) {
            final VenusWeather state = STATES.computeIfAbsent(level, l -> new VenusWeather());
            state.raining = raining;
            state.rainTime = 6000;
            state.targetRain = target;
            state.rainChange = 6000;
        }
    }

    private void advance(ServerLevel level) {
        final RandomSource random = level.getRandom();
        if (--this.rainTime <= 0) {
            this.raining = !this.raining;
            this.rainTime = this.raining ? random.nextInt(3600) + 1000 : random.nextInt(2000) + 1000;
        }
        if (--this.rainChange <= 0) {
            this.targetRain = 0.15F + random.nextFloat() * 0.45F;
            this.rainChange = random.nextInt(200) + 100;
        }
        final boolean wasRaining = level.isRaining();
        final float before = level.getRainLevel(1.0F);
        float strength = before;
        if (this.raining && strength < this.targetRain) {
            strength += 0.004F;
        } else if (!this.raining || strength > this.targetRain) {
            strength -= 0.004F;
        }
        strength = Mth.clamp(strength, 0.0F, MAX_STRENGTH);
        level.setRainLevel(strength);
        final var players = level.getServer().getPlayerList();
        if (before != strength) {
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, strength), level.dimension());
        }
        if (wasRaining != level.isRaining()) {
            players.broadcastAll(new ClientboundGameEventPacket(wasRaining ? ClientboundGameEventPacket.STOP_RAINING
                    : ClientboundGameEventPacket.START_RAINING, 0.0F), level.dimension());
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, strength), level.dimension());
        }
    }
}
