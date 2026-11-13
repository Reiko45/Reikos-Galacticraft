/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityMinerBase and
 * dimension.WorldProviderAsteroids#getClosestAsteroidsXZ). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.energy.IGCEnergyHandler;
import io.github.reiko45.galacticraft.entity.AstroMinerEntity;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.machine.EnergyAccess;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.AstroMinerDockMenu;
import io.github.reiko45.galacticraft.menu.SyncedData;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Astro Miner Base (1.12.2 TileEntityMinerBase): eight blocks make one 2 x 2 x 2 base; the lowest corner is the master
 * and holds the 72-slot store, the battery and the energy (16,000 gJ, 30 a tick in through any face on its back). The
 * others pass power, items and clicks to it. The miner docks in the front face, and the base gives it its mining
 * targets. Nothing goes in by automation; anything but its front face can take the store out.
 */
public class MinerBaseBlockEntity extends MachineBlockEntity {
    public static final int HOLD_SIZE = 72;
    /** 1.12.2 setMaxExtract(12) (20 in hard mode): what charging the miner draws each tick. */
    public static final float ENERGY_USE = 12.0F;

    /** 1.12.2: 20 gJ/t in hard mode. */
    public static float energyUse() {
        return io.github.reiko45.galacticraft.GCConfig.hardMode() ? 20.0F : ENERGY_USE;
    }

    private boolean master;
    @Nullable
    private BlockPos masterPos;
    private Direction facing = Direction.NORTH;
    @Nullable
    private UUID linkedMinerId;
    @Nullable
    private AstroMinerEntity linkedMiner;
    private final LinkedList<BlockPos> targetPoints = new LinkedList<>();
    private boolean findTargetPointsFlag;
    private boolean hasEnoughEnergyToRun;
    private int ticks;

    public MinerBaseBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.MINER_BASE.get(), pos, state, HOLD_SIZE + 1, 16_000.0F, 2.5F * ENERGY_USE, 0.0F);
    }

    // ------------------------------------------------------------------ the multiblock

    /** 1.12.2 setMainBlockPos: the master is the block at the master position. */
    public void setMaster(BlockPos masterPos) {
        this.master = this.worldPosition.equals(masterPos);
        this.masterPos = this.master ? null : masterPos.immutable();
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isMaster() {
        return this.master;
    }

    @Nullable
    public BlockPos masterPos() {
        return this.master ? this.worldPosition : this.masterPos;
    }

    @Nullable
    public MinerBaseBlockEntity getMaster() {
        if (this.master) {
            return this;
        }
        return this.masterPos != null && this.level != null && this.level.getBlockEntity(this.masterPos) instanceof MinerBaseBlockEntity base
                && base.master ? base : null;
    }

    public Direction facing() {
        final MinerBaseBlockEntity master = getMaster();
        return master == null ? this.facing : master.facing;
    }

    /** The 8 blocks of the base whose master is at {@code master}. */
    public static List<BlockPos> parts(BlockPos master) {
        final List<BlockPos> parts = new ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    parts.add(master.offset(x, y, z));
                }
            }
        }
        return parts;
    }

    /** 1.12.2 updateFacing (the wrench): turn the dock a quarter, while no miner is linked. */
    public void rotate() {
        final MinerBaseBlockEntity master = getMaster();
        if (master == null) {
            return;
        }
        if (master.linkedMinerId == null) {
            master.facing = switch (master.facing) {
                case SOUTH -> Direction.WEST;
                case EAST -> Direction.SOUTH;
                case WEST -> Direction.NORTH;
                default -> Direction.EAST;
            };
            master.findTargetPointsFlag = true;
            master.setChanged();
            for (BlockPos part : parts(master.worldPosition)) {
                if (this.level.getBlockEntity(part) instanceof MinerBaseBlockEntity base) {
                    this.level.sendBlockUpdated(part, base.getBlockState(), base.getBlockState(), 3);
                }
            }
            this.level.updateNeighborsAt(master.worldPosition, getBlockState().getBlock());
            this.level.invalidateCapabilities(master.worldPosition);
            for (BlockPos part : parts(master.worldPosition)) {
                this.level.invalidateCapabilities(part);
            }
        }
    }

    // ------------------------------------------------------------------ the miner

    @Nullable
    public UUID linkedMinerId() {
        final MinerBaseBlockEntity master = getMaster();
        return master == null ? null : master.linkedMinerId;
    }

    @Nullable
    public AstroMinerEntity linkedMiner() {
        final MinerBaseBlockEntity master = getMaster();
        return master == null ? null : master.linkedMiner;
    }

    public void linkMiner(AstroMinerEntity miner) {
        this.linkedMiner = miner;
        this.linkedMinerId = miner.getUUID();
        setChanged();
        syncParts();
    }

    public void unlinkMiner() {
        this.linkedMiner = null;
        this.linkedMinerId = null;
        setChanged();
        syncParts();
    }

    private void syncParts() {
        if (this.level != null && !this.level.isClientSide()) {
            for (BlockPos part : parts(this.worldPosition)) {
                this.level.sendBlockUpdated(part, this.level.getBlockState(part), this.level.getBlockState(part), 3);
            }
        }
    }

    public boolean hasEnoughEnergyToRun() {
        return this.hasEnoughEnergyToRun;
    }

    /** 1.12.2 spawnMiner: a new miner docks here, if none is linked and its space can be cleared. */
    public boolean spawnMiner(net.minecraft.server.level.ServerPlayer player) {
        final MinerBaseBlockEntity master = getMaster();
        if (master == null) {
            return false;
        }
        if (master.linkedMiner != null && master.linkedMiner.isRemoved()) {
            master.unlinkMiner();
        }
        if (master.linkedMinerId == null && AstroMinerEntity.spawnAtBase((ServerLevel) this.level, master.worldPosition, master.facing, player)) {
            master.findTargetPoints();
            return true;
        }
        return false;
    }

    /** 1.12.2 setDisabled 0: the dock's Recall button. */
    public void recall() {
        final MinerBaseBlockEntity master = getMaster();
        if (master != null && master.linkedMiner != null) {
            master.linkedMiner.recall();
        }
    }

    /** 1.12.2 findNextTarget: the next point to mine, or null when everything has been mined. */
    @Nullable
    public BlockPos findNextTarget() {
        if (!this.targetPoints.isEmpty()) {
            setChanged();
            return this.targetPoints.removeFirst();
        }
        return null;
    }

    /**
     * 1.12.2 findTargetPoints: among the asteroids, the (up to 100) nearest asteroid centres ahead of the base within 512
     * blocks, each moved half a shaft back towards it; elsewhere a fixed pattern of 6 to 9 points 32-47 blocks ahead, low
     * down, and the same again a shaft further on.
     */
    private void findTargetPoints() {
        this.targetPoints.clear();
        final BlockPos pos = this.worldPosition;
        if (this.level instanceof ServerLevel serverLevel && serverLevel.getChunkSource().getGenerator() instanceof AsteroidsChunkGenerator generator) {
            final List<BlockPos> roids = closestAsteroids(serverLevel, generator, pos, this.facing, 100);
            if (!roids.isEmpty()) {
                this.targetPoints.addAll(roids);
                setChanged();
                return;
            }
        }
        BlockPos target = pos.relative(this.facing, this.level.getRandom().nextInt(16) + 32);
        final int minY = Math.max(this.level.getMinBuildHeight() + 5, Math.min(pos.getY() * 2 - 90, pos.getY() - 22));
        target = new BlockPos(target.getX(), minY + 5 + this.level.getRandom().nextInt(4), target.getZ());
        this.targetPoints.add(target);
        final Direction lateral = this.facing.getAxis() == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        this.targetPoints.add(target.relative(lateral, 13));
        this.targetPoints.add(target.relative(lateral, -13));
        if (target.getY() > this.level.getMinBuildHeight() + 17) {
            this.targetPoints.add(target.relative(lateral, 7).below(11));
            this.targetPoints.add(target.relative(lateral, -7).below(11));
        } else {
            this.targetPoints.add(target.relative(lateral, 26));
            this.targetPoints.add(target.relative(lateral, -26));
        }
        this.targetPoints.add(target.relative(lateral, 7).above(11));
        this.targetPoints.add(target.relative(lateral, -7).above(11));
        if (target.getY() < pos.getY() - 38) {
            this.targetPoints.add(target.relative(lateral, 13).above(22));
            this.targetPoints.add(target.above(22));
            this.targetPoints.add(target.relative(lateral, -13).above(22));
        }
        final int count = this.targetPoints.size();
        for (int i = 0; i < count; i++) {
            this.targetPoints.add(this.targetPoints.get(i).relative(this.facing, AstroMinerEntity.MINE_LENGTH + 6));
        }
        setChanged();
    }

    /** 1.12.2 WorldProviderAsteroids#getClosestAsteroidsXZ. */
    public static List<BlockPos> closestAsteroids(ServerLevel level, AsteroidsChunkGenerator generator, BlockPos pos, Direction facing, int count) {
        final TreeMap<Integer, BlockPos> targets = new TreeMap<>();
        final int x = pos.getX();
        final int z = pos.getZ();
        for (AsteroidsChunkGenerator.Asteroid roid : generator.asteroidsNear(level.getChunkSource().randomState(), x, z, 512)) {
            final boolean behind = switch (facing) {
                case NORTH -> z - 16 < roid.z();
                case SOUTH -> z + 16 > roid.z();
                case WEST -> x - 16 < roid.x();
                case EAST -> x + 16 > roid.x();
                default -> false;
            };
            if (behind) {
                continue;
            }
            final int dx = x - roid.x();
            final int dz = z - roid.z();
            final int distance = dx * dx + dz * dz;
            if (distance < 262144) {
                targets.put(distance, new BlockPos(roid.x(), roid.y(), roid.z()));
            }
        }
        final List<BlockPos> result = new ArrayList<>();
        final int offset = AstroMinerEntity.MINE_LENGTH_AST / 2;
        for (BlockPos target : targets.values()) {
            result.add(target.relative(facing.getOpposite(), offset));
            if (result.size() >= count) {
                break;
            }
        }
        return result;
    }

    /** 1.12.2 addToInventory: into the store (never the battery slot), topping up stacks first. */
    public boolean addToInventory(ItemStack stack) {
        boolean added = false;
        if (stack.isStackable()) {
            for (int k = 1; k < this.inventory.getSlots() && !stack.isEmpty(); k++) {
                final ItemStack existing = this.inventory.getStackInSlot(k);
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
            for (int k = 1; k < this.inventory.getSlots(); k++) {
                if (this.inventory.getStackInSlot(k).isEmpty()) {
                    this.inventory.setStackInSlot(k, stack.copy());
                    stack.setCount(0);
                    added = true;
                    break;
                }
            }
        }
        setChanged();
        return added;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void tick() {
        if (!this.master) {
            return;
        }
        this.ticks++;
        if (this.energy.getStoredGJ() < this.energy.getCapacityGJ()) {
            dischargeBattery(0);
        }
        this.hasEnoughEnergyToRun = this.energy.getStoredGJ() > energyUse();
        if (this.ticks % 10 == 0 && this.energy.getStoredGJ() > 0.0F) {
            this.energy.consume(Math.min(5.0F, this.energy.getStoredGJ()));
        }
        if (this.findTargetPointsFlag) {
            if (this.linkedMiner != null) {
                findTargetPoints();
            }
            this.findTargetPointsFlag = false;
        }
        if (this.linkedMiner != null && this.linkedMiner.isRemoved()) {
            this.linkedMiner = null;
        }
    }

    public int ticks() {
        return this.ticks;
    }

    /** 1.12.2 updateGUIstate: -3 with no miner (or none loaded), else the miner's state, where it is and what it mined. */
    public int guiState() {
        if (this.linkedMinerId == null || this.linkedMiner == null || this.linkedMiner.isRemoved()) {
            return -3;
        }
        return this.linkedMiner.syncedState();
    }

    private int minerDelta(int axis) {
        if (this.linkedMiner == null) {
            return 0;
        }
        return switch (axis) {
            case 0 -> Mth.floor(this.linkedMiner.getX()) - this.worldPosition.getX() - 1;
            case 1 -> Mth.floor(this.linkedMiner.getY()) - this.worldPosition.getY() - 1;
            default -> Mth.floor(this.linkedMiner.getZ()) - this.worldPosition.getZ() - 1;
        };
    }

    // ------------------------------------------------------------------ capabilities

    /** Power in through the faces on the base's back (1.12.2 getElectricInputDirection: facing.getOpposite()). */
    @Nullable
    @Override
    public IGCEnergyHandler energyHandler(@Nullable Direction side) {
        final MinerBaseBlockEntity master = getMaster();
        if (master == null) {
            return null;
        }
        if (side == null) {
            return master.energy;
        }
        return side == master.facing.getOpposite() ? master.energy.view(true, false) : null;
    }

    /** Items out of every face but the front; nothing in (1.12.2 canInsertItem false). */
    @Nullable
    @Override
    public IItemHandler itemHandler(@Nullable Direction side) {
        final MinerBaseBlockEntity master = getMaster();
        if (master == null) {
            return null;
        }
        if (side == master.facing) {
            return null;
        }
        return master.superItemHandler(side);
    }

    @Nullable
    private IItemHandler superItemHandler(@Nullable Direction side) {
        return super.itemHandler(side);
    }

    @Override
    protected EnergyAccess energyAccess(RelativeSide side) {
        return EnergyAccess.INPUT;
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        final int[] slots = new int[HOLD_SIZE + 1];
        for (int i = 0; i <= HOLD_SIZE; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return false;
    }

    /** 1.12.2 canExtractItem: the store, and the battery only once it is empty. */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        return slot > 0 || this.inventory.getStackInSlot(0).getItem() instanceof ElectricItem electric
                && electric.stored(this.inventory.getStackInSlot(0)) <= 0.0F;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot > 0 || stack.getItem() instanceof ElectricItem;
    }

    // ------------------------------------------------------------------ menu, saving, syncing

    /** Synced: the miner's state, where it is relative to the dock, and how many blocks it has mined. */
    @Override
    protected void addSyncedData(SyncedData data) {
        data.addShort(this::guiState);
        data.addShort(() -> minerDelta(0));
        data.addShort(() -> minerDelta(1));
        data.addShort(() -> minerDelta(2));
        data.addInt(() -> this.linkedMiner == null ? 0 : this.linkedMiner.syncedMineCount());
        data.addShort(() -> this.linkedMinerId == null ? 0 : 1);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AstroMinerDockMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isMaster", this.master);
        if (this.masterPos != null) {
            tag.put("MasterPos", NbtUtils.writeBlockPos(this.masterPos));
        }
        tag.putInt("facing", this.facing.get2DDataValue());
        if (this.linkedMinerId != null) {
            tag.putUUID("LinkedMiner", this.linkedMinerId);
        }
        final ListTag points = new ListTag();
        this.targetPoints.forEach(point -> points.add(NbtUtils.writeBlockPos(point)));
        tag.put("TargetPoints", points);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.master = tag.getBoolean("isMaster");
        this.masterPos = NbtUtils.readBlockPos(tag, "MasterPos").orElse(null);
        this.facing = Direction.from2DDataValue(tag.getInt("facing"));
        this.linkedMinerId = tag.hasUUID("LinkedMiner") ? tag.getUUID("LinkedMiner") : null;
        this.targetPoints.clear();
        if (tag.contains("TargetPoints")) {
            for (Tag entry : tag.getList("TargetPoints", Tag.TAG_INT_ARRAY)) {
                final int[] values = ((net.minecraft.nbt.IntArrayTag) entry).getAsIntArray();
                if (values.length == 3) {
                    this.targetPoints.add(new BlockPos(values[0], values[1], values[2]));
                }
            }
        } else {
            this.findTargetPointsFlag = this.master;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean("isMaster", this.master);
        if (this.masterPos != null) {
            tag.put("MasterPos", NbtUtils.writeBlockPos(this.masterPos));
        }
        tag.putInt("facing", this.facing.get2DDataValue());
        if (this.linkedMinerId != null) {
            tag.putUUID("LinkedMiner", this.linkedMinerId);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.master = tag.getBoolean("isMaster");
        this.masterPos = NbtUtils.readBlockPos(tag, "MasterPos").orElse(null);
        this.facing = Direction.from2DDataValue(tag.getInt("facing"));
        this.linkedMinerId = tag.hasUUID("LinkedMiner") ? tag.getUUID("LinkedMiner") : null;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
