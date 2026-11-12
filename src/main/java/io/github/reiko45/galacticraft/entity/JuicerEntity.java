/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.entities.EntityJuicer). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Juicer (1.12.2 EntityJuicer): a little scorpion-like spider of the Venus dungeons, which the Spider Queen calls up when
 * she hides. Every 3-12 seconds, standing on (or hanging under) Venus dungeon brick, it looks up (or down) to 10 blocks
 * for more of it, and glides there at up to 0.21 blocks a tick on each axis: under a ceiling it hangs upside down
 * without falling until its next drop. It bites like a spider and breathes anything.
 *
 * <p>1.12.2 used one data flag for both "hanging" and "falling" (isHanging read the falling flag), so a Juicer counts as
 * hanging for the first tick of each glide and not during the rest; that is kept. One difference: 1.12.2 steered it
 * with a ceiling path-finder that only let it walk under a solid ceiling; here it walks the floor like a spider and
 * keeps still while hanging.
 */
public class JuicerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(JuicerEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private BlockPos jumpTarget;
    private int timeSinceLastJump;

    public JuicerEntity(EntityType<? extends JuicerEntity> type, Level level) {
        super(type, level);
        this.timeSinceLastJump = this.random.nextInt(200) + 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.30000001192092896D).add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HANGING, false);
    }

    /** Clinging to a ceiling (1.12.2 isHanging, which is also isFalling). */
    public boolean isHanging() {
        return this.entityData.get(HANGING);
    }

    public void setHanging(boolean hanging) {
        this.entityData.set(HANGING, hanging);
    }

    @Nullable
    public BlockPos jumpTarget() {
        return this.jumpTarget;
    }

    /** Ready to look for its next ledge now (tests). */
    public void readyToJump() {
        this.timeSinceLastJump = 0;
    }

    private static boolean isDungeonBrick(BlockState state) {
        return state.is(GCBlocks.VENUS_DUNGEON_BRICK_1.get()) || state.is(GCBlocks.VENUS_DUNGEON_BRICK_2.get());
    }

    /** 1.12.2 onLivingUpdate: pick a jump target between floor and ceiling of dungeon brick. */
    @Override
    public void aiStep() {
        if (isHanging()) {
            setOnGround(true);
        }
        super.aiStep();
        if (!level().isClientSide && this.jumpTarget == null) {
            if (this.timeSinceLastJump <= 0) {
                final boolean hanging = isHanging();
                final BlockPos against = BlockPos.containing(getX(), getY() + (hanging ? 1.0D : -0.5D), getZ());
                if (isDungeonBrick(level().getBlockState(against))) {
                    final Vec3 from = position();
                    final BlockHitResult hit = level().clip(new ClipContext(from, from.add(0.0D, hanging ? -10.0D : 10.0D, 0.0D),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                    if (hit.getType() == HitResult.Type.BLOCK && isDungeonBrick(level().getBlockState(hit.getBlockPos()))) {
                        this.jumpTarget = hanging ? hit.getBlockPos() : hit.getBlockPos().below();
                        setHanging(true);
                    }
                }
            } else {
                this.timeSinceLastJump--;
            }
        }
        if (isHanging()) {
            // it waits on the ceiling for its next drop (1.12.2's ceiling path-finder let it creep along there)
            getNavigation().stop();
            setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    /** 1.12.2 onUpdate: the glide to the target, settling under the ceiling or on the floor. */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || this.jumpTarget == null) {
            return;
        }
        final double dx = this.jumpTarget.getX() - getX() + 0.5D;
        final double dy = this.jumpTarget.getY() - getY() + 0.6D;
        final double dz = this.jumpTarget.getZ() - getZ() + 0.5D;
        final double motionY = dy > 0 ? Math.min(dy / 2.0D, 0.2123D) : Math.max(dy / 2.0D, -0.2123D);
        final double motionX = dx > 0 ? Math.min(dx / 2.0D, 0.2123D) : Math.max(dx / 2.0D, -0.2123D);
        final double motionZ = dz > 0 ? Math.min(dz / 2.0D, 0.2123D) : Math.max(dz / 2.0D, -0.2123D);
        setDeltaMovement(motionX, motionY, motionZ);
        if (dy > 0.0D && Math.abs(this.jumpTarget.getY() - (getY() + motionY)) < 0.4D) {
            setPos(this.jumpTarget.getX() + 0.5D, this.jumpTarget.getY() + 0.2D, this.jumpTarget.getZ() + 0.5D);
            land(true);
        } else if (dy < 0.0D && Math.abs(this.jumpTarget.getY() - (getY() + motionY)) < 0.8D) {
            setPos(this.jumpTarget.getX() + 0.5D, this.jumpTarget.getY() + 1.0D, this.jumpTarget.getZ() + 0.5D);
            land(false);
        } else {
            setHanging(false);
        }
    }

    private void land(boolean hanging) {
        this.jumpTarget = null;
        this.timeSinceLastJump = this.random.nextInt(180) + 60;
        setHanging(hanging);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean isNoGravity() {
        return isHanging() || this.jumpTarget != null || super.isNoGravity();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** 1.12.2 setInWeb: cobwebs don't slow it. */
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, speedMultiplier);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() + 0.4F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("timeSinceLastJump", this.timeSinceLastJump);
        tag.putBoolean("jumpTargetNull", this.jumpTarget == null);
        if (this.jumpTarget != null) {
            tag.putInt("jumpTargetX", this.jumpTarget.getX());
            tag.putInt("jumpTargetY", this.jumpTarget.getY());
            tag.putInt("jumpTargetZ", this.jumpTarget.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.timeSinceLastJump = tag.getInt("timeSinceLastJump");
        this.jumpTarget = tag.getBoolean("jumpTargetNull") ? null
                : new BlockPos(tag.getInt("jumpTargetX"), tag.getInt("jumpTargetY"), tag.getInt("jumpTargetZ"));
        setHanging(this.jumpTarget != null);
    }
}
