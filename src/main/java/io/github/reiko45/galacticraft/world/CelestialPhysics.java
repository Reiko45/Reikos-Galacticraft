package io.github.reiko45.galacticraft.world;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Per-dimension physics, ported from {@code IGalacticraftWorldProvider} in 1.12.2.
 *
 * @param gravityReduction   subtracted from the vanilla gravity of 0.08 blocks/tick^2
 *                           (1.12.2 {@code getGravity()}; Moon = 0.062, so effective gravity is 0.018)
 * @param fallDamageModifier fall distance multiplier (1.12.2 {@code getFallDamageModifier()}; Moon = 0.18)
 * @param solarMultiplier    solar panel output multiplier (1.12.2 {@code ISolarLevel#getSolarEnergyMultiplier()}; Moon = 1.4)
 * @param breathable         whether the atmosphere holds oxygen (1.12.2 {@code hasBreathableAtmosphere()} and
 *                           {@code isGasPresent(OXYGEN)}); without it players need oxygen gear, mobs suffocate and
 *                           torches go out
 * @param arrowGravity       arrow gravity per tick (1.12.2 {@code getArrowGravity()}, 0.005 on every space world; vanilla 0.05)
 * @param meteorFrequency    1.12.2 {@code getMeteorFrequency()}: 5 on airless worlds, 0 for none; a small meteor falls
 *                           near each player with chance 1 in (750 x frequency) per tick, a large one 1 in 3x that
 * @param fuelUsage          1.12.2 {@code getFuelUsageMultiplier()}: a climbing rocket burns 1 mB every floor(3 / this)
 *                           ticks (Moon 0.7, orbit 0.5)
 * @param exitHeight         1.12.2 {@code IExitHeight#getYCoordinateToTeleport()}: where a rocket reaches space
 */
public record CelestialPhysics(double gravityReduction, float fallDamageModifier, float solarMultiplier, boolean breathable,
                               float arrowGravity, double meteorFrequency, double fuelUsage, double exitHeight) {
    /** Earth orbit (1.12.2 WorldProviderOverworldOrbit); its solar multiplier is the spaceStationEnergyScalar config. */
    public static final CelestialPhysics ORBIT = new CelestialPhysics(0.075D, 0.4F, 2.0F, false, 0.005F, 0.0D, 0.5D, 750.0D);
    /**
     * Mars (1.12.2 WorldProviderMars): solar (1 / 1.25 AU)^3 = 0.512 from WorldProviderSpace#getSolarSize, meteors
     * 100 x its atmosphere's density of 0.1, arrows fall 0.015 per tick.
     */
    public static final CelestialPhysics MARS = new CelestialPhysics(0.058D, 0.38F, 0.512F, false, 0.015F, 10.0D, 0.9D, 1200.0D);
    private static final Map<ResourceKey<Level>, CelestialPhysics> BY_DIMENSION = Map.of(
            GCDimensions.MOON, new CelestialPhysics(0.062D, 0.18F, 1.4F, false, 0.005F, 5.0D, 0.7D, 1200.0D),
            GCDimensions.ORBIT, ORBIT,
            GCDimensions.MARS, MARS
    );

    /** Where a rocket launched in this world reaches space (1200 outside Galacticraft worlds). */
    public static double exitHeight(Level level) {
        final CelestialPhysics physics = of(level.dimension());
        return physics == null ? 1200.0D : physics.exitHeight();
    }

    /** Ticks between each mB a climbing Tier 1 rocket burns (3 outside Galacticraft worlds). */
    public static int fuelInterval(Level level) {
        return fuelInterval(level, 3.0D);
    }

    /** Ticks between each mB a climbing rocket burns: floor(base / fuel usage), {@code base} outside Galacticraft worlds. */
    public static int fuelInterval(Level level, double base) {
        final CelestialPhysics physics = of(level.dimension());
        return physics == null || physics.fuelUsage() <= 0.0D ? (int) base : Math.max(1, (int) Math.floor(base / physics.fuelUsage()));
    }

    /** 1.12.2 WorldProviderOverworldOrbit#isDaytime: in orbit the planet only hides the sun for a sixth of the day. */
    public static boolean isDaytime(Level level) {
        if (level.dimension().equals(GCDimensions.ORBIT)) {
            final float angle = level.getTimeOfDay(1.0F); // celestial angle: 0 at noon, 0.5 at midnight
            return angle < 0.42F || angle > 0.58F;
        }
        return level.isDay();
    }

    /** 1.12.2 TransformerHooks#getItemGravity: max(0.002, 0.04 - gravity / 1.75). */
    public double itemGravity() {
        return Math.max(0.002D, 0.03999999910593033D - this.gravityReduction / 1.75D);
    }

    @Nullable
    public static CelestialPhysics of(ResourceKey<Level> dimension) {
        return BY_DIMENSION.get(dimension);
    }

    /** 1.12.2 OxygenUtil#noAtmosphericCombustion: a Galacticraft body without oxygen in its atmosphere. */
    public static boolean isAirless(Level level) {
        final CelestialPhysics physics = of(level.dimension());
        return physics != null && !physics.breathable();
    }
}
