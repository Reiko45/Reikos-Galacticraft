/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityGrapple). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Grapple (1.12.2 EntityGrapple): the hook fired from a Grapple. It flies straight (no gravity, 3 blocks a tick) up to 40
 * blocks; when it bites into a block it reels its shooter in (they move a twelfth of the way to it each tick) until they
 * stop, and if they are close it comes back (with its string) into their inventory. It falls out if its block changes,
 * and lets go after a minute in the ground.
 */
public class GrappleEntity extends Entity {
    private static final EntityDataAccessor<Integer> SHOOTER_ID = SynchedEntityData.defineId(GrappleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PULLING = SynchedEntityData.defineId(GrappleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> STRING = SynchedEntityData.defineId(GrappleEntity.class, EntityDataSerializers.ITEM_STACK);

    @Nullable
    private BlockPos hitPos;
    @Nullable
    private BlockState hitState;
    private boolean inGround;
    /** 1.12.2 canBePickedUp: 1 by anyone (giving back the string), 2 only by a creative player. */
    private int pickup = 1;
    private int shake;
    @Nullable
    private Player shooter;
    private int ticksInGround;
    private int ticksInAir;
    private float roll;
    private float rollO;
    private boolean pullingPlayer;
    /** 1.12.2 kept the shooter's pull in the shooter's (server-side) motion; here it is kept on the hook. */
    private Vec3 pull = Vec3.ZERO;

    public GrappleEntity(EntityType<? extends GrappleEntity> type, Level level) {
        super(type, level);
    }

    /** Fired from {@code shooter}'s eyes along their look at {@code velocity} x 1.5 blocks a tick. */
    public static GrappleEntity shoot(Level level, Player shooter, float velocity, ItemStack string) {
        final GrappleEntity grapple = new GrappleEntity(GCEntities.GRAPPLE.get(), level);
        grapple.shooter = shooter;
        grapple.moveTo(shooter.getX(), shooter.getEyeY(), shooter.getZ(), shooter.getYRot(), shooter.getXRot());
        final float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
        final float pitch = shooter.getXRot() * Mth.DEG_TO_RAD;
        final double mx = -Mth.sin(yaw) * Mth.cos(pitch);
        final double mz = Mth.cos(yaw) * Mth.cos(pitch);
        final double my = -Mth.sin(pitch);
        grapple.setPos(grapple.getX() + mx, grapple.getY() + my, grapple.getZ() + mz);
        grapple.shoot(mx, my, mz, velocity * 1.5F, 1.0F);
        grapple.entityData.set(STRING, string);
        grapple.entityData.set(SHOOTER_ID, shooter.getId());
        return grapple;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHOOTER_ID, 0);
        builder.define(PULLING, false);
        builder.define(STRING, new ItemStack(Items.STRING));
    }

    /** 1.12.2 shoot: the aim spreads by a gaussian with a random sign (0.0075 x inaccuracy). */
    private void shoot(double x, double y, double z, float speed, float inaccuracy) {
        final double length = Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;
        x += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        y += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        z += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.007499999832361937D * inaccuracy;
        setDeltaMovement(x * speed, y * speed, z * speed);
        final double horizontal = Math.sqrt(x * x + z * z);
        setYRot((float) (Mth.atan2(x, z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(y, horizontal) * Mth.RAD_TO_DEG));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.ticksInGround = 0;
    }

    public void setPickup(int pickup) {
        this.pickup = pickup;
    }

    @Nullable
    public Player shooter() {
        return level().getEntity(this.entityData.get(SHOOTER_ID)) instanceof Player player ? player : null;
    }

    public boolean pulling() {
        return this.entityData.get(PULLING);
    }

    public boolean inGround() {
        return this.inGround;
    }

    public ItemStack string() {
        return this.entityData.get(STRING);
    }

    public float roll(float partialTick) {
        return Mth.lerp(partialTick, this.rollO, this.roll);
    }

    @Override
    public void tick() {
        super.tick();
        this.rollO = this.roll;
        if (!level().isClientSide()) {
            if (this.shooter != null) {
                this.entityData.set(SHOOTER_ID, this.shooter.getId());
            }
            if (pulling()) {
                final Player target = shooter();
                if (target != null) {
                    // 1.12.2: the shooter has stopped (their pull is under 0.1 a tick): pick the hook up if close, and let go
                    if (this.pull.lengthSqr() < 0.01D && this.pullingPlayer) {
                        if (distanceToSqr(target) < 10.0D) {
                            playerTouch(target);
                        }
                        this.entityData.set(PULLING, false);
                        discard();
                        return;
                    }
                    this.pullingPlayer = true;
                }
            }
        } else if (pulling()) {
            final Player target = shooter();
            if (target != null && target.isLocalPlayer()) {
                target.setDeltaMovement((getX() - target.getX()) / 12.0D, (getY() - target.getY()) / 12.0D, (getZ() - target.getZ()) / 12.0D);
            }
        }
        if (this.hitPos != null) {
            final BlockState state = level().getBlockState(this.hitPos);
            if (!state.isAir()) {
                final VoxelShape shape = state.getCollisionShape(level(), this.hitPos);
                if (!shape.isEmpty() && shape.bounds().move(this.hitPos).contains(position())) {
                    this.inGround = true;
                }
            }
        }
        if (this.shake > 0) {
            this.shake--;
        }
        if (this.inGround) {
            tickInGround();
        } else {
            tickInAir();
        }
    }

    private void tickInGround() {
        if (this.hitPos == null) {
            return;
        }
        if (level().getBlockState(this.hitPos) == this.hitState) {
            if (this.shooter != null) {
                this.pull = position().subtract(this.shooter.position()).scale(1.0D / 16.0D);
                if (this.shooter instanceof ServerPlayer player) {
                    // 1.12.2 preventFlyingKicks: being reeled in through the air is not flying
                    final var connection = (io.github.reiko45.galacticraft.mixin.ServerGamePacketListenerImplAccessor) player.connection;
                    connection.galacticraftcore$setAboveGroundTickCount(0);
                }
            }
            if (!level().isClientSide() && this.ticksInGround < 5) {
                this.entityData.set(PULLING, true);
            }
            this.ticksInGround++;
            if (this.ticksInGround == 1200) {
                discard();
            }
        } else {
            // its block changed: it falls out
            this.inGround = false;
            setDeltaMovement(getDeltaMovement().multiply(this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F));
            this.ticksInGround = 0;
            this.ticksInAir = 0;
        }
    }

    private void tickInAir() {
        this.roll += 5.0F;
        this.ticksInAir++;
        if (!level().isClientSide()) {
            this.entityData.set(PULLING, false);
        }
        if (this.shooter != null && distanceToSqr(this.shooter) >= 40.0D * 40.0D) {
            discard();
            return;
        }
        Vec3 motion = getDeltaMovement();
        final Vec3 from = position();
        Vec3 to = from.add(motion);
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            to = hit.getLocation();
        }
        // 1.12.2: an entity in the way takes the hit instead, and nothing happens to it (or to the block behind it)
        final List<Entity> entities = level().getEntities(this, getBoundingBox().expandTowards(motion).inflate(1.0D),
                entity -> entity.isPickable() && (entity != this.shooter || this.ticksInAir >= 5));
        for (Entity entity : entities) {
            if (entity.getBoundingBox().inflate(0.3D).clip(from, to).isPresent()) {
                hit = null;
                break;
            }
        }
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            this.hitPos = blockHit.getBlockPos();
            this.hitState = level().getBlockState(this.hitPos);
            motion = blockHit.getLocation().subtract(from);
            setDeltaMovement(motion);
            final double length = motion.length();
            setPos(getX() - motion.x / length * 0.05D, getY() - motion.y / length * 0.05D, getZ() - motion.z / length * 0.05D);
            playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            this.inGround = true;
            this.shake = 7;
            if (!this.hitState.isAir()) {
                this.hitState.entityInside(level(), this.hitPos, this);
            }
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        final double horizontal = motion.horizontalDistance();
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG);
        while (pitch - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }
        while (pitch - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }
        while (yaw - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (yaw - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
        setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
        setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);
        if (isInWater()) {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.BUBBLE, getX() - motion.x * 0.25D, getY() - motion.y * 0.25D, getZ() - motion.z * 0.25D,
                        motion.x, motion.y, motion.z);
            }
        }
        if (isInWaterOrRain()) {
            clearFire();
        }
        checkInsideBlocks();
    }

    /** 1.12.2 onCollideWithPlayer: a hook in the ground goes back to whoever touches it (with its string). */
    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide() || !this.inGround || this.shake > 0) {
            return;
        }
        boolean take = this.pickup == 1 || this.pickup == 2 && player.getAbilities().instabuild;
        if (this.pickup == 1 && !string().isEmpty() && !player.getInventory().add(string().copy())) {
            take = false;
        }
        if (take) {
            playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.take(this, 1);
            discard();
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = getBoundingBox().getSize();
        if (Double.isNaN(size)) {
            size = 1.0D;
        }
        size *= 64.0D * 10.0D;
        return distance < size * size;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        final Player target = shooter();
        return target == null ? super.getBoundingBoxForCulling() : super.getBoundingBoxForCulling().minmax(target.getBoundingBox());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.hitPos != null) {
            tag.put("HitPos", NbtUtils.writeBlockPos(this.hitPos));
        }
        if (this.hitState != null) {
            tag.put("InBlockState", NbtUtils.writeBlockState(this.hitState));
        }
        tag.putShort("life", (short) this.ticksInGround);
        tag.putByte("shake", (byte) this.shake);
        tag.putBoolean("inGround", this.inGround);
        tag.putByte("pickup", (byte) this.pickup);
        if (!string().isEmpty()) {
            tag.put("stringStack", string().save(registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.hitPos = NbtUtils.readBlockPos(tag, "HitPos").orElse(null);
        this.hitState = tag.contains("InBlockState") ? NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("InBlockState"))
                : Blocks.AIR.defaultBlockState();
        this.ticksInGround = tag.getShort("life");
        this.shake = tag.getByte("shake") & 255;
        this.inGround = tag.getBoolean("inGround");
        this.pickup = tag.getByte("pickup");
        this.entityData.set(STRING, ItemStack.parse(registryAccess(), tag.getCompound("stringStack")).orElse(new ItemStack(Items.STRING)));
    }
}
