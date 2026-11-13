/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityAstroMiner and
 * tick.AsteroidsTickHandlerServer). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.WalkwayBlock;
import io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.DeadBushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.jetbrains.annotations.Nullable;

/**
 * Astro Miner (1.12.2 EntityAstroMiner): a mining drone that lives in a 2 x 2 x 2 Astro Miner Base. Charged to 12,000
 * at its base, it flies out along block lines to a mining target (the next nearby asteroid, or a set of points ahead of
 * the base elsewhere), tunnels a 14-shaft pattern there, and comes back through its own tunnels to empty its 227-slot
 * hold into the base (as each mined block's own item), charge up and go again. It turns back early when low on energy
 * or after dropping ten stacks it couldn't hold, and gets stuck on things it may not mine (bedrock, stone bricks,
 * portals, rails, walkways, block entities, crops, lava sources, protected areas). It only works while its owner is
 * online, and keeps its chunk loaded while it does.
 */
public class AstroMinerEntity extends Entity implements Telemetry {
    public static final int MINE_LENGTH = 24;
    public static final int MINE_LENGTH_AST = 12;
    public static final int MAX_ENERGY = 12000;
    private static final int RETURN_ENERGY = 1000;
    private static final int RETURN_DROPS = 10;
    public static final int INV_SIZE = 227;
    private static final float C_LENGTH = 2.6F;
    private static final float C_WIDTH = 1.8F;
    private static final float C_HEIGHT = 1.7F;
    private static final double SPEEDUP = 2.5D;

    public static final int AISTATE_OFFLINE = -1;
    public static final int AISTATE_STUCK = 0;
    public static final int AISTATE_ATBASE = 1;
    public static final int AISTATE_TRAVELLING = 2;
    public static final int AISTATE_MINING = 3;
    public static final int AISTATE_RETURNING = 4;
    public static final int AISTATE_DOCKING = 5;

    public static final int FAIL_BASEDESTROYED = 3;
    public static final int FAIL_OUTOFENERGY = 4;
    public static final int FAIL_RETURNPATHBLOCKED = 5;
    public static final int FAIL_ANOTHERWASLINKED = 8;

    /** Keeps an active miner's chunk loaded while its owner is online (1.12.2 kept it from unloading). */
    public static final TicketController TICKETS = new TicketController(GalacticraftCore.id("astro_miner"));

    private static final EntityDataAccessor<Integer> AI_STATE = SynchedEntityData.defineId(AstroMinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(AstroMinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MINE_COUNT = SynchedEntityData.defineId(AstroMinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(AstroMinerEntity.class, EntityDataSerializers.FLOAT);

    /** 1.12.2 headings / headings2: indexed by Direction (down, up, north, south, west, east). */
    private static final int[][] HEADINGS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    private static final int[][] HEADINGS_2 = {{0, -3, 0}, {0, 2, 0}, {0, 0, -3}, {0, 0, 2}, {-3, 0, 0}, {2, 0, 0}};
    /** 1.12.2 prepareMove: the 12 blocks cleared in front, for moving down/up, north/south and west/east. */
    private static final int[][][] MINE_PATTERN = {
            {{0, 0, 0}, {1, 0, 0}, {1, 0, -1}, {0, 0, -1}, {0, 0, -2}, {-1, 0, -2}, {-1, 0, -1}, {-2, 0, -1}, {-2, 0, 0}, {-1, 0, 0}, {-1, 0, 1}, {0, 0, 1}},
            {{0, -2, 0}, {-1, -2, 0}, {0, -1, 0}, {-1, -1, 0}, {1, -1, 0}, {-2, -1, 0}, {1, 0, 0}, {-2, 0, 0}, {0, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {-1, 1, 0}},
            {{0, -2, -1}, {0, -1, 0}, {0, -1, -1}, {0, -1, 1}, {0, -1, -2}, {0, 0, 1}, {0, 0, -2}, {0, 0, -1}, {0, -2, 0}, {0, 1, -1}, {0, 0, 0}, {0, 1, 0}}};

    public NonNullList<ItemStack> stacks = NonNullList.withSize(INV_SIZE, ItemStack.EMPTY);
    public int energyLevel;
    public int mineCount;
    public float targetYaw;
    public float targetPitch;
    public int aiState = AISTATE_ATBASE;
    @Nullable
    private UUID playerUUID;
    @Nullable
    private ServerPlayer player;
    @Nullable
    private BlockPos posTarget;
    @Nullable
    private BlockPos posBase;
    @Nullable
    private BlockPos waypointBase;
    private final LinkedList<BlockPos> wayPoints = new LinkedList<>();
    private final LinkedList<BlockPos> minePoints = new LinkedList<>();
    @Nullable
    private BlockPos minePointCurrent;
    private Direction baseFacing = Direction.NORTH;
    public Direction facing = Direction.NORTH;
    private Direction facingAI = Direction.NORTH;
    @Nullable
    private Direction lastFacing;
    private Vec3 motion = Vec3.ZERO;
    private static final double SPEED_BASE = 0.022D;
    private double speed = SPEED_BASE;
    private static final float ROT_SPEED_BASE = 1.5F;
    private float rotSpeed = ROT_SPEED_BASE;
    private double speedup = SPEEDUP;
    private boolean noSpeedup;
    private float shipDamage;
    private boolean flagLink;
    private boolean flagCheckPlayer;
    private int tryBlockLimit;
    private int inventoryDrops;
    private boolean stopForTurn;
    /** 1.12.2 blockingBlock: what stopped the last move (stone stands for a protected area). */
    private Block blockingBlock = Blocks.AIR;
    private int givenFailMessage;
    @Nullable
    private BlockPos mineLast;
    private int mineCountDown;
    private int pathBlockedCount;
    private boolean spawnedInCreative;
    @Nullable
    private net.minecraft.world.level.ChunkPos forcedChunk;

    /** Client: blocks being cut, for the laser effect (1.12.2 laserBlocks / laserTimes). */
    public final LinkedList<BlockPos> laserBlocks = new LinkedList<>();
    public final LinkedList<Integer> laserTimes = new LinkedList<>();
    /** Client: how far the lasers are folded away, 0 (out) to 1 (stowed). */
    public float retraction = 1.0F;

    public AstroMinerEntity(EntityType<? extends AstroMinerEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AI_STATE, AISTATE_ATBASE);
        builder.define(ENERGY, 0);
        builder.define(MINE_COUNT, 0);
        builder.define(DAMAGE, 0.0F);
    }

    /** What clients and the dock see: offline while the owner is away (1.12.2 getNetworkedData). */
    public int syncedState() {
        return this.entityData.get(AI_STATE);
    }

    public int syncedEnergy() {
        return this.entityData.get(ENERGY);
    }

    public int syncedMineCount() {
        return this.entityData.get(MINE_COUNT);
    }

    @Nullable
    public BlockPos base() {
        return this.posBase;
    }

    @Nullable
    public UUID ownerId() {
        return this.playerUUID;
    }

    private boolean onAsteroids() {
        return level().dimension() == GCDimensions.ASTEROIDS;
    }

    // ------------------------------------------------------------------ spawning (1.12.2 spawnMinerAtBase)

    /**
     * A new miner docked at the base whose 2 x 2 x 2 block starts at {@code base}, facing out of {@code facing}. It first
     * clears the docking space; false (and nothing spawned) if something there can't be mined.
     */
    public static boolean spawnAtBase(ServerLevel level, BlockPos base, Direction facing, ServerPlayer owner) {
        final AstroMinerEntity miner = new AstroMinerEntity(GCEntities.ASTRO_MINER.get(), level);
        miner.setOwner(owner);
        miner.spawnedInCreative = owner.getAbilities().instabuild;
        miner.waypointBase = base.offset(1, 1, 1).relative(facing);
        miner.baseFacing = facing;
        miner.facingAI = facing;
        miner.lastFacing = facing;
        miner.targetPitch = 0.0F;
        miner.targetYaw = yawFor(facing);
        miner.setXRot(miner.targetPitch);
        miner.setYRot(miner.targetYaw);
        miner.facing = miner.facingFromRotation();
        miner.setPos(miner.waypointBase.getX(), miner.waypointBase.getY() - 1, miner.waypointBase.getZ());
        miner.aiState = AISTATE_ATBASE;
        miner.posBase = base.immutable();
        miner.speedup = miner.onAsteroids() ? SPEEDUP * 2.2D : SPEEDUP;
        if (miner.prepareMove(12, 0) || miner.prepareMove(12, 1) || miner.prepareMove(12, 2)) {
            sharedBlocker = miner.blockingBlock;
            return false;
        }
        level.addFreshEntity(miner);
        miner.flagLink = true;
        return true;
    }

    /** What stopped the last failed spawn, for the message (1.12.2's static blockingBlock). */
    public static Block sharedBlocker = Blocks.AIR;

    private static float yawFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    public void setOwner(ServerPlayer owner) {
        this.player = owner;
        this.playerUUID = owner.getUUID();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        if (getY() < level().getMinBuildHeight() - 64.0D) {
            discard();
            return;
        }
        if (damage() > 0.0F) {
            setDamage(damage() - 1.0F);
        }
        this.stopForTurn = !checkRotation();
        this.facing = facingFromRotation();
        setBoundingBox(makeBoundingBox());
        if (level().isClientSide()) {
            super.tick();
            if (syncedState() == AISTATE_MINING && this.tickCount % 2 == 0) {
                prepareMoveClient(1, 2);
            }
            return;
        }
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.xRotO = getXRot();
        this.yRotO = getYRot();
        this.tickCount++;
        if (this.tickCount % 10 == 0 || this.flagLink) {
            this.flagLink = false;
            checkPlayer();
            checkBase();
        } else if (this.flagCheckPlayer) {
            checkPlayer();
        }
        if (this.player == null) {
            this.motion = Vec3.ZERO;
            releaseChunk();
            sync();
            return;
        }
        keepChunkLoaded();
        if (this.lastFacing != this.facingAI) {
            this.lastFacing = this.facingAI;
            prepareMove(12, 0);
            prepareMove(12, 1);
            prepareMove(12, 2);
        }
        if (this.aiState > AISTATE_ATBASE) {
            if (this.energyLevel <= 0) {
                freeze(FAIL_OUTOFENERGY);
            } else if (!onAsteroids() && this.tickCount % 2 == 0) {
                this.energyLevel--;
            }
        }
        switch (this.aiState) {
            case AISTATE_STUCK -> {
                if (this.tickCount % 600 == 0) {
                    if ((this.givenFailMessage & 8) > 0) {
                        atBase();
                    } else {
                        this.aiState = AISTATE_RETURNING;
                        if (this.energyLevel <= 0) {
                            this.energyLevel = 20;
                        }
                    }
                }
            }
            case AISTATE_ATBASE -> atBase();
            case AISTATE_TRAVELLING -> {
                if (!moveToTarget()) {
                    prepareMove(2, 2);
                }
            }
            case AISTATE_MINING -> {
                if (!doMining() && this.tickCount % 2 == 0) {
                    this.energyLevel--;
                    prepareMove(1, 2);
                }
            }
            case AISTATE_RETURNING -> {
                moveToBase();
                prepareMove(4, 1);
            }
            case AISTATE_DOCKING -> {
                if (this.waypointBase != null) {
                    this.speed = SPEED_BASE / 1.6D;
                    this.rotSpeed = ROT_SPEED_BASE / 1.6F;
                    if (moveToPos(this.waypointBase, true)) {
                        this.aiState = AISTATE_ATBASE;
                        this.motion = Vec3.ZERO;
                        this.speed = SPEED_BASE;
                        this.rotSpeed = ROT_SPEED_BASE;
                    }
                } else {
                    GalacticraftCore.LOGGER.error("Astro Miner missing base position");
                    this.aiState = AISTATE_STUCK;
                }
            }
            default -> {
            }
        }
        setPos(getX() + this.motion.x, getY() + this.motion.y, getZ() + this.motion.z);
        sync();
    }

    private void sync() {
        this.entityData.set(AI_STATE, this.player == null ? AISTATE_OFFLINE : this.aiState);
        this.entityData.set(ENERGY, this.energyLevel);
        this.entityData.set(MINE_COUNT, this.mineCount);
    }

    /** 1.12.2 checkPlayer: the owner, while they are online. */
    private void checkPlayer() {
        this.flagCheckPlayer = false;
        if (this.player == null || this.player.isRemoved() || this.player.hasDisconnected()) {
            this.player = this.playerUUID == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(this.playerUUID);
        }
    }

    /** Link to the base, or stop if another miner has it; warn the owner once if it has gone. */
    private void checkBase() {
        if (this.posBase == null || !level().isLoaded(this.posBase)) {
            return;
        }
        if (level().getBlockEntity(this.posBase) instanceof MinerBaseBlockEntity base && base.isMaster() && !base.isRemoved()) {
            final UUID linked = base.linkedMinerId();
            if (!getUUID().equals(linked)) {
                if (linked == null) {
                    base.linkMiner(this);
                } else {
                    freeze(FAIL_ANOTHERWASLINKED);
                }
            } else if (base.linkedMiner() != this) {
                base.linkMiner(this);
            }
        } else if ((this.givenFailMessage & 1 << FAIL_BASEDESTROYED) == 0) {
            message(FAIL_BASEDESTROYED);
            this.givenFailMessage += 1 << FAIL_BASEDESTROYED;
        }
    }

    private void message(int failure) {
        if (this.player != null) {
            this.player.sendSystemMessage(Component.translatable("gui.galacticraftcore.astro_miner.fail_" + failure));
        }
    }

    private void freeze(int failure) {
        this.aiState = AISTATE_STUCK;
        this.motion = Vec3.ZERO;
        if (this.player != null && (this.givenFailMessage & 1 << failure) == 0) {
            message(failure);
            this.givenFailMessage += 1 << failure;
        }
    }

    /** 1.12.2 recall: a miner on its way out or mining turns for home. */
    public void recall() {
        if (this.aiState > AISTATE_ATBASE && this.aiState < AISTATE_RETURNING) {
            this.aiState = AISTATE_RETURNING;
            this.pathBlockedCount = 0;
        }
    }

    private Direction facingFromRotation() {
        if (getXRot() > 45.0F) {
            return Direction.UP;
        }
        if (getXRot() < -45.0F) {
            return Direction.DOWN;
        }
        final float yaw = getYRot() % 360.0F;
        if (yaw < 45.0F || yaw > 315.0F) {
            return Direction.SOUTH;
        }
        if (yaw < 135.0F) {
            return Direction.EAST;
        }
        return yaw < 225.0F ? Direction.NORTH : Direction.WEST;
    }

    /** 1.12.2 atBase: unload into the base, charge up (16 a tick), and set off when full, empty and given a target. */
    private void atBase() {
        if (!(this.posBase != null && level().getBlockEntity(this.posBase) instanceof MinerBaseBlockEntity base) || base.isRemoved() || !base.isMaster()) {
            freeze(FAIL_BASEDESTROYED);
            return;
        }
        this.givenFailMessage &= 64;
        this.wayPoints.clear();
        boolean somethingTransferred = true;
        if (this.tickCount % 5 == 0) {
            somethingTransferred = emptyInventory(base);
        }
        this.inventoryDrops = 0;
        if (base.hasEnoughEnergyToRun() && this.energyLevel < MAX_ENERGY) {
            this.energyLevel += 16;
            base.energy().consume(MinerBaseBlockEntity.ENERGY_USE);
        }
        if (this.energyLevel >= MAX_ENERGY && !somethingTransferred && hasHoldSpace()) {
            this.energyLevel = MAX_ENERGY;
            if (findNextTarget(base)) {
                this.aiState = AISTATE_TRAVELLING;
                this.wayPoints.add(this.waypointBase);
                this.mineCount = 0;
            } else if ((this.givenFailMessage & 64) == 0) {
                message(6);
                this.givenFailMessage += 64;
            }
        }
    }

    /** 1.12.2 emptyInventory: one stack (or part of one) a time into the base. */
    private boolean emptyInventory(MinerBaseBlockEntity base) {
        for (int i = 0; i < this.stacks.size(); i++) {
            final ItemStack stack = this.stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            final int before = stack.getCount();
            base.addToInventory(stack);
            if (stack.isEmpty()) {
                this.stacks.set(i, ItemStack.EMPTY);
                return true;
            } else if (stack.getCount() < before) {
                return true;
            }
        }
        return false;
    }

    private boolean hasHoldSpace() {
        for (ItemStack stack : this.stacks) {
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean findNextTarget(MinerBaseBlockEntity base) {
        if (!this.minePoints.isEmpty() && this.pathBlockedCount < 2) {
            this.posTarget = this.minePoints.getFirst();
            return true;
        }
        this.posTarget = base.findNextTarget();
        this.pathBlockedCount = 0;
        return this.posTarget != null;
    }

    private boolean moveToTarget() {
        if (this.energyLevel < RETURN_ENERGY || this.inventoryDrops > RETURN_DROPS) {
            this.aiState = AISTATE_RETURNING;
            this.pathBlockedCount = 0;
            return true;
        }
        if (this.posTarget == null) {
            GalacticraftCore.LOGGER.error("Astro Miner missing target");
            this.aiState = AISTATE_STUCK;
            return true;
        }
        if (moveToPos(this.posTarget, false)) {
            this.aiState = AISTATE_MINING;
            this.wayPoints.add(this.posTarget);
            setMinePoints();
            return true;
        }
        return false;
    }

    private void moveToBase() {
        if (this.wayPoints.isEmpty()) {
            this.aiState = AISTATE_DOCKING;
            if (this.waypointBase != null) {
                setPos(this.waypointBase.getX(), this.waypointBase.getY(), this.waypointBase.getZ());
                this.facingAI = this.baseFacing;
            }
            return;
        }
        if (moveToPos(this.wayPoints.getLast(), true)) {
            this.wayPoints.removeLast();
        }
    }

    /** 1.12.2 setMinePoints: 14 shafts round the target, 12 long among the asteroids (24 elsewhere). */
    private void setMinePoints() {
        if (!this.minePoints.isEmpty()) {
            return;
        }
        final BlockPos front = BlockPos.containing(getX() + 0.5D, getY() + 1.5D, getZ() + 0.5D);
        int end = onAsteroids() ? MINE_LENGTH_AST : MINE_LENGTH;
        if (this.baseFacing == Direction.NORTH || this.baseFacing == Direction.WEST) {
            end = -end;
        }
        final int[][] offsets = {{0, 0, 1}, {4, 0, 1}, {4, 0, 0}, {2, 3, 0}, {2, 3, 1}, {-2, 3, 1}, {-2, 3, 0}, {-4, 0, 0}, {-4, 0, 1},
                {-2, -3, 1}, {-2, -3, 0}, {2, -3, 0}, {2, -3, 1}, {0, 0, 1}};
        final boolean alongZ = this.baseFacing.getAxis() == Direction.Axis.Z;
        for (int[] offset : offsets) {
            final int along = offset[2] * end;
            this.minePoints.add(alongZ ? front.offset(offset[0], offset[1], along) : front.offset(along, offset[1], offset[0]));
        }
    }

    private boolean doMining() {
        if (this.energyLevel < RETURN_ENERGY || this.inventoryDrops > RETURN_DROPS || this.minePoints.isEmpty()) {
            if (!this.minePoints.isEmpty() && this.minePointCurrent != null) {
                this.minePoints.addFirst(this.minePointCurrent);
            }
            this.aiState = AISTATE_RETURNING;
            this.pathBlockedCount = 0;
            return true;
        }
        if (moveToPos(this.minePoints.getFirst(), false)) {
            this.minePointCurrent = this.minePoints.removeFirst();
            return true;
        }
        return false;
    }

    private void tryBackIn() {
        if (this.waypointBase != null && this.waypointBase.distSqr(BlockPos.containing(getX(), getY(), getZ())) <= 9.1D) {
            this.aiState = AISTATE_DOCKING;
            this.targetYaw = yawFor(this.baseFacing);
        } else {
            freeze(FAIL_RETURNPATHBLOCKED);
        }
    }

    /**
     * 1.12.2 prepareMove: clear the 12 blocks {@code dist} ahead (2: the far edge), at most {@code limit} this tick, waiting
     * three calls at each new position. True if the way is barred; with nothing to cut, the miner speeds up.
     */
    private boolean prepareMove(int limit, int dist) {
        if (this.mineCountDown > 0) {
            this.mineCountDown--;
            return false;
        }
        BlockPos front = BlockPos.containing(getX() + 0.5D, getY() + 1.5D, getZ() + 0.5D);
        final int index = this.facingAI.get3DDataValue();
        if (dist == 2) {
            front = front.offset(HEADINGS_2[index][0], HEADINGS_2[index][1], HEADINGS_2[index][2]);
        } else {
            if ((index & 1) == 0) {
                dist++;
            }
            if (dist > 0) {
                front = front.offset(HEADINGS[index][0] * dist, HEADINGS[index][1] * dist, HEADINGS[index][2] * dist);
            }
        }
        if (!front.equals(this.mineLast) && this.aiState != AISTATE_ATBASE) {
            this.mineCountDown = 3;
            this.mineLast = front;
            return false;
        }
        if (this.waypointBase != null && front.getY() == this.waypointBase.getY()
                && front.getX() == this.waypointBase.getX() - (this.baseFacing == Direction.EAST ? 1 : 0)
                && front.getZ() == this.waypointBase.getZ() - (this.baseFacing == Direction.SOUTH ? 1 : 0)) {
            tryBackIn();
            return false;
        }
        boolean barred = false;
        this.tryBlockLimit = limit;
        for (int[] offset : MINE_PATTERN[index >> 1]) {
            if (tryMineBlock(front.offset(offset[0], offset[1], offset[2]))) {
                barred = true;
            }
        }
        if (barred) {
            if (this.player != null && this.blockingBlock != Blocks.AIR && this.aiState != AISTATE_ATBASE) {
                this.player.sendSystemMessage(Component.translatable("gui.galacticraftcore.astro_miner.route_blocked").append(" ")
                        .append(this.blockingBlock == Blocks.STONE ? Component.translatable("gui.galacticraftcore.astro_miner.protected")
                                : this.blockingBlock.getName()));
            }
            this.motion = Vec3.ZERO;
            this.tryBlockLimit = 0;
            if (this.aiState == AISTATE_TRAVELLING) {
                this.aiState = AISTATE_RETURNING;
            } else if (this.aiState == AISTATE_MINING) {
                this.pathBlockedCount++;
                this.aiState = AISTATE_RETURNING;
            } else if (this.aiState == AISTATE_RETURNING) {
                tryBackIn();
            } else if (this.aiState != AISTATE_ATBASE) {
                freeze(FAIL_RETURNPATHBLOCKED);
            }
            if (this.aiState != AISTATE_ATBASE) {
                this.blockingBlock = Blocks.AIR;
            }
        }
        if (this.tryBlockLimit == limit && !this.noSpeedup) {
            this.motion = this.motion.scale(this.speedup);
        }
        return barred;
    }

    /** 1.12.2 tryMineBlock: true if this block bars the way; otherwise mine it if the limit allows. */
    private boolean tryMineBlock(BlockPos pos) {
        final BlockState state = level().getBlockState(pos);
        final Block block = state.getBlock();
        if (state.isAir()) {
            return false;
        }
        if (unminable(state)) {
            this.blockingBlock = block;
            return true;
        }
        final FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            if (fluid.getType() == Fluids.LAVA && fluid.isSource() && this.aiState != AISTATE_RETURNING) {
                this.blockingBlock = Blocks.LAVA;
                return true;
            }
            return false;
        }
        if (block != GCBlocks.FALLEN_METEOR.get()) {
            if (isGrowingPlant(block) || state.getDestroySpeed(level(), pos) < 0.0F || state.hasBlockEntity()) {
                this.blockingBlock = block;
                return true;
            }
        }
        if (this.tryBlockLimit == 0) {
            return false;
        }
        if (this.player == null || CommonHooks.fireBlockBreak(level(), this.player.gameMode.getGameModeForPlayer(), this.player, pos, state).isCanceled()) {
            this.blockingBlock = Blocks.STONE;
            return true;
        }
        this.tryBlockLimit--;
        if (!(block instanceof BushBlock && !(block instanceof SugarCaneBlock) || block instanceof LeavesBlock)) {
            final ItemStack drop = pickItem(state);
            if (!drop.isEmpty() && !addToInventory(drop)) {
                dropStack(pos, drop);
            }
        }
        level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return false;
    }

    /** 1.12.2 noMineList: bedrock, mossy cobblestone, portals, stone bricks, farmland, rails, levers, redstone, walkways. */
    public static boolean unminable(BlockState state) {
        final Block block = state.getBlock();
        return block == Blocks.BEDROCK || block == Blocks.MOSSY_COBBLESTONE || block == Blocks.END_PORTAL || block == Blocks.END_PORTAL_FRAME
                || block == Blocks.NETHER_PORTAL || state.is(BlockTags.STONE_BRICKS) || block == Blocks.FARMLAND || block == Blocks.RAIL
                || block == Blocks.LEVER || block == Blocks.REDSTONE_WIRE || WalkwayBlock.isWalkway(state);
    }

    /** 1.12.2: plants other than grass, dead bushes, tall plants, lily pads, flowers and sugar cane bar the way. */
    private static boolean isGrowingPlant(Block block) {
        if (!(block instanceof BushBlock || block instanceof CactusBlock)) {
            return false;
        }
        return !(block instanceof TallGrassBlock || block instanceof DeadBushBlock || block instanceof DoublePlantBlock || block instanceof WaterlilyBlock
                || block.defaultBlockState().is(BlockTags.FLOWERS) || block instanceof SugarCaneBlock);
    }

    /** 1.12.2 getPickBlock: the block itself (fallen meteors give raw meteoric iron). */
    private static ItemStack pickItem(BlockState state) {
        if (state.is(GCBlocks.FALLEN_METEOR.get())) {
            return new ItemStack(GCItems.METEORIC_IRON_RAW.get());
        }
        final Item item = state.getBlock().asItem();
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void dropStack(BlockPos pos, ItemStack drop) {
        final float f = 0.7F;
        final ItemEntity item = new ItemEntity(level(), pos.getX() + this.random.nextFloat() * f + (1.0F - f) * 0.5D,
                pos.getY() + this.random.nextFloat() * f + (1.0F - f) * 0.5D, pos.getZ() + this.random.nextFloat() * f + (1.0F - f) * 0.5D, drop);
        item.setDefaultPickUpDelay();
        level().addFreshEntity(item);
        this.inventoryDrops++;
    }

    /** 1.12.2 addToInventory: top up matching stacks, then the first empty slot; counts as mined. */
    private boolean addToInventory(ItemStack stack) {
        boolean added = false;
        if (stack.isStackable()) {
            for (int k = 0; k < this.stacks.size() && !stack.isEmpty(); k++) {
                final ItemStack existing = this.stacks.get(k);
                if (ItemStack.isSameItemSameComponents(stack, existing)) {
                    final int combined = existing.getCount() + stack.getCount();
                    if (combined <= stack.getMaxStackSize()) {
                        stack.setCount(0);
                        existing.setCount(combined);
                        added = true;
                    } else if (existing.getCount() < stack.getMaxStackSize()) {
                        stack.shrink(stack.getMaxStackSize() - existing.getCount());
                        existing.setCount(stack.getMaxStackSize());
                        added = true;
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            for (int k = 0; k < this.stacks.size(); k++) {
                if (this.stacks.get(k).isEmpty()) {
                    this.stacks.set(k, stack.copy());
                    stack.setCount(0);
                    added = true;
                    break;
                }
            }
        }
        if (added) {
            this.mineCount++;
        }
        return added;
    }

    /**
     * 1.12.2 moveToPos: one axis at a time (z, y, x going out on a north/south base; x, y, z otherwise; reversed coming
     * back, to use the same tunnels). The miner's position is its centre minus one block in y. True when there.
     */
    private boolean moveToPos(BlockPos pos, boolean reverse) {
        this.noSpeedup = false;
        final boolean zFirst = reverse != (this.baseFacing.get3DDataValue() < 4);
        final boolean atX = Math.abs(getX() - pos.getX()) <= 0.0001D;
        final boolean atY = Math.abs(getY() - (pos.getY() - 1)) <= 0.0001D;
        final boolean atZ = Math.abs(getZ() - pos.getZ()) <= 0.0001D;
        if (zFirst) {
            if (!atZ) {
                moveToPosZ(pos.getZ());
            } else if (!atY) {
                moveToPosY(pos.getY() - 1);
            } else if (!atX) {
                moveToPosX(pos.getX());
            } else {
                return true;
            }
        } else {
            if (!atX) {
                moveToPosX(pos.getX());
            } else if (!atY) {
                moveToPosY(pos.getY() - 1);
            } else if (!atZ) {
                moveToPosZ(pos.getZ());
            } else {
                return true;
            }
        }
        return false;
    }

    private void moveToPosX(int x) {
        this.targetPitch = 0.0F;
        double mx;
        if (getX() > x) {
            if (this.aiState != AISTATE_DOCKING) {
                this.targetYaw = 270.0F;
            }
            mx = -this.speed;
            if (mx * this.speedup <= x - getX()) {
                mx = x - getX();
                this.noSpeedup = true;
            }
            this.facingAI = Direction.WEST;
        } else {
            if (this.aiState != AISTATE_DOCKING) {
                this.targetYaw = 90.0F;
            }
            mx = this.speed;
            if (mx * this.speedup >= x - getX()) {
                mx = x - getX();
                this.noSpeedup = true;
            }
            this.facingAI = Direction.EAST;
        }
        this.motion = new Vec3(this.stopForTurn ? 0.0D : mx, 0.0D, 0.0D);
    }

    private void moveToPosY(int y) {
        double my;
        if (getY() > y) {
            this.targetPitch = -90.0F;
            my = -this.speed;
            if (my * this.speedup <= y - getY()) {
                my = y - getY();
                this.noSpeedup = true;
            }
            this.facingAI = Direction.DOWN;
        } else {
            this.targetPitch = 90.0F;
            my = this.speed;
            if (my * this.speedup >= y - getY()) {
                my = y - getY();
                this.noSpeedup = true;
            }
            this.facingAI = Direction.UP;
        }
        this.motion = new Vec3(0.0D, this.stopForTurn ? 0.0D : my, 0.0D);
    }

    private void moveToPosZ(int z) {
        this.targetPitch = 0.0F;
        double mz;
        if (getZ() > z) {
            if (this.aiState != AISTATE_DOCKING) {
                this.targetYaw = 180.0F;
            }
            mz = -this.speed;
            if (mz * this.speedup <= z - getZ()) {
                mz = z - getZ();
                this.noSpeedup = true;
            }
            this.facingAI = Direction.NORTH;
        } else {
            if (this.aiState != AISTATE_DOCKING) {
                this.targetYaw = 0.0F;
            }
            mz = this.speed;
            if (mz * this.speedup >= z - getZ()) {
                mz = z - getZ();
                this.noSpeedup = true;
            }
            this.facingAI = Direction.SOUTH;
        }
        this.motion = new Vec3(0.0D, 0.0D, this.stopForTurn ? 0.0D : mz);
    }

    /** 1.12.2 checkRotation: turn towards the target pitch and yaw at 1.5 degrees a tick; false while still turning. */
    private boolean checkRotation() {
        boolean done = true;
        float pitch = getXRot();
        if (Math.abs(pitch - this.targetPitch) > 0.001F) {
            if (pitch > this.targetPitch + 180.0F) {
                pitch -= 360.0F;
            } else if (pitch < this.targetPitch - 180.0F) {
                pitch += 360.0F;
            }
            pitch = pitch > this.targetPitch ? Math.max(this.targetPitch, pitch - this.rotSpeed) : Math.min(this.targetPitch, pitch + this.rotSpeed);
            setXRot(pitch);
        }
        float yaw = getYRot();
        if (Math.abs(yaw - this.targetYaw) > 0.001F) {
            if (yaw > this.targetYaw + 180.0F) {
                yaw -= 360.0F;
            } else if (yaw < this.targetYaw - 180.0F) {
                yaw += 360.0F;
            }
            yaw = yaw > this.targetYaw ? Math.max(this.targetYaw, yaw - this.rotSpeed) : Math.min(this.targetYaw, yaw + this.rotSpeed);
            setYRot(yaw);
            done = false;
        }
        return done;
    }

    /** 1.12.2 setBoundingBoxForFacing: 2.6 long the way it points, 1.8 x 1.7 across, centred a block above its position. */
    @Override
    protected AABB makeBoundingBox() {
        double xs = C_WIDTH;
        double ys = C_WIDTH;
        double zs = C_WIDTH;
        // Entity's constructor asks for a box before this class has set its facing
        switch (this.facing == null ? Direction.NORTH : this.facing) {
            case DOWN, UP -> ys = C_LENGTH;
            case NORTH, SOUTH -> {
                ys = C_HEIGHT;
                zs = C_LENGTH;
            }
            default -> {
                ys = C_HEIGHT;
                xs = C_LENGTH;
            }
        }
        return new AABB(getX() - xs / 2.0D, getY() + 1.0D - ys / 2.0D, getZ() - zs / 2.0D, getX() + xs / 2.0D, getY() + 1.0D + ys / 2.0D, getZ() + zs / 2.0D);
    }

    // ------------------------------------------------------------------ client lasers (1.12.2 prepareMoveClient)

    private void prepareMoveClient(int limit, int dist) {
        BlockPos front = BlockPos.containing(getX() + 0.5D, getY() + 1.5D, getZ() + 0.5D);
        final int index = this.facing.get3DDataValue();
        if (dist == 2) {
            front = front.offset(HEADINGS_2[index][0], HEADINGS_2[index][1], HEADINGS_2[index][2]);
        } else {
            if ((index & 1) == 0) {
                dist++;
            }
            front = front.offset(HEADINGS[index][0] * dist, HEADINGS[index][1] * dist, HEADINGS[index][2] * dist);
        }
        if (front.equals(this.mineLast)) {
            return;
        }
        boolean barred = false;
        this.tryBlockLimit = limit;
        for (int[] offset : MINE_PATTERN[index >> 1]) {
            if (tryBlockClient(front.offset(offset[0], offset[1], offset[2]))) {
                barred = true;
            }
        }
        if (barred) {
            this.tryBlockLimit = 0;
        }
        if (this.tryBlockLimit == limit) {
            this.mineLast = front;
        }
    }

    private boolean tryBlockClient(BlockPos pos) {
        if (this.laserBlocks.contains(pos)) {
            return false;
        }
        final BlockState state = level().getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (unminable(state)) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.getBlock() instanceof BushBlock || state.hasBlockEntity() || state.getDestroySpeed(level(), pos) < 0.0F) {
            return true;
        }
        if (this.tryBlockLimit == 0) {
            return false;
        }
        this.tryBlockLimit--;
        this.laserBlocks.add(pos.immutable());
        this.laserTimes.add(this.tickCount);
        return false;
    }

    // ------------------------------------------------------------------ chunk loading (1.12.2 AsteroidsTickHandlerServer)

    private void keepChunkLoaded() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final net.minecraft.world.level.ChunkPos chunk = chunkPosition();
        if (!chunk.equals(this.forcedChunk)) {
            TICKETS.forceChunk(serverLevel, getUUID(), chunk.x, chunk.z, true, true);
            if (this.forcedChunk != null) {
                TICKETS.forceChunk(serverLevel, getUUID(), this.forcedChunk.x, this.forcedChunk.z, false, true);
            }
            this.forcedChunk = chunk;
            if (this.player != null) {
                GCPlayerData.get(this.player).setAstroMinerChunk(getUUID(), level().dimension(), chunk);
            }
        }
    }

    private void releaseChunk() {
        if (this.forcedChunk != null && level() instanceof ServerLevel serverLevel) {
            TICKETS.forceChunk(serverLevel, getUUID(), this.forcedChunk.x, this.forcedChunk.z, false, true);
            this.forcedChunk = null;
        }
    }

    // ------------------------------------------------------------------ damage and removal

    public float damage() {
        return this.entityData.get(DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DAMAGE, damage);
    }

    /**
     * 1.12.2 attackEntityFrom: creative players remove it outright; while its owner is online, other players' hits (and
     * other damage, but not mobs') wear it down until it breaks into its item and its hold.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isRemoved() || source.is(net.minecraft.world.damagesource.DamageTypes.CACTUS)) {
            return true;
        }
        if (level().isClientSide()) {
            return true;
        }
        final Entity attacker = source.getEntity();
        if (attacker instanceof Player player && player.getAbilities().instabuild) {
            if (this.player == null && !this.spawnedInCreative) {
                player.sendSystemMessage(Component.translatable("gui.galacticraftcore.astro_miner.offline_owner"));
            }
            discard();
            return true;
        }
        if (this.player == null || attacker instanceof LivingEntity && !(attacker instanceof Player)) {
            return false;
        }
        markHurt();
        this.shipDamage += amount * 10.0F;
        if (attacker instanceof Player) {
            this.shipDamage += amount * 21.0F;
        }
        if (this.shipDamage > 90.0F) {
            dropAsItems();
            discard();
        }
        return true;
    }

    private void dropAsItems() {
        spawnAtLocation(new ItemStack(GCItems.ASTRO_MINER.get()), 0.0F);
        for (int i = 0; i < this.stacks.size(); i++) {
            if (!this.stacks.get(i).isEmpty()) {
                spawnAtLocation(this.stacks.get(i), 0.0F);
            }
            this.stacks.set(i, ItemStack.EMPTY);
        }
    }

    /** 1.12.2 setDead: the owner gets a miner back in their allowance; the base forgets it. */
    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy()) {
            releaseChunk();
            final ServerPlayer owner = this.player != null ? this.player
                    : this.playerUUID == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(this.playerUUID);
            if (owner != null) {
                final GCPlayerData data = GCPlayerData.get(owner);
                if (!this.spawnedInCreative && data.astroMinerCount > 0) {
                    data.astroMinerCount--;
                }
                data.removeAstroMinerChunk(getUUID());
            }
            if (this.posBase != null && level().getBlockEntity(this.posBase) instanceof MinerBaseBlockEntity base) {
                base.unlinkMiner();
            }
        } else if (!level().isClientSide()) {
            releaseChunk();
        }
        super.remove(reason);
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
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = getBoundingBox().getSize();
        if (Double.isNaN(size)) {
            size = 1.0D;
        }
        size *= 64.0D * 5.0D;
        return distance < size * size;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(GCItems.ASTRO_MINER.get());
    }

    // ------------------------------------------------------------------ saving

    private static ListTag positions(List<BlockPos> positions) {
        final ListTag list = new ListTag();
        positions.forEach(pos -> list.add(NbtUtils.writeBlockPos(pos)));
        return list;
    }

    private static void readPositions(CompoundTag tag, String key, List<BlockPos> into) {
        into.clear();
        for (Tag entry : tag.getList(key, Tag.TAG_INT_ARRAY)) {
            final int[] values = ((net.minecraft.nbt.IntArrayTag) entry).getAsIntArray();
            if (values.length == 3) {
                into.add(new BlockPos(values[0], values[1], values[2]));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ContainerHelper.saveAllItems(tag, this.stacks, registryAccess());
        tag.putInt("Energy", this.energyLevel);
        if (this.posBase != null) {
            tag.put("Base", NbtUtils.writeBlockPos(this.posBase));
        }
        if (this.posTarget != null) {
            tag.put("Target", NbtUtils.writeBlockPos(this.posTarget));
        }
        if (this.waypointBase != null) {
            tag.put("WBase", NbtUtils.writeBlockPos(this.waypointBase));
        }
        tag.putInt("BaseFacing", this.baseFacing.get3DDataValue());
        tag.putInt("AIState", this.aiState);
        tag.putInt("Facing", this.facingAI.get3DDataValue());
        tag.put("WayPoints", positions(this.wayPoints));
        tag.put("MinePoints", positions(this.minePoints));
        if (this.minePointCurrent != null) {
            tag.put("MinePointCurrent", NbtUtils.writeBlockPos(this.minePointCurrent));
        }
        if (this.playerUUID != null) {
            tag.putUUID("Player", this.playerUUID);
        }
        tag.putDouble("speedup", this.speedup);
        tag.putInt("pathBlockedCount", this.pathBlockedCount);
        tag.putBoolean("spawnedInCreative", this.spawnedInCreative);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.stacks = NonNullList.withSize(INV_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.stacks, registryAccess());
        int items = 0;
        for (ItemStack stack : this.stacks) {
            items += stack.getCount();
        }
        this.mineCount = items;
        this.energyLevel = tag.getInt("Energy");
        this.posBase = NbtUtils.readBlockPos(tag, "Base").orElse(null);
        this.flagLink = this.posBase != null;
        this.posTarget = NbtUtils.readBlockPos(tag, "Target").orElse(null);
        this.waypointBase = NbtUtils.readBlockPos(tag, "WBase").orElse(null);
        this.baseFacing = Direction.from3DDataValue(tag.getInt("BaseFacing"));
        if (this.baseFacing.getAxis() == Direction.Axis.Y) {
            this.baseFacing = Direction.NORTH;
        }
        this.aiState = tag.contains("AIState") ? tag.getInt("AIState") : AISTATE_ATBASE;
        this.facingAI = Direction.from3DDataValue(tag.getInt("Facing"));
        if (this.facingAI.getAxis() != Direction.Axis.Y) {
            this.targetYaw = yawFor(this.facingAI);
        }
        this.lastFacing = null;
        readPositions(tag, "WayPoints", this.wayPoints);
        readPositions(tag, "MinePoints", this.minePoints);
        this.minePointCurrent = NbtUtils.readBlockPos(tag, "MinePointCurrent").orElse(null);
        this.playerUUID = tag.hasUUID("Player") ? tag.getUUID("Player") : null;
        this.speedup = tag.contains("speedup") ? tag.getDouble("speedup") : onAsteroids() ? SPEEDUP * 1.6D : SPEEDUP;
        this.pathBlockedCount = tag.getInt("pathBlockedCount");
        this.spawnedInCreative = tag.getBoolean("spawnedInCreative");
        this.flagCheckPlayer = true;
    }

    /** For tests: the points still to mine. */
    public List<BlockPos> minePoints() {
        return new ArrayList<>(this.minePoints);
    }

    // ------------------------------------------------------------------ telemetry (1.12.2 transmitData / receiveData)

    @Override
    public void transmitData(int[] data) {
        data[0] = (int) getX();
        data[1] = (int) getY();
        data[2] = (int) getZ();
        data[3] = this.energyLevel;
        data[4] = this.aiState;
    }

    @Override
    public void receiveData(int[] data, net.minecraft.network.chat.Component[] lines) {
        lines[1] = net.minecraft.network.chat.Component.literal("x: " + data[0]);
        lines[2] = net.minecraft.network.chat.Component.literal("y: " + data[1]);
        lines[3] = net.minecraft.network.chat.Component.literal("z: " + data[2]);
        lines[4] = net.minecraft.network.chat.Component.translatable("gui.galacticraftcore.energy_storage.desc.1").append(": " + data[3] / 120 + "%");
        lines[0] = net.minecraft.network.chat.Component.translatable(switch (data[4]) {
            case AISTATE_STUCK -> "gui.galacticraftcore.message.no_energy";
            case AISTATE_ATBASE -> "gui.galacticraftcore.miner.docked";
            case AISTATE_TRAVELLING -> "gui.galacticraftcore.miner.travelling";
            case AISTATE_MINING -> "gui.galacticraftcore.miner.mining";
            case AISTATE_RETURNING -> "gui.galacticraftcore.miner.returning";
            case AISTATE_DOCKING -> "gui.galacticraftcore.miner.docking";
            case AISTATE_OFFLINE -> "gui.galacticraftcore.miner.offline";
            default -> "";
        });
    }

    @Override
    public void adjustDisplay(int[] data, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.scale(0.9F, 0.9F, 0.9F);
    }
}
