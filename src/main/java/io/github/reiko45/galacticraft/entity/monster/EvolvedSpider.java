package io.github.reiko45.galacticraft.entity.monster;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Evolved Spider (1.12.2 EntityEvolvedSpider): 22 health; speed 0.30/0.35/0.40 and damage 2/3/4 by difficulty. */
public class EvolvedSpider extends Spider {
    public EvolvedSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 22.0D).add(Attributes.MOVEMENT_SPEED, 0.35D).add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        final double d = EvolvedMobs.difficulty(this);
        EvolvedMobs.set(this, Attributes.MOVEMENT_SPEED, 0.3D + 0.05D * d);
        EvolvedMobs.set(this, Attributes.ATTACK_DAMAGE, 2.0D + d);
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }
}
