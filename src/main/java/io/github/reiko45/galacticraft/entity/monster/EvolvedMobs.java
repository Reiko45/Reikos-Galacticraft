package io.github.reiko45.galacticraft.entity.monster;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.Holder;

/** Shared bits of the evolved mobs (1.12.2 EntityEvolved*#applyEntityAttributes). */
final class EvolvedMobs {
    private EvolvedMobs() {
    }

    /** 1.12.2: 0 on peaceful/easy, 1 on normal, 2 on hard. */
    static double difficulty(LivingEntity entity) {
        final Difficulty difficulty = entity.level().getDifficulty();
        return difficulty == Difficulty.HARD ? 2.0D : difficulty == Difficulty.NORMAL ? 1.0D : 0.0D;
    }

    static void set(LivingEntity entity, Holder<Attribute> attribute, double value) {
        final var instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
