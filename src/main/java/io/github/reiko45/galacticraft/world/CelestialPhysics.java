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
 */
public record CelestialPhysics(double gravityReduction, float fallDamageModifier, float solarMultiplier) {
    private static final Map<ResourceKey<Level>, CelestialPhysics> BY_DIMENSION = Map.of(
            GCDimensions.MOON, new CelestialPhysics(0.062D, 0.18F, 1.4F)
    );

    @Nullable
    public static CelestialPhysics of(ResourceKey<Level> dimension) {
        return BY_DIMENSION.get(dimension);
    }
}
