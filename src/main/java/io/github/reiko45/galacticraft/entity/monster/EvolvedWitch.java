/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.EntityEvolvedWitch). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.Level;

/**
 * 1.12.2 EntityEvolvedWitch: a witch in an oxygen mask, breathing anywhere (the {@code breathes_in_space} tag). It is
 * what an Alien Villager becomes when struck by lightning; it has no natural spawns. Its drops are a witch's plus, now
 * and then, Galacticraft gear (its loot table).
 */
public class EvolvedWitch extends Witch {
    public EvolvedWitch(EntityType<? extends Witch> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Witch.createAttributes();
    }
}
