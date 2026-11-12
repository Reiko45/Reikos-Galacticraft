/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.entities.EntitySpiderQueen). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The Spider Queen (1.12.2 EntitySpiderQueen), boss of the Venus dungeons: 150 health, a bite, and from more than 5
 * blocks away a Web Shot every 1-3 seconds (more often the closer you are) that slows whatever it hits. Hurt past two
 * thirds and again past one third of her health, she flees to the middle of her room, climbs to its ceiling and wraps
 * herself in a web cocoon (counted up to 20 by the burrowed counter), immune to everything, calling down a Juicer every
 * second, up to six; she comes back out once every one of them is dead. Defeated, she drops the Tier 3 Dungeon Key and
 * fills the treasure chest with a Volcanic Pickaxe or a Shield Controller.
 */
public class SpiderQueenEntity extends DungeonBossEntity implements RangedAttackMob {
    public static final double MAX_HEALTH = 150.0D;
    public static final int MAX_JUICERS = 6;
    private static final EntityDataAccessor<Byte> BURROWED_COUNT = SynchedEntityData.defineId(SpiderQueenEntity.class, EntityDataSerializers.BYTE);

    public boolean shouldEvade;
    private final List<JuicerEntity> juicersSpawned = new ArrayList<>();
    @Nullable
    private List<UUID> spawnedPreload;
    private int rangedAttackTime;
    private final int minRangedAttackTime = 20;
    private final int maxRangedAttackTime = 60;

    public SpiderQueenEntity(EntityType<? extends SpiderQueenEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.250000001192092896D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
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

    /** 1.12.2 applyEntityAttributes: 150 health times the dungeon boss health config. */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        final var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(MAX_HEALTH * GCConfig.DUNGEON_BOSS_HEALTH_MOD.get());
            setHealth(getMaxHealth());
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED_COUNT, (byte) -1);
    }

    /** -1 out in the open; 0-20 while hidden in her cocoon (how far it has grown). */
    public byte burrowedCount() {
        return this.entityData.get(BURROWED_COUNT);
    }

    public void setBurrowedCount(byte count) {
        this.entityData.set(BURROWED_COUNT, count);
    }

    public List<JuicerEntity> juicers() {
        return this.juicersSpawned;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !this.shouldEvade && this.deathTime <= 0) {
            final LivingEntity target = getTarget();
            if (target != null) {
                final double dX = target.getX() - getX();
                final double dY = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - getY();
                final double dZ = target.getZ() - getZ();
                final float distance = 5.0F;
                final double d0 = distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--this.rangedAttackTime == 0) {
                    if (dX * dX + dY * dY + dZ * dZ > distance * distance) {
                        final float f = Mth.sqrt((float) d0) / distance;
                        performRangedAttack(target, 0.0F);
                        this.rangedAttackTime = Mth.floor(f * (this.maxRangedAttackTime - this.minRangedAttackTime) + this.minRangedAttackTime);
                    }
                } else if (this.rangedAttackTime < 0) {
                    final float f = Mth.sqrt((float) d0) / distance;
                    this.rangedAttackTime = Mth.floor(f * (this.maxRangedAttackTime - this.minRangedAttackTime) + this.minRangedAttackTime);
                }
            }
        }
        if (this.spawnedPreload != null && level() instanceof ServerLevel serverLevel) {
            for (UUID id : this.spawnedPreload) {
                if (serverLevel.getEntity(id) instanceof JuicerEntity juicer && !this.juicersSpawned.contains(juicer)) {
                    this.juicersSpawned.add(juicer);
                }
            }
            if (this.juicersSpawned.size() == this.spawnedPreload.size()) {
                this.spawnedPreload = null;
            }
        }
        if (!level().isClientSide && this.shouldEvade) {
            evade();
        }
    }

    /** Off to the middle of the room and up to its ceiling, to hide and call the Juicers. */
    private void evade() {
        final DungeonSpawnerBlockEntity spawner = spawner();
        final AABB room = spawner == null ? null : spawner.roomBounds();
        if (room != null) {
            final double tarX = (room.minX + room.maxX) / 2.0D;
            final double tarZ = (room.minZ + room.maxZ) / 2.0D;
            final double dX = tarX - getX();
            final double dY = room.maxY - getY();
            final double dZ = tarZ - getZ();
            final double speed = getAttributeValue(Attributes.MOVEMENT_SPEED);
            setDeltaMovement(Math.min(Math.max(dX / 2.0D, -speed), speed), getDeltaMovement().y, Math.min(Math.max(dZ / 2.0D, -speed), speed));
            getNavigation().moveTo(tarX, getY(), tarZ, speed);
            if (Math.abs(dX) < 0.1D && Math.abs(dZ) < 0.1D) {
                setDeltaMovement(getDeltaMovement().x, Math.min(dY, 0.2D), getDeltaMovement().z);
                if (Math.abs(dY) - getBbHeight() < 1.1D && Math.abs(getY() - this.yOld) < 0.05D) {
                    if (burrowedCount() >= 0) {
                        if (this.tickCount % 20 == 0 && this.juicersSpawned.size() < MAX_JUICERS) {
                            final JuicerEntity juicer = GCEntities.JUICER.get().create(level());
                            if (juicer != null) {
                                final double angle = Math.random() * 2.0D * Math.PI;
                                juicer.setPos(getX() + 3.0D * Math.sin(angle), getY() + 0.2D, getZ() + 3.0D * Math.cos(angle));
                                juicer.setHanging(true);
                                level().addFreshEntity(juicer);
                                this.juicersSpawned.add(juicer);
                            }
                        }
                        if (burrowedCount() < 20) {
                            setBurrowedCount((byte) (burrowedCount() + 1));
                        }
                    } else {
                        setBurrowedCount((byte) 0);
                    }
                }
            }
        }
        if (!this.juicersSpawned.isEmpty()) {
            boolean allDead = true;
            for (JuicerEntity juicer : this.juicersSpawned) {
                if (juicer.isAlive()) {
                    allDead = false;
                    break;
                }
            }
            if (allDead) {
                this.juicersSpawned.clear();
                this.shouldEvade = false;
                setBurrowedCount((byte) -1);
            }
        }
    }

    /** Hiding (and while climbing to hide), she can't be hurt (1.12.2 isEntityInvulnerable). */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (burrowedCount() >= 0) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    /** 1.12.2 damageEntity: crossing two thirds or one third of her health sends her to hide. */
    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        final float before = getHealth();
        super.actuallyHurt(source, amount);
        final float health = getHealth();
        final float third = getMaxHealth() / 3.0F;
        if (health < third && before >= third || health < 2 * third && before >= 2 * third) {
            this.shouldEvade = true;
        }
    }

    /** 1.12.2 attackEntityWithRangedAttack: a Web Shot, more accurate on harder difficulties. */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        final WebShotEntity shot = new WebShotEntity(level(), this, target, 0.8F, 14 - level().getDifficulty().getId() * 4);
        playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(shot);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public void makeStuckInBlock(BlockState state, net.minecraft.world.phys.Vec3 speedMultiplier) {
        if (!state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            super.makeStuckInBlock(state, speedMultiplier);
        }
    }

    @Override
    public int chestTier() {
        return 3;
    }

    @Override
    protected ItemStack key() {
        return new ItemStack(GCItems.TIER_3_DUNGEON_KEY.get());
    }

    /** 1.12.2 getGuaranteedLoot: GalacticraftRegistry#getDungeonLoot(3), a Volcanic Pickaxe or a Shield Controller. */
    @Override
    protected ItemStack guaranteedLoot() {
        return this.random.nextBoolean() ? new ItemStack(GCItems.VOLCANIC_PICKAXE.get()) : new ItemStack(GCItems.SHIELD_CONTROLLER.get());
    }

    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 0.4F;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
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
        playSound(SoundEvents.SPIDER_STEP, 0.5F, 0.5F);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true; // 1.12.2 ignoreFrustumCheck
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("should_evade", this.shouldEvade);
        final ListTag list = new ListTag();
        for (Entity juicer : this.juicersSpawned) {
            list.add(LongTag.valueOf(juicer.getUUID().getMostSignificantBits()));
            list.add(LongTag.valueOf(juicer.getUUID().getLeastSignificantBits()));
        }
        tag.put("spawned_children", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.shouldEvade = tag.getBoolean("should_evade");
        if (tag.contains("spawned_children")) {
            this.spawnedPreload = new ArrayList<>();
            final ListTag list = tag.getList("spawned_children", Tag.TAG_LONG);
            for (int i = 0; i + 1 < list.size(); i += 2) {
                this.spawnedPreload.add(new UUID(((LongTag) list.get(i)).getAsLong(), ((LongTag) list.get(i + 1)).getAsLong()));
            }
        }
    }
}
