package io.github.reiko45.galacticraft.entity.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

/** Evolved Skeleton (1.12.2 EntityEvolvedSkeleton): 25 health, speed 0.35, breathes in space; it doesn't turn into a Stray. */
public class EvolvedSkeleton extends Skeleton {
    public EvolvedSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes().add(Attributes.MAX_HEALTH, 25.0D).add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }
}
