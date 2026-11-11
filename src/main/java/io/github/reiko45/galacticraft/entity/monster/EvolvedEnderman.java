package io.github.reiko45.galacticraft.entity.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;

/** Evolved Enderman (1.12.2 EntityEvolvedEnderman): an enderman with an oxygen mask and tanks; vanilla stats. */
public class EvolvedEnderman extends EnderMan {
    public EvolvedEnderman(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EnderMan.createAttributes();
    }
}
