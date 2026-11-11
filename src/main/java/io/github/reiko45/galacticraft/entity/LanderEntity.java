package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.menu.CargoMenu;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Space Lander (1.12.2 EntityLander on EntityLanderBase): brings a player down to the Moon from y 900 with the rocket's
 * cargo and fuel. After 40 ticks it drops at 2.5 blocks/tick and gravity adds 0.008 per tick; the rider steers with
 * forward/back (pitch, up to 45 degrees) and left/right (yaw), slows with jump (to 1 block/tick, 0.15 below y 90) and
 * speeds up with sprint. Hitting the ground faster than 2 blocks/tick blows it up (explosion 12). Landed, it opens as a
 * chest with the cargo; it can't be left while in the air.
 */
public class LanderEntity extends Entity {
    public static final int KEY_PITCH_DOWN = 1;
    public static final int KEY_PITCH_UP = 2;
    public static final int KEY_YAW_LEFT = 4;
    public static final int KEY_YAW_RIGHT = 8;
    public static final int KEY_SLOW = 16;
    public static final int KEY_FAST = 32;

    /** 1.12.2 EntityLanderBase FUEL_TANK_CAPACITY. */
    public static final int FUEL_CAPACITY = 5000;

    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(LanderEntity.class, EntityDataSerializers.INT);

    private SimpleContainer cargo = new SimpleContainer(3);
    private int keys;
    protected int ticks;
    private double lastMotionY;

    public LanderEntity(EntityType<? extends LanderEntity> type, Level level) {
        super(type, level);
    }

    public static LanderEntity create(Level level, double x, double y, double z, NonNullList<ItemStack> cargo, int fuel) {
        return load(new LanderEntity(GCEntities.LANDER.get(), level), x, y, z, cargo, fuel);
    }

    /** Position it and fill it with the rocket's cargo (plus a slot for a fuel canister) and fuel. */
    protected static <L extends LanderEntity> L load(L lander, double x, double y, double z, NonNullList<ItemStack> cargo, int fuel) {
        final LanderEntity base = lander;
        base.setPos(x, y, z);
        base.cargo = new SimpleContainer(Math.max(3, cargo.size() + 1));
        for (int i = 0; i < cargo.size(); i++) {
            base.cargo.setItem(i, cargo.get(i).copy());
        }
        base.entityData.set(FUEL, fuel);
        return lander;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUEL, 0);
    }

    public int fuel() {
        return this.entityData.get(FUEL);
    }

    public SimpleContainer cargo() {
        return this.cargo;
    }

    /** 1.12.2 checkFluidTankTransfer: fill a canister in the last cargo slot with the leftover fuel. */
    private void fillCanister() {
        final int slot = this.cargo.getContainerSize() - 1;
        final ItemStack stack = this.cargo.getItem(slot);
        if (stack.isEmpty() || fuel() <= 0) {
            return;
        }
        final FluidTank tank = new FluidTank(FUEL_CAPACITY);
        tank.setFluid(new FluidStack(GCFluids.FUEL.get(), fuel()));
        final FluidActionResult result = FluidUtil.tryFillContainer(stack, tank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            this.cargo.setItem(slot, result.getResult());
            this.entityData.set(FUEL, tank.getFluidAmount());
        }
    }

    public void setKeys(int keys) {
        this.keys = keys;
    }

    public boolean inAir() {
        return !onGround();
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
        if (this.level().isClientSide) {
            if (this.ticks > 40 && inAir() && hasThrusters()) {
                RocketEffects.spawnLanderFlames(this);
            }
            return;
        }
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        fillCanister();
        fly();
    }

    /** One server tick of flight (1.12.2 EntityLander#tickInAir and getMotionVec). */
    protected void fly() {
        Vec3 motion = getDeltaMovement();
        if (onGround()) {
            setXRot(0.0F);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (this.ticks < 40) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        double motionY = this.ticks < 45 ? -2.5D : motion.y - 0.008D;
        if (!getPassengers().isEmpty()) {
            float pitch = getXRot();
            float yaw = getYRot();
            if ((this.keys & KEY_PITCH_DOWN) != 0) {
                pitch = Mth.clamp(pitch - 1.0F, -45.0F, 45.0F);
            }
            if ((this.keys & KEY_PITCH_UP) != 0) {
                pitch = Mth.clamp(pitch + 1.0F, -45.0F, 45.0F);
            }
            if ((this.keys & KEY_YAW_LEFT) != 0) {
                yaw -= 1.0F;
            }
            if ((this.keys & KEY_YAW_RIGHT) != 0) {
                yaw += 1.0F;
            }
            if ((this.keys & KEY_SLOW) != 0) {
                motionY = Math.min(motionY + 0.03D, getY() < 90.0D ? -0.15D : -1.0D);
            }
            if ((this.keys & KEY_FAST) != 0) {
                motionY = Math.min(motionY - 0.022D, -1.0D);
            }
            setXRot(pitch);
            setYRot(yaw);
        }
        final double horizontal = -Math.sin(Math.toRadians(getXRot()));
        final double motionX = Math.cos(Math.toRadians(getYRot())) * horizontal / 2.0D;
        final double motionZ = Math.sin(Math.toRadians(getYRot())) * horizontal / 2.0D;
        this.lastMotionY = motionY;
        setDeltaMovement(motionX, motionY, motionZ);
        move(MoverType.SELF, getDeltaMovement());
        if (onGround()) {
            onGroundHit();
        }
    }

    /** 1.12.2 onGroundHit: too fast and it explodes, throwing the rider out. */
    private void onGroundHit() {
        if (Math.abs(this.lastMotionY) > 2.0D) {
            ejectPassengers();
            this.level().explode(this, getX(), getY(), getZ(), 12.0F, Level.ExplosionInteraction.TNT);
            discard();
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hasPassenger(player)) {
            if (onGround()) {
                player.stopRiding();
            }
        } else if (player instanceof ServerPlayer serverPlayer && canOpen()) {
            CargoMenu.openLander(serverPlayer, this);
        }
        return InteractionResult.CONSUME;
    }

    /** Whether it fires exhaust while falling. */
    protected boolean hasThrusters() {
        return true;
    }

    /** Whether a player may open it as a chest. */
    protected boolean canOpen() {
        return true;
    }

    /** Whether hitting it now breaks it. */
    protected boolean canBreak() {
        return !inAir();
    }

    /** Hitting a landed lander breaks it and drops its cargo. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || isRemoved() || !canBreak() || !(source.getEntity() instanceof Player)) {
            return false;
        }
        for (int i = 0; i < this.cargo.getContainerSize(); i++) {
            if (!this.cargo.getItem(i).isEmpty()) {
                spawnAtLocation(this.cargo.getItem(i));
            }
        }
        this.cargo.clearContent();
        discard();
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    /** 1.12.2 getMountedYOffset 2.25. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // 1.12.2: getMountedYOffset (2.25) + player getYOffset (-0.35); 1.21 players already sit 0.6 lower
        return new Vec3(0.0D, 2.25D - 0.35D + 0.6D, 0.0D);
    }

    public boolean keepsRider() {
        return inAir();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D * 256.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        final int size = Math.max(3, tag.getInt("CargoSize"));
        this.cargo = new SimpleContainer(size);
        final NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, this.level().registryAccess());
        for (int i = 0; i < size; i++) {
            this.cargo.setItem(i, items.get(i));
        }
        this.entityData.set(FUEL, tag.getInt("Fuel"));
        this.ticks = 40;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CargoSize", this.cargo.getContainerSize());
        final NonNullList<ItemStack> items = NonNullList.withSize(this.cargo.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, this.cargo.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, this.level().registryAccess());
        tag.putInt("Fuel", fuel());
    }
}
