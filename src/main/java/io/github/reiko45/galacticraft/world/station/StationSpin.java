/*
 * Spin physics derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.dimension.SpinManager and
 * micdoodle8.mods.galacticraft.core.entities.player.FreefallHandler#testEntityFreefall/#tickFreefallEntity).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.station;

import io.github.reiko45.galacticraft.block.SpinThrusterBlock;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * How one space station spins (1.12.2 SpinManager, which ran one per station dimension). Spin Thrusters on the
 * station push it round: the station's blocks are found by flood fill from its arrival block, each weighing its
 * hardness (0.1 to 30, wood a quarter), giving the centre of mass and the moment of inertia; thrusters one way minus
 * thrusters the other give the torque. With unequal thrusters the station spins up towards sqrt(g / r) / 2 radians per
 * tick (r its largest radius), capped at 0.00125 per thruster up to 4; balanced thrusters slow it to a stop. The blocks
 * never move: the sky turns, and anything floating free (not touching the station) is carried round the centre.
 */
public final class StationSpin {
    /** 1.12.2 GFORCE: 9.81 / 400. */
    private static final float GFORCE = 9.81F / 400.0F;
    /** Flood fill limit, so a huge station can't stall the server. */
    private static final int MAX_BLOCKS = 100_000;

    public float angularVelocity;
    private float angularVelocityTarget;
    private float angularVelocityAccel;
    public boolean thrustersFiring;
    public double centreX;
    public double centreZ;
    private float momentOfInertia;
    public int minX;
    public int maxX;
    public int minY;
    public int maxY;
    public int minZ;
    public int maxZ;
    private final List<BlockPos> thrustersPlus = new ArrayList<>();
    private final List<BlockPos> thrustersMinus = new ArrayList<>();

    StationSpin(BlockPos base) {
        this.centreX = base.getX() + 0.5D;
        this.centreZ = base.getZ() + 0.5D;
        this.minX = base.getX() - 10;
        this.maxX = base.getX() + 4;
        this.minY = base.getY() - 2;
        this.maxY = base.getY() + 5;
        this.minZ = base.getZ() - 3;
        this.maxZ = base.getZ() + 4;
    }

    /** The spin a station built at {@code base} would have (used by the GameTests). */
    public static StationSpin measure(ServerLevel level, BlockPos base) {
        final StationSpin spin = new StationSpin(base);
        spin.refresh(level, base);
        return spin;
    }

    public float targetVelocity() {
        return this.angularVelocityTarget;
    }

    public int thrusterCount() {
        return this.thrustersPlus.size() + this.thrustersMinus.size();
    }

    public float skyDegreesPerTick() {
        return this.angularVelocity * Mth.RAD_TO_DEG;
    }

    // ------------------------------------------------------------------ shape and speed

    /** A thruster was placed, broken, wrenched or switched at {@code pos}: recompute the station it belongs to. */
    public static void refreshAt(ServerLevel level, BlockPos pos, boolean placingThruster) {
        if (!level.dimension().equals(GCDimensions.ORBIT)) {
            return;
        }
        final SpaceStations stations = SpaceStations.get(level.getServer());
        final SpaceStations.Station station = stations.at(pos);
        if (station == null) {
            return;
        }
        station.spin().refresh(level, station.base());
        stations.setDirty();
        sync(level, station);
    }

    /** 1.12.2 SpinManager#refresh: flood fill the station, find its mass centre, moment of inertia and thrusters. */
    void refresh(ServerLevel level, BlockPos base) {
        final Set<BlockPos> checked = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        final List<BlockPos> foundThrusters = new ArrayList<>();
        checked.add(base);
        queue.add(base);
        int bMinX = base.getX();
        int bMaxX = bMinX;
        int bMinY = base.getY();
        int bMaxY = bMinY;
        int bMinZ = base.getZ();
        int bMaxZ = bMinZ;
        // Sums relative to the arrival block, in doubles: stations sit thousands of blocks out, where 1.12.2's float
        // sums of absolute x^2 lose all precision.
        double mass = 0.1D;
        double sumX = 0.0D;
        double sumZ = 0.0D;
        double moment = 0.0D;
        while (!queue.isEmpty() && checked.size() < MAX_BLOCKS) {
            final BlockPos pos = queue.poll();
            bMinX = Math.min(bMinX, pos.getX());
            bMaxX = Math.max(bMaxX, pos.getX());
            bMinY = Math.min(bMinY, pos.getY());
            bMaxY = Math.max(bMaxY, pos.getY());
            bMinZ = Math.min(bMinZ, pos.getZ());
            bMaxZ = Math.max(bMaxZ, pos.getZ());
            for (Direction side : Direction.values()) {
                final BlockPos next = pos.relative(side);
                if (!checked.add(next) || !level.isLoaded(next)) {
                    continue;
                }
                final BlockState state = level.getBlockState(next);
                if (state.isAir() || !state.getFluidState().isEmpty() && !state.getFluidState().isSource()) {
                    continue;
                }
                queue.add(next);
                double m = 1.0D;
                if (state.getFluidState().isEmpty()) {
                    m = Mth.clamp(state.getDestroySpeed(level, next), 0.1F, 30.0F);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_STAIRS)) {
                        m /= 4.0D;
                    }
                }
                final double dx = next.getX() - base.getX();
                final double dz = next.getZ() - base.getZ();
                sumX += m * dx;
                sumZ += m * dz;
                mass += m;
                moment += m * (dx * dx + dz * dz);
                if (state.getBlock() instanceof SpinThrusterBlock && !level.hasNeighborSignal(next)) {
                    foundThrusters.add(next);
                }
            }
        }
        this.thrustersPlus.clear();
        this.thrustersMinus.clear();
        for (BlockPos thruster : foundThrusters) {
            (level.getBlockState(thruster).getValue(SpinThrusterBlock.REVERSED) ? this.thrustersMinus : this.thrustersPlus).add(thruster);
        }
        final double relCentreX = sumX / mass;
        final double relCentreZ = sumZ / mass;
        this.centreX = base.getX() + relCentreX + 0.5D;
        this.centreZ = base.getZ() + relCentreZ + 0.5D;
        this.minX = bMinX;
        this.maxX = bMaxX + 1;
        this.minY = bMinY;
        this.maxY = bMaxY + 1;
        this.minZ = bMinZ;
        this.maxZ = bMaxZ + 1;
        // About the centre of mass (parallel axis theorem, as 1.12.2 did)
        this.momentOfInertia = (float) (moment - (relCentreX * relCentreX + relCentreZ * relCentreZ) * mass);
        updateSpinSpeed();
    }

    /** 1.12.2 SpinManager#updateSpinSpeed. */
    private void updateSpinSpeed() {
        if (this.momentOfInertia <= 0.0F) {
            return;
        }
        float torque = 0.0F;
        for (BlockPos thruster : this.thrustersPlus) {
            torque += (float) Math.sqrt(Mth.square(thruster.getX() - this.centreX) + Mth.square(thruster.getZ() - this.centreZ));
        }
        for (BlockPos thruster : this.thrustersMinus) {
            torque -= (float) Math.sqrt(Mth.square(thruster.getX() - this.centreX) + Mth.square(thruster.getZ() - this.centreZ));
        }
        if (this.thrustersPlus.size() == this.thrustersMinus.size()) {
            this.angularVelocityAccel = 0.000004F;
            this.angularVelocityTarget = 0.0F;
            return;
        }
        final int count = Math.min(4, this.thrustersPlus.size() + this.thrustersMinus.size());
        final float maxR = (float) Math.max(Math.max(this.maxX - this.centreX, this.centreX - this.minX),
                Math.max(this.maxZ - this.centreZ, this.centreZ - this.minZ));
        float target = Mth.sqrt(GFORCE / maxR) / 2.0F;
        final float cap = 0.00125F * count;
        this.angularVelocityAccel = torque / this.momentOfInertia / 20.0F;
        if (this.angularVelocityAccel < 0.0F) {
            this.angularVelocityAccel = -this.angularVelocityAccel;
            target = Math.max(-target, -cap);
        } else {
            target = Math.min(target, cap);
        }
        this.angularVelocityTarget = target;
    }

    /** Spin up or down by one tick's acceleration; returns whether anything changed. */
    public boolean spinStep() {
        if (this.angularVelocityTarget < this.angularVelocity) {
            this.angularVelocity = Math.max(this.angularVelocityTarget, this.angularVelocity - this.angularVelocityAccel);
            this.thrustersFiring = true;
        } else if (this.angularVelocityTarget > this.angularVelocity) {
            this.angularVelocity = Math.min(this.angularVelocityTarget, this.angularVelocity + this.angularVelocityAccel);
            this.thrustersFiring = true;
        } else if (this.thrustersFiring) {
            this.thrustersFiring = false;
        } else {
            return false;
        }
        return true;
    }

    /** One server tick (1.12.2 SpinManager#updateSpin): spin up or down, and carry free-floating things round. */
    void tick(ServerLevel level, SpaceStations.Station station) {
        if (spinStep()) {
            SpaceStations.get(level.getServer()).setDirty();
            sync(level, station);
        }
        final AABB region = new AABB(station.base()).inflate(SpaceStations.SPACING / 2.0D - 1.0D, 0.0D, SpaceStations.SPACING / 2.0D - 1.0D)
                .setMinY(level.getMinBuildHeight()).setMaxY(level.getMaxBuildHeight());
        for (Entity entity : level.getEntities((Entity) null, region, StationSpin::driftsFree)) {
            final AABB box = entity.getBoundingBox();
            final boolean outside = box.maxX < this.minX || box.minX > this.maxX || box.maxY < this.minY || box.minY > this.maxY
                    || box.maxZ < this.minZ || box.minZ > this.maxZ;
            if (outside || inFreefall(level, box)) {
                if (this.angularVelocity != 0.0F) {
                    moveRotated(entity);
                }
                keepMomentum(entity);
            }
        }
    }

    /** 1.12.2: dropped items, mobs, lit TNT and falling blocks drift; players steer themselves. */
    private static boolean driftsFree(Entity entity) {
        return (entity instanceof ItemEntity || entity instanceof LivingEntity && !(entity instanceof Player) || entity instanceof PrimedTnt
                || entity instanceof FallingBlockEntity) && !entity.onGround() && !entity.isPassenger();
    }

    /** 1.12.2 FreefallHandler#testEntityFreefall: nothing but air in or right around the box. */
    public static boolean inFreefall(BlockGetter level, AABB box) {
        final int x0 = Mth.floor(box.minX - 0.2D);
        final int x1 = Mth.floor(box.maxX + 0.2D);
        final int y0 = Math.max(level.getMinBuildHeight(), Mth.floor(box.minY - 0.1D));
        final int y1 = Mth.floor(box.maxY + 0.1D);
        final int z0 = Mth.floor(box.minZ - 0.2D);
        final int z1 = Mth.floor(box.maxZ + 0.2D);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 1.12.2 SpinManager#moveRotatedEntity: round the centre by the tick's angle, turning with it. */
    private void moveRotated(Entity entity) {
        final double xx = entity.getX() - this.centreX;
        final double zz = entity.getZ() - this.centreZ;
        float angle = (float) Math.atan2(zz, xx);
        angle += this.angularVelocity / 3.0F;
        final double arc = Math.sqrt(xx * xx + zz * zz) * this.angularVelocity;
        final double offsetX = -arc * Mth.sin(angle);
        final double offsetZ = arc * Mth.cos(angle);
        if (!entity.level().isLoaded(BlockPos.containing(entity.getX() + offsetX, 64, entity.getZ() + offsetZ))) {
            entity.discard(); // rotated into an unloaded chunk: gone, as in 1.12.2
            return;
        }
        entity.setPos(entity.getX() + offsetX, entity.getY(), entity.getZ() + offsetZ);
        entity.xo += offsetX;
        entity.zo += offsetZ;
        entity.setYRot(entity.getYRot() + skyDegreesPerTick());
        final Vec3 motion = entity.getDeltaMovement();
        final double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        final double direction = Math.atan2(motion.z, motion.x) + this.angularVelocity;
        entity.setDeltaMovement(speed * Math.cos(direction), motion.y, speed * Math.sin(direction));
    }

    /** 1.12.2 FreefallHandler#tickFreefallEntity: undo the air drag, so floating things keep moving (up to 10 per tick). */
    private static void keepMomentum(Entity entity) {
        final Vec3 m = entity.getDeltaMovement();
        final Vec3 kept = entity instanceof LivingEntity ? new Vec3(m.x / 0.91D, m.y / 0.98D, m.z / 0.91D)
                : entity instanceof FallingBlockEntity ? new Vec3(m.x, m.y / 0.98D, m.z) : m.scale(1.0D / 0.98D);
        entity.setDeltaMovement(Mth.clamp(kept.x, -10.0D, 10.0D), Mth.clamp(kept.y, -10.0D, 10.0D), Mth.clamp(kept.z, -10.0D, 10.0D));
    }

    // ------------------------------------------------------------------ saving and sync

    CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putFloat("Omega", this.angularVelocity);
        tag.putFloat("OmegaTarget", this.angularVelocityTarget);
        tag.putFloat("OmegaAccel", this.angularVelocityAccel);
        tag.putDouble("CentreX", this.centreX);
        tag.putDouble("CentreZ", this.centreZ);
        tag.putFloat("Moment", this.momentOfInertia);
        tag.putIntArray("Bounds", new int[]{this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ});
        tag.put("ThrustersPlus", new LongArrayTag(this.thrustersPlus.stream().mapToLong(BlockPos::asLong).toArray()));
        tag.put("ThrustersMinus", new LongArrayTag(this.thrustersMinus.stream().mapToLong(BlockPos::asLong).toArray()));
        return tag;
    }

    void load(CompoundTag tag) {
        this.angularVelocity = tag.getFloat("Omega");
        this.angularVelocityTarget = tag.getFloat("OmegaTarget");
        this.angularVelocityAccel = tag.getFloat("OmegaAccel");
        this.centreX = tag.getDouble("CentreX");
        this.centreZ = tag.getDouble("CentreZ");
        this.momentOfInertia = tag.getFloat("Moment");
        final int[] bounds = tag.getIntArray("Bounds");
        if (bounds.length == 6) {
            this.minX = bounds[0];
            this.maxX = bounds[1];
            this.minY = bounds[2];
            this.maxY = bounds[3];
            this.minZ = bounds[4];
            this.maxZ = bounds[5];
        }
        this.thrustersPlus.clear();
        for (long pos : tag.getLongArray("ThrustersPlus")) {
            this.thrustersPlus.add(BlockPos.of(pos));
        }
        this.thrustersMinus.clear();
        for (long pos : tag.getLongArray("ThrustersMinus")) {
            this.thrustersMinus.add(BlockPos.of(pos));
        }
    }

    GCNetwork.StationSpinSync packet(int id) {
        return new GCNetwork.StationSpinSync(id, this.angularVelocity, this.thrustersFiring, this.centreX, this.centreZ,
                new int[]{this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ});
    }

    /** 1.12.2 C_UPDATE_STATION_SPIN/DATA/BOX: tell everyone in orbit. */
    static void sync(ServerLevel level, SpaceStations.Station station) {
        final GCNetwork.StationSpinSync packet = station.spin().packet(station.id());
        for (ServerPlayer player : level.players()) {
            GCNetwork.sendToPlayer(player, packet);
        }
    }

    /** A player arrived in orbit: send every station's spin (1.12.2 SpinManager#sendPackets). */
    public static void syncAll(ServerPlayer player) {
        for (SpaceStations.Station station : SpaceStations.get(player.server).all()) {
            GCNetwork.sendToPlayer(player, station.spin().packet(station.id()));
        }
    }

    // ------------------------------------------------------------------ client side

    /** What the client knows of each station's spin. */
    public static final class Client {
        private static final Map<Integer, GCNetwork.StationSpinSync> STATIONS = new HashMap<>();

        private Client() {
        }

        public static void update(GCNetwork.StationSpinSync packet) {
            STATIONS.put(packet.station(), packet);
        }

        public static void reset() {
            STATIONS.clear();
        }

        /** The spin of the station whose region {@code pos} is in, if any. */
        @Nullable
        public static GCNetwork.StationSpinSync at(BlockPos pos) {
            return STATIONS.get(Math.floorDiv(pos.getX() + SpaceStations.SPACING / 2, SpaceStations.SPACING));
        }

        /** Whether the station around {@code pos} is spinning up or down (thruster smoke). */
        public static boolean firingNear(BlockPos pos) {
            final GCNetwork.StationSpinSync spin = at(pos);
            return spin != null && spin.firing();
        }

        public static boolean inOrbit(Level level) {
            return level.dimension().equals(GCDimensions.ORBIT);
        }
    }
}
