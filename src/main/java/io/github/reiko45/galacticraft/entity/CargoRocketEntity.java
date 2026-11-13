/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.entities.EntityCargoRocket and
 * api.prefab.entity.EntityAutoRocket). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity.AutoLaunch;
import io.github.reiko45.galacticraft.menu.CargoMenu;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Cargo Rocket (1.12.2 EntityCargoRocket): an unmanned rocket with 18, 36 or 54 slots and a 2,000 mB tank. On a pad with
 * a Launch Controller it flies to the pad of the controller whose frequency that controller targets, on any world: the
 * Launch button (or the controller's auto-launch rule) counts 20 ticks, it climbs (up to 5 / (1 + 0.1 per filled slot)
 * blocks a tick, reached over 250 ticks), and on reaching space it reappears 800 blocks above the target pad and lands on
 * it, slowing as it nears. It burns 1 mB every 3 ticks, landing included; out of fuel it falls and crashes. Loaders and
 * unloaders fill and empty it; right-click opens its cargo.
 */
public class CargoRocketEntity extends RocketEntity {
    public static final int FUEL_CAPACITY = 2000;
    /** 1.12.2 getOnPadYOffset. */
    public static final double PAD_OFFSET = -0.05D;
    public static final double ARRIVAL_HEIGHT = 800.0D;
    /** Keeps a flying cargo rocket's chunk loaded, refreshed while it flies (1.12.2 relied on launch controller chunk loading). */
    private static final TicketType<ChunkPos> FLIGHT_TICKET = TicketType.create("galacticraftcore_cargo_rocket", Comparator.comparingLong(ChunkPos::toLong), 100);

    /** 1.12.2 statusMessage values shown by the cargo GUI. */
    public static final int STATUS_NONE = 0;
    public static final int STATUS_FREQUENCY_NOT_SET = 1;
    public static final int STATUS_NOT_ENOUGH_FUEL = 2;
    public static final int STATUS_SUCCESS = 3;

    private int destinationFrequency = -1;
    @Nullable
    private BlockPos target;
    @Nullable
    private ResourceKey<Level> targetDimension;
    @Nullable
    private AutoLaunch autoLaunchSetting;
    private int autoLaunchCountdown;
    @Nullable
    private BlockPos activeLaunchController;
    private int statusCode = STATUS_NONE;
    private int statusCooldown;
    private int lastStatusCooldown;
    private boolean statusValid;
    private int ticks;

    public CargoRocketEntity(EntityType<? extends CargoRocketEntity> type, Level level) {
        super(type, level);
    }

    public static CargoRocketEntity create(Level level, double x, double y, double z, int storage, int fuel) {
        return place(new CargoRocketEntity(GCEntities.CARGO_ROCKET.get(), level), x, y, z, storage, fuel);
    }

    // ------------------------------------------------------------------ the rocket

    /** 1.12.2 getRocketTier: Integer.MAX_VALUE (it goes wherever its controller points). */
    @Override
    public int tier() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int fuelCapacity() {
        return FUEL_CAPACITY;
    }

    @Override
    public ItemStack item(int storage, int fuel) {
        return GCItems.CARGO_ROCKET.get().stack(storage, fuel);
    }

    @Override
    protected double speedCap(boolean airless) {
        return 1.0D;
    }

    @Override
    protected double burnTicks() {
        return 3.0D;
    }

    @Override
    protected double riderHeight() {
        return 0.0D;
    }

    /** 1.12.2 getPreLaunchWait. */
    @Override
    protected int preLaunchWait() {
        return 20;
    }

    @Override
    protected void spawnFlames() {
        RocketEffects.spawnFlames(this, 1);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    /** 1.12.2 getCargoFilledAmount: 1 + 0.1 for every filled slot. */
    public double weight() {
        double weight = 1.0D;
        for (int i = 0; i < cargo().getContainerSize(); i++) {
            weight += cargo().getItem(i).isEmpty() ? 0.0D : 0.1D;
        }
        return weight;
    }

    /** 1.12.2 onUpdate: min(t / 250, 1) x 5 / weight blocks a tick. */
    @Override
    protected Vec3 climbMotion() {
        return new Vec3(0.0D, Math.min(timeSinceLaunch() / 250.0D, 1.0D) * 5.0D / weight(), 0.0D);
    }

    // ------------------------------------------------------------------ landing (1.12.2 EntityAutoRocket#onUpdate, LANDING)

    /** Down at (height above the pad - 0.04) / 55 a tick, at most 2; drifting up to 0.1 a tick towards the pad, tilted. */
    @Override
    protected Vec3 landingMotion() {
        if (this.target == null) {
            return new Vec3(0.0D, -1.0D, 0.0D);
        }
        final double yDiff = getY() - PAD_OFFSET - this.target.getY();
        final double motY = Math.max(-2.0D, (yDiff - 0.04D) / -55.0D);
        final double motX = Mth.clamp((getX() - this.target.getX() - 0.5D) / -100.0D, -0.1D, 0.1D);
        final double motZ = Mth.clamp((getZ() - this.target.getZ() - 0.5D) / -100.0D, -0.1D, 0.1D);
        if (motX != 0.0D || motZ != 0.0D) {
            final double signed = motX < 0 ? 50.0D : -50.0D;
            setYRot((float) Math.toDegrees(Math.atan(motZ / (motX == 0.0D ? 1.0E-9D : motX))));
            setXRot((float) Math.toDegrees(Math.atan(Math.sqrt(motX * motX + motZ * motZ) / signed) * 100.0D));
        } else {
            setXRot(0.0F);
        }
        if (yDiff > 1.0D && yDiff < 4.0D) {
            // 1.12.2: a rocket still sitting on the target pad is knocked off as an item
            for (RocketEntity other : level().getEntitiesOfClass(RocketEntity.class, getBoundingBox().move(0.0D, -3.0D, 0.0D), r -> r != this)) {
                other.dropAsItem();
                other.discard();
            }
        }
        return new Vec3(motX, motY, motZ);
    }

    /** 1.12.2 failRocket while landing: within 0.4 of the pad, it docks. */
    @Override
    protected boolean tryLand() {
        if (this.target == null || getY() - PAD_OFFSET - this.target.getY() >= 0.4D) {
            return false;
        }
        final BlockPos centre = LandingPadBlock.findLaunchPadCentre(level(), this.target);
        if (centre == null) {
            return false;
        }
        setPos(centre.getX() + 0.5D, centre.getY() + 0.4D + PAD_OFFSET, centre.getZ() + 0.5D);
        setXRot(0.0F);
        setDeltaMovement(Vec3.ZERO);
        resetFlight();
        this.target = null;
        this.targetDimension = null;
        this.destinationFrequency = -1;
        final LaunchControllerBlockEntity controller = LaunchControllerBlockEntity.attachedTo(level(), centre);
        if (controller != null) {
            updateControllerSettings(controller);
        }
        return true;
    }

    // ------------------------------------------------------------------ reaching space (1.12.2 onReachAtmosphere)

    @Override
    protected void reachSpace() {
        if (!(level() instanceof ServerLevel serverLevel) || !findTarget(this.destinationFrequency, true) || this.target == null
                || this.targetDimension == null) {
            discard(); // 1.12.2: no valid landing spot, the rocket is lost
            return;
        }
        final ServerLevel destination = serverLevel.getServer().getLevel(this.targetDimension);
        if (destination == null) {
            discard();
            return;
        }
        final Vec3 arrival = new Vec3(this.target.getX() + 0.5D, this.target.getY() + ARRIVAL_HEIGHT, this.target.getZ() + 0.5D);
        setPhase(Phase.LANDING);
        setXRot(0.0F);
        keepLoaded(destination, BlockPos.containing(arrival));
        if (destination == serverLevel) {
            setPos(arrival);
            return;
        }
        changeDimension(new DimensionTransition(destination, arrival, Vec3.ZERO, getYRot(), 0.0F, DimensionTransition.DO_NOTHING));
    }

    private static void keepLoaded(ServerLevel level, BlockPos pos) {
        final ChunkPos chunk = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(FLIGHT_TICKET, chunk, 2, chunk);
    }

    // ------------------------------------------------------------------ launch control (1.12.2 EntityAutoRocket)

    @Override
    public void tick() {
        super.tick();
        if (isRemoved() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.ticks++;
        if (launched() && this.ticks % 20 == 0) {
            keepLoaded(serverLevel, blockPosition());
        }
        if (this.statusCooldown > 0) {
            this.statusCooldown--;
        }
        if (this.statusCooldown == 0 && this.lastStatusCooldown > 0 && this.statusValid) {
            autoLaunch();
        }
        if (this.autoLaunchCountdown > 0 && --this.autoLaunchCountdown <= 0) {
            autoLaunch();
        }
        if (this.autoLaunchSetting == AutoLaunch.ROCKET_IS_FUELED && fuel() >= tankCapacity()) {
            autoLaunch();
        }
        if (this.autoLaunchSetting == AutoLaunch.INSTANT && this.autoLaunchCountdown == 0) {
            autoLaunch();
        }
        if (this.autoLaunchSetting == AutoLaunch.REDSTONE_SIGNAL && this.ticks % 11 == 0 && this.activeLaunchController != null
                && level().hasNeighborSignal(this.activeLaunchController)) {
            autoLaunch();
        }
        if (phase() == Phase.UNIGNITED && this.ticks % 17 == 0) {
            final BlockPos pad = LandingPadBlock.findLaunchPadCentre(level(), BlockPos.containing(getX(), getY() - 0.1D, getZ()));
            final LaunchControllerBlockEntity controller = pad == null ? null : LaunchControllerBlockEntity.attachedTo(level(), pad);
            if (controller != null) {
                updateControllerSettings(controller);
            } else {
                this.activeLaunchController = null;
                this.autoLaunchSetting = null;
                this.autoLaunchCountdown = 0;
            }
        }
        this.lastStatusCooldown = this.statusCooldown;
    }

    /** 1.12.2 updateControllerSettings: the controller beside our pad sets when we launch. */
    public void updateControllerSettings(LaunchControllerBlockEntity controller) {
        this.activeLaunchController = controller.getBlockPos();
        if (!controller.controlEnabled()) {
            // out of power, disabled or not set to launch
            this.autoLaunchSetting = null;
            this.autoLaunchCountdown = 0;
            return;
        }
        this.autoLaunchSetting = controller.launchSelection();
        final int countdown = switch (this.autoLaunchSetting) {
            case INSTANT -> 12; // a moment to leave the controller's GUI
            case TIME_10_SECONDS -> 200;
            case TIME_30_SECONDS -> 600;
            case TIME_1_MINUTE -> 1200;
            default -> 0;
        };
        if (countdown > 0 && (this.autoLaunchCountdown <= 0 || this.autoLaunchCountdown > countdown)) {
            this.autoLaunchCountdown = countdown;
        }
    }

    @Nullable
    private LaunchControllerBlockEntity activeController() {
        return this.activeLaunchController != null && level().getBlockEntity(this.activeLaunchController) instanceof LaunchControllerBlockEntity controller
                ? controller : null;
    }

    /** 1.12.2 autoLaunch: a controller's rule fired (needs 40% fuel), or the Launch button's check passed. */
    private void autoLaunch() {
        if (this.autoLaunchSetting != null) {
            final LaunchControllerBlockEntity controller = activeController();
            if (controller != null && controller.controlEnabled() && fuel() > tankCapacity() * 2 / 5) {
                ignite();
            }
            this.autoLaunchSetting = null;
            return;
        }
        ignite();
    }

    /** 1.12.2 igniteWithResult: only with somewhere to go. */
    @Override
    public void ignite() {
        if (setFrequency()) {
            super.ignite();
        }
        this.activeLaunchController = null;
    }

    /** 1.12.2 setFrequency: our controller is valid and its target has a pad. */
    private boolean setFrequency() {
        final LaunchControllerBlockEntity controller = activeController();
        if (controller != null && controller.validFrequency() && findTarget(controller.destFrequency(), false)) {
            this.destinationFrequency = controller.destFrequency();
            return true;
        }
        this.destinationFrequency = -1;
        return false;
    }

    /** 1.12.2 setTarget: the pad within 2 blocks of the controller with this frequency (on any world). */
    private boolean findTarget(int frequency, boolean doSet) {
        final LaunchControllerBlockEntity controller = LaunchControllerBlockEntity.withFrequency(frequency);
        final BlockPos pad = controller == null ? null : controller.landingPad();
        if (doSet) {
            this.target = pad;
            this.targetDimension = controller == null || controller.getLevel() == null ? null : controller.getLevel().dimension();
        }
        return pad != null;
    }

    /** 1.12.2 checkLaunchValidity, from the Launch button: shows a status for 40 ticks, then launches if it passed. */
    public void pressLaunch() {
        this.statusCooldown = 40;
        this.statusValid = false;
        if (!hasFuel()) {
            this.destinationFrequency = -1;
            this.statusCode = STATUS_NOT_ENOUGH_FUEL;
            return;
        }
        if (phase() != Phase.UNIGNITED) {
            this.destinationFrequency = -1;
            return;
        }
        this.statusValid = setFrequency();
        this.statusCode = this.statusValid ? STATUS_SUCCESS : STATUS_FREQUENCY_NOT_SET;
    }

    public int statusCode() {
        return this.statusCode;
    }

    public int statusCooldown() {
        return this.statusCooldown;
    }

    public boolean statusValid() {
        return this.statusValid;
    }

    @Nullable
    public BlockPos target() {
        return this.target;
    }

    @Nullable
    public AutoLaunch autoLaunchSetting() {
        return this.autoLaunchSetting;
    }

    // ------------------------------------------------------------------ cargo

    /** 1.12.2 addCargo: a full rocket set to launch when full goes. */
    @Override
    public LoadState addCargo(ItemStack stack, boolean doAdd) {
        final LoadState state = super.addCargo(stack, doAdd);
        if (state != LoadState.SUCCESS && this.autoLaunchSetting == AutoLaunch.CARGO_IS_FULL) {
            autoLaunch();
        }
        return state;
    }

    /** 1.12.2 removeCargo: an emptied rocket set to launch when unloaded goes. */
    @Override
    public Removal removeCargo(boolean doRemove) {
        final Removal removal = super.removeCargo(doRemove);
        if (removal.stack().isEmpty() && this.autoLaunchSetting == AutoLaunch.CARGO_IS_UNLOADED) {
            autoLaunch();
        }
        return removal;
    }

    /** 1.12.2 processInitialInteract: its cargo and Launch button, from the ground. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CargoMenu.openCargoRocket(serverPlayer, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.destinationFrequency = tag.contains("DestinationFrequency") ? tag.getInt("DestinationFrequency") : -1;
        this.target = NbtUtils.readBlockPos(tag, "Target").orElse(null);
        this.targetDimension = tag.contains("TargetDimension")
                ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("TargetDimension"))) : null;
        this.autoLaunchSetting = tag.contains("AutoLaunch") ? AutoLaunch.values()[Mth.clamp(tag.getInt("AutoLaunch"), 0, AutoLaunch.values().length - 1)] : null;
        this.autoLaunchCountdown = tag.getInt("AutoLaunchCountdown");
        this.activeLaunchController = NbtUtils.readBlockPos(tag, "LaunchController").orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DestinationFrequency", this.destinationFrequency);
        if (this.target != null) {
            tag.put("Target", NbtUtils.writeBlockPos(this.target));
        }
        if (this.targetDimension != null) {
            tag.putString("TargetDimension", this.targetDimension.location().toString());
        }
        if (this.autoLaunchSetting != null) {
            tag.putInt("AutoLaunch", this.autoLaunchSetting.ordinal());
        }
        tag.putInt("AutoLaunchCountdown", this.autoLaunchCountdown);
        if (this.activeLaunchController != null) {
            tag.put("LaunchController", NbtUtils.writeBlockPos(this.activeLaunchController));
        }
    }
}
