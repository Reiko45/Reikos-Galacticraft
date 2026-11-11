package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Galacticraft's own tags (the {@code c:} convention tags are in data/GCTags). */
public final class GCModTags {
    /** Galacticraft-inventory thermal padding slots; filled by the planets (Asteroids) phase. */
    public static final TagKey<Item> THERMAL_HELMETS = item("thermal_padding/helmet");
    public static final TagKey<Item> THERMAL_CHESTPLATES = item("thermal_padding/chestplate");
    public static final TagKey<Item> THERMAL_LEGGINGS = item("thermal_padding/leggings");
    public static final TagKey<Item> THERMAL_BOOTS = item("thermal_padding/boots");
    /** Galacticraft-inventory shield controller slot (Venus). */
    public static final TagKey<Item> SHIELD_CONTROLLERS = item("shield_controllers");

    /**
     * Non-full blocks that still seal a room (1.12.2 config "External Sealable IDs", default glass_pane), in addition
     * to full cubes and glass.
     */
    public static final TagKey<Block> SEALABLE = block("sealable");
    /** Full blocks that let air through (1.12.2: gravel, wool, sponge, leaves). */
    public static final TagKey<Block> AIR_PERMEABLE = block("air_permeable");

    /** Mobs that don't need oxygen on airless worlds (1.12.2 IEntityBreathable, used by the evolved mobs later). */
    public static final TagKey<EntityType<?>> BREATHES_IN_SPACE = TagKey.create(Registries.ENTITY_TYPE, GalacticraftCore.id("breathes_in_space"));

    private GCModTags() {
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, GalacticraftCore.id(path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, GalacticraftCore.id(path));
    }
}
