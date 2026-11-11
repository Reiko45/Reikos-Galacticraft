package io.github.reiko45.galacticraft.entity.monster;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Evolved Zombie (1.12.2 EntityEvolvedZombie): a zombie in an oxygen mask that breathes on airless worlds (entity tag
 * {@code galacticraftcore:breathes_in_space}). 30 health; speed 0.26/0.30/0.34, damage 3/4/5 and follow range 16/18/20
 * on easy/normal/hard. It doesn't drown into a Drowned or call vanilla zombies as reinforcements.
 */
public class EvolvedZombie extends Zombie {
    public EvolvedZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D).add(Attributes.FOLLOW_RANGE, 18.0D);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        final double d = EvolvedMobs.difficulty(this);
        EvolvedMobs.set(this, Attributes.MOVEMENT_SPEED, 0.26D + 0.04D * d);
        EvolvedMobs.set(this, Attributes.ATTACK_DAMAGE, 3.0D + d);
        EvolvedMobs.set(this, Attributes.FOLLOW_RANGE, 16.0D + d * 2.0D);
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    /** Vanilla would spawn plain zombies, which suffocate here. */
    @Override
    protected void randomizeReinforcementsChance() {
        EvolvedMobs.set(this, Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
