package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Evolved Skeleton Boss (1.12.2 EntitySkeletonBoss + EntityBossBase): 150 health, a bow (arrows every 25 ticks within
 * 10 blocks), and a grab: touching its target it lifts them up and throws them 40 ticks later. It leaves when nobody
 * is left in its room. Dying takes 200 ticks of explosions; then it refills the dungeon's treasure chest (loot plus a
 * schematic), drops a Tier 1 Dungeon Key and, rarely, its bow.
 */
public class SkeletonBossEntity extends DungeonBossEntity implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> THROW = SynchedEntityData.defineId(SkeletonBossEntity.class, EntityDataSerializers.INT);

    private int throwTimer;
    private int postThrowDelay = 20;
    @Nullable
    private Entity thrownEntity;

    public SkeletonBossEntity(EntityType<? extends SkeletonBossEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        this.xpReward = 50;
    }

    @Override
    public int chestTier() {
        return 1;
    }

    @Override
    protected ItemStack key() {
        return new ItemStack(GCItems.TIER_1_DUNGEON_KEY.get());
    }

    @Override
    protected ItemStack guaranteedLoot() {
        return guaranteedLoot(this.random);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 150.0D).add(Attributes.MOVEMENT_SPEED, 0.325D)
                .add(Attributes.FOLLOW_RANGE, 40.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(THROW, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 25, 10.0F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        final double d = switch (level.getDifficulty()) {
            case HARD -> 2.0D;
            case NORMAL -> 1.0D;
            default -> 0.0D;
        };
        final var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(150.0D * io.github.reiko45.galacticraft.GCConfig.DUNGEON_BOSS_HEALTH_MOD.get());
            setHealth(getMaxHealth());
        }
        final var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.25D + 0.075D * d);
        }
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    /** Ticks left of the current grab and throw, for the model (1.12.2 throwTimer + postThrowDelay). */
    public int throwTicks() {
        return this.entityData.get(THROW);
    }

    /** 1.12.2 onCollideWithPlayer: grab the target. */
    @Override
    public void playerTouch(Player player) {
        if (!level().isClientSide && !isNoAi() && getPassengers().isEmpty() && this.postThrowDelay == 0 && this.throwTimer == 0
                && player == getTarget() && this.deathTime == 0) {
            playSound(GCSounds.BOSS_LAUGH.get(), 1.5F, 1.0F);
            player.startRiding(this, true);
            this.throwTimer = 40;
        }
        super.playerTouch(player);
    }

    /** Held players can't get off by themselves. */
    public boolean holdsRider() {
        return this.throwTimer > 0 && isAlive();
    }

    /** 1.12.2 EventHandlerGC#entityLivingEvent: no dismounting while the boss holds you (dead riders still fall off). */
    @net.neoforged.fml.common.EventBusSubscriber(modid = io.github.reiko45.galacticraft.GalacticraftCore.MOD_ID)
    public static final class Events {
        private Events() {
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void dismount(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
            if (event.isDismounting() && event.getEntityBeingMounted() instanceof SkeletonBossEntity boss && boss.holdsRider()
                    && event.getEntityMounting().isAlive()) {
                event.setCanceled(true);
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        final double offsetX = Math.sin(-this.yHeadRot * Mth.DEG_TO_RAD);
        final double offsetZ = Math.cos(this.yHeadRot * Mth.DEG_TO_RAD);
        final double offsetY = 2.0D * Math.cos((this.throwTimer + this.postThrowDelay) * 0.05F);
        callback.accept(passenger, getX() + offsetX, getY() + getPassengersRidingOffset() + offsetY, getZ() + offsetZ);
    }

    private double getPassengersRidingOffset() {
        return getBbHeight() * 0.75D;
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide) {
            // 1.12.2 onLivingUpdate: every player within 20 blocks has found the boss
            for (net.minecraft.world.entity.player.Player player : level().players()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && !player.isSpectator() && player.distanceToSqr(this) < 20.0D * 20.0D) {
                    io.github.reiko45.galacticraft.registry.GCTriggers.FIND_MOON_BOSS.get().trigger(serverPlayer);
                }
            }
            if (this.throwTimer > 0) {
                this.throwTimer--;
            }
            if (this.postThrowDelay > 0) {
                this.postThrowDelay--;
            }
            if (!getPassengers().isEmpty() && this.throwTimer == 0) {
                this.postThrowDelay = 20;
                this.thrownEntity = getPassengers().get(0);
                ejectPassengers();
            }
            if (this.thrownEntity != null && this.postThrowDelay == 18) {
                throwEntity(this.thrownEntity);
                this.thrownEntity = null;
            }
            this.entityData.set(THROW, this.throwTimer + this.postThrowDelay);
        }
        super.aiStep();
    }

    /** 1.12.2: thrown away from the boss, up to 0.4 upwards. */
    private void throwEntity(Entity entity) {
        double dx = getX() - entity.getX();
        double dz = getZ() - entity.getZ();
        while (dx * dx + dz * dz < 1.0E-4D) {
            dx = (Math.random() - Math.random()) * 0.01D;
            dz = (Math.random() - Math.random()) * 0.01D;
        }
        playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F);
        final double distance = Math.sqrt(dx * dx + dz * dz);
        final double strength = 2.4D;
        Vec3 motion = entity.getDeltaMovement().scale(0.5D).add(-dx / distance * strength, strength / 5.0D, -dz / distance * strength);
        motion = new Vec3(motion.x, Math.min(motion.y, 0.4D), motion.z);
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true;
        entity.hasImpulse = true;
    }

    /** 1.12.2 attackEntityWithRangedAttack. */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (!getPassengers().isEmpty()) {
            return;
        }
        final ItemStack bow = getMainHandItem().isEmpty() ? new ItemStack(Items.BOW) : getMainHandItem();
        final AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), velocity, bow);
        final double dx = target.getX() - getX();
        final double dy = target.getY(0.3333333333333333D) - arrow.getY();
        final double dz = target.getZ() - getZ();
        final double distance = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + distance * 0.2D, dz, 1.6F, 14 - level().getDifficulty().getId() * 4);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    /** 1.12.2: the boss's death cry halfway through dying. */
    @Override
    protected void deathSound(int deathTime) {
        if (deathTime == 100) {
            playSound(GCSounds.BOSS_DEATH.get(), 10.0F, 1.5F);
        }
    }

    /** 1.12.2 getGuaranteedLoot: one of GalacticraftRegistry#getDungeonLoot(1), the Moon Buggy or Tier 2 Rocket schematic. */
    public static ItemStack guaranteedLoot(net.minecraft.util.RandomSource random) {
        final List<ItemStack> loot = List.of(new ItemStack(GCItems.BUGGY_SCHEMATIC.get()), new ItemStack(GCItems.TIER_2_ROCKET_SCHEMATIC.get()));
        return loot.get(random.nextInt(loot.size())).copy();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GCSounds.BOSS_OOH.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }
}
