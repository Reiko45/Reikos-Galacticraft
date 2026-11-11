package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tier 1 Rocket (1.12.2 EntityTier1Rocket on EntityTieredRocket / EntityAutoRocket / EntitySpaceshipBase).
 *
 * <p>Holds 1,000 mB of fuel and 0, 18, 36 or 54 slots of cargo (plus two slots that carry the launch pad and the rocket
 * itself on arrival). Ignited by its rider (jump key), it counts down 400 ticks, takes the launch pad with it, then
 * climbs: speed rises to 1 block/tick over 150 ticks (1.2x, up to 1.6, where there is no atmosphere), burning 1 mB
 * every 3 ticks. Above y 1200 the rider reaches space and picks a destination. Out of fuel it falls back and crashes
 * (explosion 5, 81 damage to the rider). Hitting it damages it; enough damage drops it as an item.
 */
public class Tier1RocketEntity extends Entity implements Refuelable {
    public static final int FUEL_CAPACITY = 1000;
    public static final int PRE_LAUNCH_WAIT = 400;
    public static final double EXIT_HEIGHT = 1200.0D;

    public enum Phase {
        UNIGNITED, IGNITED, LAUNCHED, LANDING
    }

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Tier1RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COUNTDOWN = SynchedEntityData.defineId(Tier1RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(Tier1RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STORAGE = SynchedEntityData.defineId(Tier1RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROLL = SynchedEntityData.defineId(Tier1RocketEntity.class, EntityDataSerializers.INT);

    private SimpleContainer cargo = new SimpleContainer(2);
    private int timeSinceLaunch;
    private float shipDamage;
    private int launchCooldown;

    public Tier1RocketEntity(EntityType<? extends Tier1RocketEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public static Tier1RocketEntity create(Level level, double x, double y, double z, int storage, int fuel) {
        final Tier1RocketEntity rocket = new Tier1RocketEntity(GCEntities.TIER_1_ROCKET.get(), level);
        rocket.setPos(x, y, z);
        rocket.setStorage(storage);
        rocket.setFuel(fuel);
        return rocket;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, Phase.UNIGNITED.ordinal());
        builder.define(COUNTDOWN, PRE_LAUNCH_WAIT);
        builder.define(FUEL, 0);
        builder.define(STORAGE, 0);
        builder.define(ROLL, 0);
    }

    // ------------------------------------------------------------------ state

    public Phase phase() {
        return Phase.values()[this.entityData.get(PHASE)];
    }

    private void setPhase(Phase phase) {
        this.entityData.set(PHASE, phase.ordinal());
    }

    public int countdown() {
        return this.entityData.get(COUNTDOWN);
    }

    public int fuel() {
        return this.entityData.get(FUEL);
    }

    public void setFuel(int fuel) {
        this.entityData.set(FUEL, Math.max(0, Math.min(FUEL_CAPACITY, fuel)));
    }

    /** Fill with fuel (fuel loader); returns the amount taken. */
    @Override
    public int addFuel(int amount, boolean execute) {
        final int accepted = Math.min(amount, FUEL_CAPACITY - fuel());
        if (execute && accepted > 0) {
            setFuel(fuel() + accepted);
        }
        return accepted;
    }

    public boolean hasFuel() {
        return fuel() > 0;
    }

    /** Number of chests built in (0-3): 18 cargo slots each. */
    public int storage() {
        return this.entityData.get(STORAGE);
    }

    public void setStorage(int storage) {
        this.entityData.set(STORAGE, storage);
        final SimpleContainer old = this.cargo;
        this.cargo = new SimpleContainer(storage * 18 + 2);
        for (int i = 0; i < Math.min(old.getContainerSize(), this.cargo.getContainerSize()); i++) {
            this.cargo.setItem(i, old.getItem(i));
        }
    }

    public Container cargo() {
        return this.cargo;
    }

    public int roll() {
        return this.entityData.get(ROLL);
    }

    public boolean launched() {
        return phase().ordinal() >= Phase.LAUNCHED.ordinal();
    }

    // ------------------------------------------------------------------ flight

    /** 1.12.2 igniteCheckingCooldown: start the 400 tick countdown. */
    public void ignite() {
        if (this.launchCooldown <= 0 && phase() == Phase.UNIGNITED) {
            setPhase(Phase.IGNITED);
            this.entityData.set(COUNTDOWN, PRE_LAUNCH_WAIT);
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        if (this.entityData.get(ROLL) > 0) {
            this.entityData.set(ROLL, this.entityData.get(ROLL) - 1);
        }
        if (this.level().isClientSide) {
            if (phase() == Phase.IGNITED || launched() && hasFuel()) {
                RocketEffects.spawnFlames(this);
            }
            return;
        }
        if (this.launchCooldown > 0) {
            this.launchCooldown--;
        }
        if (this.shipDamage > 0) {
            this.shipDamage--;
        }
        if (getY() > EXIT_HEIGHT && phase() != Phase.LANDING) {
            reachSpace();
            return;
        }
        if (getY() < this.level().getMinBuildHeight() - 64) {
            discard();
            return;
        }
        if (launched()) {
            this.timeSinceLaunch++;
        } else {
            this.timeSinceLaunch = 0;
        }
        if (phase() == Phase.IGNITED) {
            final int countdown = countdown() - 1;
            this.entityData.set(COUNTDOWN, countdown);
            if (countdown <= 0) {
                setPhase(Phase.LAUNCHED);
                onLaunch();
            }
        }
        double motionY = 0.0D;
        if (launched()) {
            if (hasFuel()) {
                double speed = this.timeSinceLaunch / 150.0D;
                speed = CelestialPhysics.isAirless(this.level()) ? Math.min(speed * 1.2D, 1.6D) : Math.min(speed, 1.0D);
                motionY = speed;
                if (this.timeSinceLaunch % 3 == 0) {
                    setFuel(fuel() - 1);
                }
            } else {
                motionY = getDeltaMovement().y - Math.abs(Math.sin(this.timeSinceLaunch / 1000.0D)) / 20.0D - 0.04D;
            }
            if (this.timeSinceLaunch > 50 && onGround()) {
                crash();
                return;
            }
        }
        setDeltaMovement(0.0D, motionY, 0.0D);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** 1.12.2 EntityAutoRocket#onLaunch: the launch pad goes into the rocket, the take-off point is remembered. */
    private void onLaunch() {
        // The rocket stands 0.4 above the pad block it was placed on.
        final BlockPos below = BlockPos.containing(getX(), getY() - 0.1D, getZ());
        final BlockPos centre = LandingPadBlock.findLaunchPadCentre(this.level(), below);
        boolean padTaken = false;
        if (centre != null) {
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
                this.level().removeBlock(pos, false);
            }
            padTaken = true;
        }
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                SpaceTravel.onLaunch(player, padTaken);
            }
        }
        playSound(SoundEvents.ITEM_PICKUP, 0.2F, 2.0F);
    }

    /** Above y 1200: the riders go to the destination screen and the rocket travels with them as cargo. */
    private void reachSpace() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                SpaceTravel.reachSpace(player, this);
            }
        }
        discard(); // riders are let off as it goes
    }

    /** 1.12.2 failRocket: explosion 5, 81 damage to the riders. */
    private void crash() {
        for (Entity passenger : getPassengers()) {
            passenger.hurt(GCDamageTypes.crash(this.level()), 81.0F);
        }
        this.level().explode(this, getX(), getY(), getZ(), 5.0F, Level.ExplosionInteraction.TNT);
        discard();
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || launched()) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hasPassenger(player)) {
            player.stopRiding();
        } else if (getPassengers().isEmpty()) {
            player.startRiding(this);
            player.displayClientMessage(Component.translatable("gui.galacticraftcore.rocket.controls", Component.keybind("key.jump"),
                    Component.keybind("key.galacticraftcore.spaceship_inventory")), true);
        }
        return InteractionResult.CONSUME;
    }

    /** 1.12.2 attackEntityFrom: rocks and drops as an item after enough hits; creative players remove it at once. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || isRemoved() || getY() > 300 || isInvulnerableTo(source)) {
            return false;
        }
        final boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        if (source.getEntity() != null && !(source.getEntity() instanceof Player)) {
            return false;
        }
        this.entityData.set(ROLL, 10);
        markHurt();
        this.shipDamage += amount * 10.0F;
        if (creative || this.shipDamage > 90.0F) {
            ejectPassengers();
            if (!creative) {
                dropAsItem();
            }
            discard();
        }
        return true;
    }

    public void dropAsItem() {
        if (this.level() instanceof ServerLevel) {
            spawnAtLocation(toItem());
            for (int i = 0; i < this.cargo.getContainerSize(); i++) {
                if (!this.cargo.getItem(i).isEmpty()) {
                    spawnAtLocation(this.cargo.getItem(i));
                }
            }
            this.cargo.clearContent();
        }
    }

    public ItemStack toItem() {
        return Tier1RocketItem.create(storage(), fuel());
    }

    @Override
    public ItemStack getPickResult() {
        return Tier1RocketItem.create(storage(), 0);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    /** 1.12.2 getMountedYOffset 0.3 with the player's -0.35: the rider stands at the rocket's floor. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale) {
        // 1.12.2: rider feet at getMountedYOffset (0.3) + player getYOffset (-0.35); 1.21 players already sit 0.6 lower
        return new Vec3(0.0D, 0.3D - 0.35D + 0.6D, 0.0D);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    public boolean keepsRider() {
        return phase() != Phase.UNIGNITED;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 320.0D * 320.0D;
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStorage(tag.getInt("Storage"));
        setFuel(tag.getInt("Fuel"));
        setPhase(Phase.values()[Math.min(tag.getInt("Phase"), Phase.values().length - 1)]);
        this.entityData.set(COUNTDOWN, tag.contains("Countdown") ? tag.getInt("Countdown") : PRE_LAUNCH_WAIT);
        this.timeSinceLaunch = tag.getInt("TimeSinceLaunch");
        final NonNullList<ItemStack> items = NonNullList.withSize(this.cargo.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registryAccess());
        for (int i = 0; i < items.size(); i++) {
            this.cargo.setItem(i, items.get(i));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Storage", storage());
        tag.putInt("Fuel", fuel());
        tag.putInt("Phase", phase().ordinal());
        tag.putInt("Countdown", countdown());
        tag.putInt("TimeSinceLaunch", this.timeSinceLaunch);
        final NonNullList<ItemStack> items = NonNullList.withSize(this.cargo.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, this.cargo.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, registryAccess());
    }
}
