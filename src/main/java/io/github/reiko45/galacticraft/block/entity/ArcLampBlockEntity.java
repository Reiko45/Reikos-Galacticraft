/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityArclamp). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.ArcLampBlock;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Arc Lamp (1.12.2 TileEntityArclamp). While it gets no redstone signal it floods the space in front of it with light:
 * every 100 ticks it spreads out up to 14 layers from the lamp through air and see-through blocks, widening as it goes
 * (a cone two blocks wider each layer, not back past its plate), and every air block that touches something solid in
 * that space becomes Bright Air (or bright breathable air); air it no longer reaches goes dark again. Hostile mobs
 * inside a box 25 blocks out in front (4 behind its aim, 2 behind its plate) are sent running, now and then, to a spot
 * further away. FACING is the plate's side; {@link #facing} (0-3) is which of the four ways along the plate it aims.
 */
public class ArcLampBlockEntity extends BlockEntity {
    private static final int LIGHT_RANGE = 14;

    private int ticks;
    private int sideRear;
    private int facing;
    private int facingSide;
    private final Set<BlockPos> airToRestore = new HashSet<>();
    private boolean isActive;
    @Nullable
    private AABB thisAABB;

    public ArcLampBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.ARC_LAMP.get(), pos, state);
    }

    /** Which way along its plate it aims (0-3; the renderer turns the lamp head to match). */
    public int facing() {
        return this.facing;
    }

    public boolean isLit() {
        return this.isActive;
    }

    public Set<BlockPos> litAir() {
        return this.airToRestore;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcLampBlockEntity lamp) {
        lamp.tick();
    }

    private void tick() {
        boolean initialLight = false;
        if (this.level.hasNeighborSignal(this.worldPosition)) {
            if (this.isActive) {
                this.isActive = false;
                revertAir();
                sync();
            }
        } else if (!this.isActive) {
            this.isActive = true;
            initialLight = true;
            sync();
        }
        if (this.isActive) {
            if (this.thisAABB == null) {
                initialLight = true;
                updateSides();
                this.thisAABB = boxForSideAndFacing();
            }
            if (initialLight || this.ticks % 100 == 0) {
                lightArea();
            }
            if (this.level.random.nextInt(20) == 0) {
                scareMobs();
            }
        }
        this.ticks++;
    }

    /** 1.12.2 update: the plate's side and the direction the light points, from FACING and the wrench setting. */
    private void updateSides() {
        final BlockState state = getBlockState();
        final int side = state.hasProperty(ArcLampBlock.FACING) ? state.getValue(ArcLampBlock.FACING).get3DDataValue() : 0;
        this.sideRear = side;
        switch (side) {
            case 0, 1 -> this.facingSide = this.facing + 2;
            case 2 -> this.facingSide = this.facing > 1 ? 7 - this.facing : this.facing;
            case 3 -> this.facingSide = this.facing > 1 ? this.facing + 2 : this.facing;
            case 4 -> this.facingSide = this.facing;
            case 5 -> this.facingSide = this.facing > 1 ? 5 - this.facing : this.facing;
            default -> {
            }
        }
    }

    private AABB boxForSideAndFacing() {
        final int[] range = new int[6];
        for (int i = 0; i < 6; i++) {
            range[i] = i == this.sideRear ? 2 : i == (this.facingSide ^ 1) ? 4 : 25;
        }
        final int x = this.worldPosition.getX();
        final int y = this.worldPosition.getY();
        final int z = this.worldPosition.getZ();
        return new AABB(x - range[4], y - range[0], z - range[2], x + range[5], y + range[1], z + range[3]);
    }

    /** 1.12.2: send mobs in front of the lamp off to a random spot further away. */
    private void scareMobs() {
        final Vec3 lampPos = Vec3.atLowerCornerOf(this.worldPosition);
        final Vec3 centre = Vec3.atCenterOf(this.worldPosition);
        for (PathfinderMob mob : this.level.getEntitiesOfClass(PathfinderMob.class, this.thisAABB, mob -> mob instanceof Enemy)) {
            final PathNavigation nav = mob.getNavigation();
            final Vec3 target = DefaultRandomPos.getPosAway(mob, 28, 11, centre);
            if (target == null) {
                continue;
            }
            final double distanceNew = target.distanceToSqr(lampPos);
            final double distanceCurrent = lampPos.distanceToSqr(mob.position());
            if (distanceNew > distanceCurrent) {
                Vec3 oldTarget = null;
                final Path path = nav.getPath();
                if (path != null && !path.isDone()) {
                    oldTarget = path.getNextEntityPos(mob);
                }
                if (oldTarget == null || distanceCurrent > oldTarget.distanceToSqr(lampPos)) {
                    nav.moveTo(target.x, target.y, target.z, 1.3D);
                }
            }
        }
    }

    @Nullable
    private BlockState stateIfLoaded(BlockPos pos) {
        return this.level.isLoaded(pos) && this.level.isInWorldBounds(pos) ? this.level.getBlockState(pos) : null;
    }

    private int opacity(BlockState state, BlockPos pos) {
        return state.getLightBlock(this.level, pos);
    }

    private static int axisOffset(int side, BlockPos from, BlockPos to) {
        return switch (side) {
            case 0 -> from.getY() - to.getY();
            case 1 -> to.getY() - from.getY();
            case 2 -> from.getZ() - to.getZ();
            case 3 -> to.getZ() - from.getZ();
            case 4 -> from.getX() - to.getX();
            default -> to.getX() - from.getX();
        };
    }

    /** 1.12.2 lightArea. */
    public void lightArea() {
        if (this.thisAABB == null) {
            updateSides();
            this.thisAABB = boxForSideAndFacing();
        }
        final Set<BlockPos> checked = new HashSet<>();
        final Set<BlockPos> airToRevert = new HashSet<>(this.airToRestore);
        final List<BlockPos> airNew = new ArrayList<>();
        List<BlockPos> currentLayer = new ArrayList<>();
        List<BlockPos> nextLayer = new ArrayList<>();
        final BlockPos here = this.worldPosition;
        currentLayer.add(here);
        final int sideskip1 = this.sideRear;
        final int sideskip2 = this.facingSide ^ 1;
        for (int i = 0; i < 6; i++) {
            if (i != sideskip1 && i != sideskip2 && i != (sideskip1 ^ 1) && i != (sideskip2 ^ 1)) {
                final BlockPos onEitherSide = here.relative(Direction.from3DDataValue(i));
                final BlockState state = stateIfLoaded(onEitherSide);
                if (state != null && opacity(state, onEitherSide) < 15) {
                    currentLayer.add(onEitherSide);
                }
            }
        }
        BlockPos inFront = here;
        for (int i = 0; i < 4; i++) {
            inFront = inFront.relative(Direction.from3DDataValue(this.facingSide));
            BlockState state = stateIfLoaded(inFront);
            if (state != null && opacity(state, inFront) == 15) {
                break;
            }
            inFront = inFront.relative(Direction.from3DDataValue(sideskip1 ^ 1));
            state = stateIfLoaded(inFront);
            if (state != null && opacity(state, inFront) < 15) {
                currentLayer.add(inFront);
            } else {
                break;
            }
        }
        inFront = here.relative(Direction.from3DDataValue(this.facingSide));
        for (int count = 0; count < LIGHT_RANGE; count++) {
            for (BlockPos vec : currentLayer) {
                if (count > 1) {
                    int offset = axisOffset(this.facingSide, inFront, vec);
                    final int offset2 = axisOffset(this.sideRear ^ 1, inFront, vec);
                    if (offset2 - 2 > offset) {
                        offset = offset2 - 2;
                    }
                    if (Math.abs(vec.getX() - inFront.getX()) > offset + 2 || Math.abs(vec.getY() - inFront.getY()) > offset + 2
                            || Math.abs(vec.getZ() - inFront.getZ()) > offset + 2) {
                        continue;
                    }
                }
                boolean doShine = false;
                for (int side = 0; side < 6; side++) {
                    final BlockPos sideVec = vec.relative(Direction.from3DDataValue(side));
                    final boolean toAdd = checked.add(sideVec);
                    final BlockState bs = stateIfLoaded(sideVec);
                    if (bs == null) {
                        continue;
                    }
                    if (bs.isAir()) {
                        if (toAdd && side != sideskip1 && side != sideskip2) {
                            nextLayer.add(sideVec);
                        }
                    } else {
                        doShine = true;
                        if (side != sideskip1 && side != sideskip2 && toAdd && opacity(bs, sideVec) == 0) {
                            nextLayer.add(sideVec);
                        }
                    }
                }
                if (doShine) {
                    airNew.add(vec);
                    final BlockState at = this.level.getBlockState(vec);
                    if (at.is(Blocks.AIR)) {
                        brightenAir(vec, GCBlocks.BRIGHT_AIR.get().defaultBlockState());
                    } else if (at.is(GCBlocks.BREATHABLE_AIR.get())) {
                        brightenAir(vec, GCBlocks.BRIGHT_BREATHABLE_AIR.get().defaultBlockState());
                    }
                }
            }
            if (nextLayer.isEmpty()) {
                break;
            }
            currentLayer = nextLayer;
            nextLayer = new ArrayList<>();
        }
        airToRevert.removeAll(airNew);
        for (BlockPos vec : airToRevert) {
            setDarkerAir(vec);
            this.airToRestore.remove(vec);
        }
        setChanged();
    }

    private void brightenAir(BlockPos pos, BlockState state) {
        this.level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        this.airToRestore.add(pos.immutable());
    }

    private void setDarkerAir(BlockPos pos) {
        final BlockState state = this.level.getBlockState(pos);
        if (state.is(GCBlocks.BRIGHT_AIR.get())) {
            this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (state.is(GCBlocks.BRIGHT_BREATHABLE_AIR.get())) {
            this.level.setBlock(pos, GCBlocks.BREATHABLE_AIR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** 1.12.2 revertAir: everything it lit goes dark. */
    public void revertAir() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        for (BlockPos pos : this.airToRestore) {
            setDarkerAir(pos);
        }
        this.airToRestore.clear();
        setChanged();
    }

    /** 1.12.2 facingChanged (the wrench): the next of the four directions along its plate. */
    public void facingChanged() {
        this.facing -= 2;
        if (this.facing < 0) {
            this.facing = 1 - this.facing;
        }
        this.thisAABB = null;
        revertAir();
        this.ticks = 91;
        sync();
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.facing = tag.getInt("Facing");
        this.isActive = tag.getBoolean("Active");
        this.airToRestore.clear();
        final ListTag air = tag.getList("AirBlocks", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < air.size(); i++) {
            final int[] xyz = air.getIntArray(i);
            if (xyz.length == 3) {
                this.airToRestore.add(new BlockPos(xyz[0], xyz[1], xyz[2]));
            }
        }
        this.thisAABB = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Facing", this.facing);
        tag.putBoolean("Active", this.isActive);
        final ListTag air = new ListTag();
        for (BlockPos pos : this.airToRestore) {
            air.add(new net.minecraft.nbt.IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("AirBlocks", air);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Facing", this.facing);
        tag.putBoolean("Active", this.isActive);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.facing = tag.getInt("Facing");
        this.isActive = tag.getBoolean("Active");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    /** Draws the lamp head out to the next block around (the model reaches over the plate's edges). */
    public AABB renderBounds() {
        return new AABB(this.worldPosition).inflate(0.5D);
    }
}
