package io.github.reiko45.galacticraft.entity.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/** Evolved Creeper (1.12.2 EntityEvolvedCreeper): 25 health, breathes in space, otherwise a creeper. */
public class EvolvedCreeper extends Creeper {
    public EvolvedCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes().add(Attributes.MAX_HEALTH, 25.0D);
    }
}
