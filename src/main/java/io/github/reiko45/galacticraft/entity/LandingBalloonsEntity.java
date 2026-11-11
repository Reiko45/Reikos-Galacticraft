package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Landing Balloons (1.12.2 EntityLandingBalloons on EntityLanderBase): how a player arrives on Mars. A ball of airbags
 * around the rider drops from y 900, falling a fiftieth of its height each tick (so it slows as it nears the ground),
 * then bounces 14 times: the n-th bounce leaves the ground at 4/n blocks/tick with a random sideways kick, and in the
 * air it tumbles and falls with 0.08 gravity and 5% drag. Only after the last bounce can the rider get out; then it
 * opens as a chest with the rocket's cargo, like the Moon lander. It can't be damaged before its first bounce.
 */
public class LandingBalloonsEntity extends LanderEntity {
    public static final int BOUNCES = 14;
    private static final EntityDataAccessor<Integer> GROUND_HITS = SynchedEntityData.defineId(LandingBalloonsEntity.class, EntityDataSerializers.INT);

    private final float pitchSpeed;
    private final float yawSpeed;

    public LandingBalloonsEntity(EntityType<? extends LandingBalloonsEntity> type, Level level) {
        super(type, level);
        this.pitchSpeed = this.random.nextFloat();
        this.yawSpeed = this.random.nextFloat();
    }

    public static LandingBalloonsEntity create(Level level, double x, double y, double z, NonNullList<ItemStack> cargo, int fuel) {
        return load(new LandingBalloonsEntity(GCEntities.LANDING_BALLOONS.get(), level), x, y, z, cargo, fuel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROUND_HITS, 0);
    }

    public int groundHits() {
        return this.entityData.get(GROUND_HITS);
    }

    public boolean landed() {
        return groundHits() >= BOUNCES && onGround();
    }

    /** 1.12.2 tickInAir + getMotionVec; the first 45 ticks it waits for the ground below to load. */
    @Override
    protected void fly() {
        if (this.ticks < 45) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 motion = getDeltaMovement();
        final int hits = groundHits();
        if (onGround()) {
            if (getPassengers().isEmpty() || hits >= BOUNCES) {
                setDeltaMovement(Vec3.ZERO);
                setXRot(0.0F);
                return;
            }
            this.entityData.set(GROUND_HITS, hits + 1);
            final double magnitude = 4.0D / (hits + 1);
            motion = new Vec3((this.random.nextDouble() - 0.5D) * magnitude / 3.0D, magnitude, (this.random.nextDouble() - 0.5D) * magnitude / 3.0D);
        } else if (hits == 0) {
            // 1.12.2: -posY / 50, worlds starting at y 0
            motion = new Vec3(motion.x, -(getY() - level().getMinBuildHeight()) / 50.0D, motion.z);
        } else {
            motion = new Vec3(motion.x, motion.y * 0.95D - 0.08D, motion.z);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (!onGround()) {
            setXRot(getXRot() + this.pitchSpeed);
            setYRot(getYRot() + this.yawSpeed);
        }
    }

    /** 1.12.2 shouldSpawnParticles: airbags have no thrusters. */
    @Override
    protected boolean hasThrusters() {
        return false;
    }

    @Override
    protected boolean canOpen() {
        return groundHits() >= BOUNCES;
    }

    @Override
    protected boolean canBreak() {
        return groundHits() > 0;
    }

    /** 1.12.2 shouldIgnoreShiftExit: no getting out until it has stopped bouncing. */
    @Override
    public boolean keepsRider() {
        return !landed();
    }

    /** 1.12.2 getMountedYOffset: the lander's 2.25 less 0.9, with the player's -0.35 (1.21 players sit 0.6 lower). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0D, 2.25D - 0.9D - 0.35D + 0.6D, 0.0D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(GROUND_HITS, tag.getInt("GroundHitCount"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GroundHitCount", groundHits());
    }
}
