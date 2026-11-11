package io.github.reiko45.galacticraft.world;

import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Dimension keys. In 1.12.2 dimensions were registered at runtime with numeric IDs; since 1.16 they
 * are data-driven, so the Moon is defined by datagen ({@link io.github.reiko45.galacticraft.data.GCWorldgenProvider})
 * and referenced here by key.
 */
public final class GCDimensions {
    public static final ResourceLocation MOON_ID = GalacticraftCore.id("moon");

    public static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION, MOON_ID);
    public static final ResourceKey<LevelStem> MOON_STEM = ResourceKey.create(Registries.LEVEL_STEM, MOON_ID);
    public static final ResourceKey<DimensionType> MOON_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, MOON_ID);
    public static final ResourceKey<Biome> MOON_BIOME = ResourceKey.create(Registries.BIOME, MOON_ID);
    /** Client sky/fog renderer id, see {@code GalacticraftClient}. */
    public static final ResourceLocation MOON_EFFECTS = MOON_ID;

    /**
     * Earth orbit, where every space station of Earth lives (1.12.2 gave each station its own runtime dimension, which
     * 1.21 can't create; here station n sits in its own region of one shared dimension, see SpaceStations).
     */
    public static final ResourceLocation ORBIT_ID = GalacticraftCore.id("overworld_orbit");
    public static final ResourceKey<Level> ORBIT = ResourceKey.create(Registries.DIMENSION, ORBIT_ID);
    public static final ResourceKey<LevelStem> ORBIT_STEM = ResourceKey.create(Registries.LEVEL_STEM, ORBIT_ID);
    public static final ResourceKey<DimensionType> ORBIT_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ORBIT_ID);
    public static final ResourceKey<Biome> ORBIT_BIOME = ResourceKey.create(Registries.BIOME, GalacticraftCore.id("space"));
    public static final ResourceLocation ORBIT_EFFECTS = ORBIT_ID;

    /** Mars (1.12.2 WorldProviderMars, planets module). */
    public static final ResourceLocation MARS_ID = GalacticraftCore.id("mars");
    public static final ResourceKey<Level> MARS = ResourceKey.create(Registries.DIMENSION, MARS_ID);
    public static final ResourceKey<LevelStem> MARS_STEM = ResourceKey.create(Registries.LEVEL_STEM, MARS_ID);
    public static final ResourceKey<DimensionType> MARS_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, MARS_ID);
    public static final ResourceKey<Biome> MARS_BIOME = ResourceKey.create(Registries.BIOME, MARS_ID);
    public static final ResourceLocation MARS_EFFECTS = MARS_ID;

    private GCDimensions() {
    }
}
