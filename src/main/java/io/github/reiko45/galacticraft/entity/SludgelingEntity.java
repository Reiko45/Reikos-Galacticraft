/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.entities.EntitySludgeling). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.entity.monster.EvolvedCreeper;
import io.github.reiko45.galacticraft.entity.monster.EvolvedSkeleton;
import io.github.reiko45.galacticraft.entity.monster.EvolvedSpider;
import io.github.reiko45.galacticraft.entity.monster.EvolvedZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sludgeling (1.12.2 EntitySludgeling): a tiny, fast worm that bacterial sludge spits out when something wades in. 7
 * health; it goes for players and the evolved mobs, and sometimes for Slimelings. Breathes underwater and in space;
 * makes a silverfish's noises and drops nothing.
 */
public class SludgelingEntity extends Monster {
    public SludgelingEntity(EntityType<? extends SludgelingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 7.0D).add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.25D, true));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, true, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EvolvedZombie.class, 0, false, true, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EvolvedSkeleton.class, 0, false, true, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EvolvedSpider.class, 0, false, true, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EvolvedCreeper.class, 0, false, true, null));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SlimelingEntity.class, 200, false, true, null));
    }

    /** 1.12.2 canTriggerWalking: false. */
    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.EVENTS;
    }

    /** 1.12.2 onUpdate: the body always faces where it looks. */
    @Override
    public void tick() {
        this.yBodyRot = getYRot();
        super.tick();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 1.0F);
    }
}
