/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.api.world.EnumAtmosphericGas and the planets'
 * atmosphereComponent settings). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What a Galacticraft world's air is made of (1.12.2 CelestialBody#atmosphere), for the Gas Liquefier and Methane
 * Synthesizer. Mars: carbon dioxide, argon and nitrogen; the Moon and orbit have none.
 */
public final class Atmosphere {
    public enum Gas {
        NITROGEN, OXYGEN, CO2, WATER, METHANE, HYDROGEN, HELIUM, ARGON
    }

    private static final Map<ResourceKey<Level>, List<Gas>> GASES = Map.of(
            GCDimensions.MARS, List.of(Gas.CO2, Gas.ARGON, Gas.NITROGEN),
            GCDimensions.MOON, List.of(),
            GCDimensions.ORBIT, List.of());

    private Atmosphere() {
    }

    /** The gases of a Galacticraft world, in 1.12.2's order; null for other worlds (the Overworld, the Nether...). */
    @Nullable
    public static List<Gas> of(ResourceKey<Level> dimension) {
        return GASES.get(dimension);
    }
}
