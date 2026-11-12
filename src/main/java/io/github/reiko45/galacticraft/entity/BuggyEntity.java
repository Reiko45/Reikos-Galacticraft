package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.item.BuggyItem;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Moon Buggy (1.12.2 EntityBuggy). W/S speed up and slow down (0.01 per tick up to 0.5), A/D turn 1.5 degrees per tick;
 * driving into a wall while accelerating makes it climb. It only moves while it has fuel and burns about 1 mB per 2
 * blocks driven. As in 1.12.2 the driver's client moves it (the vanilla vehicle packets carry it to the server). Up to
 * three storage boxes hold 18 slots each; the inventory key opens them. One hit breaks it into its item (fuel kept),
 * creative players just remove it. A Buggy Fueling Pad with a Fuel Loader refuels it.
 */
public class BuggyEntity extends Entity implements Refuelable, CargoCarrier {
    public static final int FUEL_CAPACITY = 1000;
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(BuggyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STORAGE = SynchedEntityData.defineId(BuggyEntity.class, EntityDataSerializers.INT);
    private static final float MAX_SPEED = 0.5F;
    private static final float ACCEL = 0.2F;
    private static final float TURN_FACTOR = 3.0F;

    private SimpleContainer cargo = new SimpleContainer(0);
    private double speed;
    private int timeClimbing;
    private boolean shouldClimb;
    private int damage;
    private long ticks;
    private double lastX;
    private double lastZ;
    /** Client: wheel spin and steering, in degrees. */
    public float wheelRotationX;
    public float wheelRotationZ;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;

    public BuggyEntity(EntityType<? extends BuggyEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public static BuggyEntity create(Level level, double x, double y, double z, int storage, int fuel) {
        final BuggyEntity buggy = new BuggyEntity(GCEntities.BUGGY.get(), level);
        buggy.setPos(x, y, z);
        buggy.setStorage(storage);
        buggy.setFuel(Math.min(fuel, FUEL_CAPACITY));
        buggy.lastX = x;
        buggy.lastZ = z;
        return buggy;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUEL, 0);
        builder.define(STORAGE, 0);
    }

    /** Storage boxes built in (0-3), 18 slots each. */
    public int storage() {
        return this.entityData.get(STORAGE);
    }

    private void setStorage(int storage) {
        this.entityData.set(STORAGE, Mth.clamp(storage, 0, 3));
        final SimpleContainer resized = new SimpleContainer(storage() * 18);
        for (int i = 0; i < Math.min(resized.getContainerSize(), this.cargo.getContainerSize()); i++) {
            resized.setItem(i, this.cargo.getItem(i));
        }
        this.cargo = resized;
    }

    public SimpleContainer cargo() {
        return this.cargo;
    }

    /** 1.12.2 EntityBuggy#addCargo, loaded through its fueling pad. */
    @Override
    public LoadState addCargo(ItemStack stack, boolean doAdd) {
        return CargoCarrier.add(this.cargo, this.cargo.getContainerSize(), stack, doAdd);
    }

    @Override
    public Removal removeCargo(boolean doRemove) {
        return CargoCarrier.remove(this.cargo, this.cargo.getContainerSize(), doRemove);
    }

    public int fuel() {
        return this.entityData.get(FUEL);
    }

    public void setFuel(int fuel) {
        this.entityData.set(FUEL, Mth.clamp(fuel, 0, FUEL_CAPACITY));
    }

    @Override
    public int addFuel(int amount, boolean execute) {
        final int accepted = Math.min(amount, FUEL_CAPACITY - fuel());
        if (execute && accepted > 0) {
            setFuel(fuel() + accepted);
        }
        return accepted;
    }

    public double speed() {
        return this.speed;
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
        if (level().isClientSide) {
            final Vec3 motion = getDeltaMovement();
            this.wheelRotationX += (float) (Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 150.0D * (this.speed < 0 ? 1 : -1));
            this.wheelRotationX %= 360.0F;
            this.wheelRotationZ = Mth.clamp(this.wheelRotationZ * 0.9F, -30.0F, 30.0F);
        }
        if (isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
            drive();
        } else {
            tickLerp();
        }
        if (!level().isClientSide) {
            if (this.damage > 0) {
                this.damage--;
            }
            burnFuel();
        }
    }

    /** 1.12.2 onUpdate on the server or the driver's client. */
    private void drive() {
        if (level().isClientSide && !level().hasChunkAt(blockPosition())) {
            // The driver's client hasn't got the ground here yet: wait instead of falling through it (as players do)
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        final Vec3 motion = getDeltaMovement();
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        if (!onGround()) {
            motionY -= gravity() * 0.5D;
        }
        if (isInWater() && this.speed > 0.2D) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_BURN, SoundSource.NEUTRAL, 0.5F,
                    2.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.8F);
        }
        this.speed *= 0.98D;
        if (getControllingPassenger() instanceof Player driver) {
            // 1.12.2 pressKey 0-3 (accelerate, decelerate, left, right), with the movement keys
            if (driver.zza > 0.0F) {
                this.speed += ACCEL / 20.0D;
                this.shouldClimb = true;
            } else if (driver.zza < 0.0F) {
                this.speed -= ACCEL / 20.0D;
                this.shouldClimb = true;
            }
            if (driver.xxa > 0.0F) {
                setYRot(getYRot() - 0.5F * TURN_FACTOR);
                this.wheelRotationZ = Mth.clamp(this.wheelRotationZ + 0.5F, -30.0F, 30.0F);
            } else if (driver.xxa < 0.0F) {
                setYRot(getYRot() + 0.5F * TURN_FACTOR);
                this.wheelRotationZ = Mth.clamp(this.wheelRotationZ - 0.5F, -30.0F, 30.0F);
            }
        }
        if (this.speed > MAX_SPEED) {
            this.speed = MAX_SPEED;
        }
        if (this.horizontalCollision && this.shouldClimb) {
            this.speed *= 0.9D;
            motionY = 0.15D * (-Math.pow(this.timeClimbing - 1, 2) / 250.0F) + 0.15F;
            motionY = Math.max(-0.15D, motionY);
        }
        this.shouldClimb = false;
        if ((motionX == 0 || motionZ == 0) && !onGround()) {
            this.timeClimbing++;
        } else {
            this.timeClimbing = 0;
        }
        if (fuel() > 0) {
            motionX = -(this.speed * Math.cos((getYRot() - 90.0F) * Mth.DEG_TO_RAD));
            motionZ = -(this.speed * Math.sin((getYRot() - 90.0F) * Mth.DEG_TO_RAD));
        } else if (onGround()) {
            // Out of fuel: roll to a stop instead of the 1.12.2 frictionless coast
            motionX *= 0.5D;
            motionZ *= 0.5D;
            this.speed = 0.0D;
        }
        setDeltaMovement(motionX, motionY, motionZ);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** 1.12.2: TransformerHooks#getGravityForEntity, 0.08 less the body's gravity reduction. */
    private double gravity() {
        final CelestialPhysics physics = CelestialPhysics.of(level().dimension());
        return 0.08D - (physics == null ? 0.0D : physics.gravityReduction());
    }

    /** 1.12.2: 1 mB every floor(2 / v^2) + 1 ticks, v being the distance moved per tick. */
    private void burnFuel() {
        final double dx = getX() - this.lastX;
        final double dz = getZ() - this.lastZ;
        this.lastX = getX();
        this.lastZ = getZ();
        if (Math.abs(dx * dz) > 0.000001D) {
            final double d = dx * dx + dz * dz;
            if (this.ticks % (Mth.floor(2.0D / d) + 1) == 0) {
                setFuel(fuel() - 1);
            }
        }
    }

    private void tickLerp() {
        if (this.lerpSteps > 0) {
            final double x = getX() + (this.lerpX - getX()) / this.lerpSteps;
            final double y = getY() + (this.lerpY - getY()) / this.lerpSteps;
            final double z = getZ() + (this.lerpZ - getZ()) / this.lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(this.lerpYRot - getYRot()) / this.lerpSteps);
            this.lerpSteps--;
            setPos(x, y, z);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpSteps = 10;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : getYRot();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    /** 1.12.2 updatePassenger: the seat is half a block off-centre. */
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) {
            return;
        }
        final double angle = getYRot() * Mth.DEG_TO_RAD + 114.8D;
        callback.accept(passenger, getX() + Math.cos(angle) * -0.5D, getY() + 0.4D - 0.35D, getZ() + Math.sin(angle) * -0.5D);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            if (getPassengers().isEmpty()) {
                io.github.reiko45.galacticraft.client.VehicleControls.buggyHelp(player);
            }
            return InteractionResult.SUCCESS;
        }
        if (hasPassenger(player)) {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }
        if (getPassengers().isEmpty() && player.startRiding(this)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** 1.12.2 attackEntityFrom: players break it (creative players remove it), mobs and cacti can't. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isRemoved() || source.is(DamageTypes.CACTUS)) {
            return true;
        }
        final Entity attacker = source.getEntity();
        if (isInvulnerableTo(source) || attacker instanceof LivingEntity && !(attacker instanceof Player)) {
            return false;
        }
        if (level().isClientSide) {
            return true;
        }
        final boolean creative = attacker instanceof Player player && player.getAbilities().instabuild;
        this.damage += (int) (amount * 10.0F);
        markHurt();
        if (creative || this.damage > 2) {
            ejectPassengers();
            if (!creative) {
                spawnAtLocation(BuggyItem.create(storage(), fuel()));
                Containers.dropContents(level(), blockPosition(), this.cargo);
            }
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return BuggyItem.create(storage(), 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Storage", storage());
        tag.putInt("Fuel", fuel());
        final NonNullList<ItemStack> items = NonNullList.withSize(this.cargo.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, this.cargo.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, registryAccess());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.cargo = new SimpleContainer(0);
        setStorage(tag.getInt("Storage"));
        setFuel(tag.getInt("Fuel"));
        final NonNullList<ItemStack> items = NonNullList.withSize(this.cargo.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registryAccess());
        for (int i = 0; i < items.size(); i++) {
            this.cargo.setItem(i, items.get(i));
        }
        this.lastX = getX();
        this.lastZ = getZ();
    }
}
