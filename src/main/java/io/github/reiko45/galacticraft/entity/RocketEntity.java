package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
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
 * A rider-flown rocket (1.12.2 EntityTieredRocket / EntityAutoRocket / EntitySpaceshipBase); the tiers set its tank,
 * climb rate, burn rate and looks.
 *
 * <p>Holds 0, 18, 36 or 54 slots of cargo (plus two slots that carry the launch pad and the rocket itself on arrival).
 * Ignited by its rider (jump key), it counts down 400 ticks, takes the launch pad with it, then climbs, its speed rising
 * over 150 ticks to the tier's top speed (1.2x as fast, to a higher limit, where there is no atmosphere). Above y 1200
 * the rider reaches space and picks a destination as far as the rocket's tier allows. Out of fuel it falls back and
 * crashes (explosion 5, 81 damage to the rider). Hitting it damages it; enough damage drops it as an item.
 */
public abstract class RocketEntity extends Entity implements Refuelable, CargoCarrier, Telemetry {
    public static final int PRE_LAUNCH_WAIT = 400;
    public static final double EXIT_HEIGHT = 1200.0D;

    public enum Phase {
        UNIGNITED, IGNITED, LAUNCHED, LANDING
    }

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COUNTDOWN = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STORAGE = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROLL = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    private SimpleContainer cargo = new SimpleContainer(2);
    private int timeSinceLaunch;
    private float shipDamage;
    private int launchCooldown;

    protected RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    protected static <R extends RocketEntity> R place(R rocket, double x, double y, double z, int storage, int fuel) {
        rocket.setPos(x, y, z);
        rocket.setStorage(storage);
        // the creative-only pre-fuelled rocket comes full, whatever the fuel factor (1.12.2 PREFUELED filled the tank)
        rocket.setFuel(storage == 0 && fuel >= rocket.fuelCapacity() ? rocket.tankCapacity() : fuel);
        return rocket;
    }

    // ------------------------------------------------------------------ tier

    /** 1.12.2 getRocketTier: how far it can fly. */
    public abstract int tier();

    /** 1.12.2 getFuelTankCapacity, in mB. */
    public abstract int fuelCapacity();

    /**
     * What the tank really holds: the tier's capacity times the {@code rocketFuelFactor} setting, which also multiplies
     * every mB burned (1.12.2 EntitySpaceshipBase#fuelTank), so a flight takes that many times the fuel.
     */
    public int tankCapacity() {
        return fuelCapacity() * fuelFactor();
    }

    public static int fuelFactor() {
        return io.github.reiko45.galacticraft.GCConfig.server(io.github.reiko45.galacticraft.GCConfig.ROCKET_FUEL_FACTOR);
    }

    /** The rocket as an item with this much storage and fuel. */
    public abstract ItemStack item(int storage, int fuel);

    /** The cap on the thrust that builds up over 150 ticks, in an atmosphere and in a vacuum. */
    protected abstract double speedCap(boolean airless);

    /** Climb speed in blocks/tick per unit of thrust. */
    protected double climbFactor() {
        return 1.0D;
    }

    /** 1.12.2 burned 1 mB every floor(this / fuel usage multiplier) ticks of climbing. */
    protected abstract double burnTicks();

    /** Where the rider's feet are above the rocket's position. */
    protected abstract double riderHeight();

    /** Exhaust while counting down and climbing (client side). */
    protected abstract void spawnFlames();

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

    protected void setPhase(Phase phase) {
        this.entityData.set(PHASE, phase.ordinal());
    }

    public int countdown() {
        return this.entityData.get(COUNTDOWN);
    }

    public int fuel() {
        return this.entityData.get(FUEL);
    }

    public void setFuel(int fuel) {
        this.entityData.set(FUEL, Math.max(0, Math.min(tankCapacity(), fuel)));
    }

    /** Fill with fuel (fuel loader); returns the amount taken. */
    @Override
    public int addFuel(int amount, boolean execute) {
        final int accepted = Math.min(amount, tankCapacity() - fuel());
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

    /**
     * 1.12.2 ModelBipedGC: a player within 14 blocks (distance squared under 200) of a crewed rocket they are not in
     * waves at it once it has launched or its countdown is under 390 (it starts at 400).
     */
    public boolean wavedAtBy(Player onlooker) {
        return distanceToSqr(onlooker) < 200.0D && !getPassengers().isEmpty() && !hasPassenger(onlooker)
                && (launched() || phase() == Phase.IGNITED && countdown() < 390);
    }

    // ------------------------------------------------------------------ flight

    /** 1.12.2 getPreLaunchWait: the countdown once ignited. */
    protected int preLaunchWait() {
        return PRE_LAUNCH_WAIT;
    }

    protected int timeSinceLaunch() {
        return this.timeSinceLaunch;
    }

    /** 1.12.2 igniteCheckingCooldown: start the countdown. */
    public void ignite() {
        if (this.launchCooldown <= 0 && phase() == Phase.UNIGNITED) {
            setPhase(Phase.IGNITED);
            this.entityData.set(COUNTDOWN, preLaunchWait());
        }
    }

    /** Back on a pad (a cargo rocket that has landed): not launched, nothing counted down. */
    protected void resetFlight() {
        setPhase(Phase.UNIGNITED);
        this.entityData.set(COUNTDOWN, preLaunchWait());
        this.timeSinceLaunch = 0;
    }

    /** Climbing with fuel: the thrust builds over 150 ticks to the tier's top speed (1.2x as fast with no air). */
    protected Vec3 climbMotion() {
        final double thrust = this.timeSinceLaunch / 150.0D;
        final boolean airless = CelestialPhysics.isAirless(this.level());
        return new Vec3(0.0D, climbFactor() * Math.min(airless ? thrust * 1.2D : thrust, speedCap(airless)), 0.0D);
    }

    /** Coming down in the landing phase with fuel (only cargo rockets land by themselves). */
    protected Vec3 landingMotion() {
        return new Vec3(0.0D, -1.0D, 0.0D);
    }

    // Client: glide to each position the server sends, as boats do, instead of jumping to it. A rocket only moves on
    // the server; jumping a block or more each tick left it jerking about against its rider's smoothly moving camera.
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
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

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : getXRot();
    }

    private void tickLerp() {
        if (this.lerpSteps > 0) {
            lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
    }

    /** In the landing phase: touched down on the pad (and handled it), so the tick stops here. */
    protected boolean tryLand() {
        return false;
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
            tickLerp();
            io.github.reiko45.galacticraft.client.sound.RocketSound.ensure(this);
            if (RocketEffects.showsExhaust(this)) {
                spawnFlames();
            }
            return;
        }
        if (this.launchCooldown > 0) {
            this.launchCooldown--;
        }
        if ((phase() == Phase.IGNITED || launched()) && hasFuel() && this.tickCount % 5 == 0) {
            RocketEffects.burn(this);
        }
        if (this.shipDamage > 0) {
            this.shipDamage--;
        }
        if (getY() > CelestialPhysics.exitHeight(this.level()) && phase() != Phase.LANDING) {
            reachSpace();
            return;
        }
        if (getY() < this.level().getMinBuildHeight() - 64) {
            discard();
            return;
        }
        if (launched()) {
            this.timeSinceLaunch++;
            // 1.12.2 EntitySpaceshipBase: whoever rides a launched rocket has launched one
            for (net.minecraft.world.entity.Entity passenger : getPassengers()) {
                if (passenger instanceof net.minecraft.server.level.ServerPlayer player) {
                    io.github.reiko45.galacticraft.registry.GCTriggers.LAUNCH_ROCKET.get().trigger(player);
                }
            }
        } else {
            this.timeSinceLaunch = 0;
        }
        if (phase() == Phase.IGNITED) {
            final int countdown = countdown() - 1;
            this.entityData.set(COUNTDOWN, countdown);
            if (countdown <= 0 && !io.github.reiko45.galacticraft.GCConfig.rocketsLaunchFrom(this.level().dimension())) {
                cancelLaunch();
            } else if (countdown <= 0) {
                setPhase(Phase.LAUNCHED);
                onLaunch();
            }
        }
        Vec3 motion = Vec3.ZERO;
        if (launched()) {
            if (hasFuel()) {
                motion = phase() == Phase.LANDING ? landingMotion() : climbMotion();
                if (this.timeSinceLaunch % CelestialPhysics.fuelInterval(this.level(), burnTicks()) == 0) {
                    setFuel(fuel() - fuelFactor());
                }
            } else {
                motion = new Vec3(0.0D, getDeltaMovement().y - Math.abs(Math.sin(this.timeSinceLaunch / 1000.0D)) / 20.0D - 0.04D, 0.0D);
            }
            if (phase() == Phase.LANDING && hasFuel() && tryLand()) {
                return;
            }
            if (this.timeSinceLaunch > 50 && onGround()) {
                crash();
                return;
            }
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** 1.12.2 EntityAutoRocket#cancelLaunch: no launching from this world (disableRocketLaunchDimensions). */
    private void cancelLaunch() {
        resetFlight();
        if (getFirstPassenger() instanceof ServerPlayer player) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.galacticraftcore.rocket.warning.no_gyroscope"));
        }
    }

    /** 1.12.2 EntityAutoRocket#onLaunch: the launch pad goes into the rocket, the take-off point is remembered. */
    private void onLaunch() {
        // The rocket stands 0.4 above the pad block it was placed on.
        final BlockPos below = BlockPos.containing(getX(), getY() - 0.1D, getZ());
        final BlockPos centre = LandingPadBlock.findLaunchPadCentre(this.level(), below);
        boolean padTaken = false;
        // 1.12.2 EventLandingPadRemoval: a powered Launch Controller beside the pad can keep it in place
        if (centre != null && !io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity.keepsPad(this.level(), centre)) {
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

    /** Above y 1200 (750 in orbit): the riders go to the destination screen and the rocket travels with them as cargo. */
    protected void reachSpace() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                SpaceTravel.reachSpace(player, this);
            }
        }
        discard(); // riders are let off as it goes
    }

    /** 1.12.2 failRocket: explosion 5, 81 damage to the riders. */
    public void crash() {
        for (Entity passenger : getPassengers()) {
            passenger.hurt(GCDamageTypes.crash(this.level()), 81.0F);
        }
        // disableSpaceshipGrief: the riders are still hurt, but nothing is blown up
        if (!io.github.reiko45.galacticraft.GCConfig.server(io.github.reiko45.galacticraft.GCConfig.DISABLE_SPACESHIP_GRIEF)) {
            this.level().explode(this, getX(), getY(), getZ(), 5.0F, Level.ExplosionInteraction.TNT);
        }
        discard();
    }

    // ------------------------------------------------------------------ cargo (Cargo Loader / Unloader)

    /** The cargo slots, without the two kept for the launch pad and the rocket itself (1.12.2 stacks.size() - 2). */
    protected int cargoSlots() {
        return this.cargo.getContainerSize() - 2;
    }

    /** 1.12.2 EntityAutoRocket#addCargo: rockets without storage have no inventory to load. */
    @Override
    public LoadState addCargo(ItemStack stack, boolean doAdd) {
        return CargoCarrier.add(this.cargo, cargoSlots(), stack, doAdd);
    }

    @Override
    public Removal removeCargo(boolean doRemove) {
        return CargoCarrier.remove(this.cargo, cargoSlots(), doRemove);
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
        return item(storage(), fuel());
    }

    @Override
    public ItemStack getPickResult() {
        return item(storage(), 0);
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

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale) {
        return new Vec3(0.0D, riderHeight(), 0.0D);
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

    // ------------------------------------------------------------------ telemetry (1.12.2 EntitySpaceshipBase)

    /** Countdown (the full wait while still on the pad), height, speed (already set), fuel %, pitch. */
    @Override
    public void transmitData(int[] data) {
        data[0] = phase() == Phase.UNIGNITED ? preLaunchWait() : phase() == Phase.IGNITED ? countdown() : 0;
        data[1] = (int) getY();
        data[3] = tankCapacity() <= 0 ? 0 : fuel() * 100 / tankCapacity();
        data[4] = (int) getXRot();
    }

    @Override
    public void receiveData(int[] data, net.minecraft.network.chat.Component[] lines) {
        final int countdown = data[0];
        lines[0] = net.minecraft.network.chat.Component.empty();
        lines[1] = countdown == preLaunchWait() ? net.minecraft.network.chat.Component.translatable("gui.galacticraftcore.rocket.on_launchpad")
                : countdown > 0 ? net.minecraft.network.chat.Component.translatable("gui.galacticraftcore.rocket.countdown").append(": " + countdown / 20)
                : net.minecraft.network.chat.Component.translatable("gui.galacticraftcore.rocket.launched");
        lines[2] = net.minecraft.network.chat.Component.translatable("gui.galacticraftcore.rocket.height").append(": " + data[1]);
        lines[3] = io.github.reiko45.galacticraft.client.screen.display.GameScreenText.speed(data[2]);
        lines[4] = net.minecraft.network.chat.Component.translatable("gui.galacticraftcore.message.fuel").append(": " + data[3] + "%");
    }

    @Override
    public void adjustDisplay(int[] data, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.mulPose(com.mojang.math.Axis.XN.rotationDegrees(data[4]));
        poseStack.translate(0.0F, getBbHeight() / 4, 0.0F);
    }
}
