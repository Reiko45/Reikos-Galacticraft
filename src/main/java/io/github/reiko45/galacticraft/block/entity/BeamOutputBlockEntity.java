/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityBeamOutput). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.beam.LaserNode;
import java.util.LinkedList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 TileEntityBeamOutput: a beam node that keeps the list of nodes it can reach (those in loaded chunks within 15
 * blocks, nearest first, that both ends accept) and aims at one of them, at first the nearest; using it cycles through
 * the rest. Losing its target, it falls back on its neighbour in the list.
 */
public abstract class BeamOutputBlockEntity extends BlockEntity implements LaserNode {
    private static final BlockPos NO_TARGET = new BlockPos(-1, -1, -1);

    protected final LinkedList<LaserNode> nodes = new LinkedList<>();
    private BlockPos targetPos = NO_TARGET;
    @Nullable
    private BlockPos preLoadTarget;
    /** Client: where the head points (1.12.2 updateOrientation). */
    public float pitch;
    public float yaw;
    /** 1.12.2 TileEntityAdvanced ticks: spins the rings and the beam. */
    public int ticks;

    protected BeamOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (this.preLoadTarget != null && this.level.getBlockEntity(this.preLoadTarget) instanceof LaserNode node) {
            setTarget(node);
            this.preLoadTarget = null;
        }
        this.ticks++;
        if (this.level.isClientSide()) {
            updateOrientation();
        } else if (this.targetPos.equals(NO_TARGET)) {
            initiateReflector();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidateReflector();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        invalidateReflector();
    }

    public void invalidateReflector() {
        for (LaserNode node : this.nodes) {
            node.removeNode(this);
        }
        this.nodes.clear();
    }

    /**
     * 1.12.2 initiateReflector: every node in the loaded chunks within 15 blocks. (1.12.2 only checked that this node was
     * less than 16 blocks past the other on each axis, so nodes a little further in the + directions are found too.)
     */
    public void initiateReflector() {
        this.nodes.clear();
        final BlockPos pos = getBlockPos();
        for (int cx = pos.getX() - 15 >> 4; cx <= pos.getX() + 15 >> 4; cx++) {
            for (int cz = pos.getZ() - 15 >> 4; cz <= pos.getZ() + 15 >> 4; cz++) {
                if (!this.level.hasChunk(cx, cz)) {
                    continue;
                }
                final LevelChunk chunk = this.level.getChunk(cx, cz);
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity != this && entity instanceof LaserNode node && !entity.isRemoved()) {
                        final BlockPos delta = pos.subtract(entity.getBlockPos());
                        if (delta.getX() < 16 && delta.getY() < 16 && delta.getZ() < 16 && canConnectTo(node) && node.canConnectTo(this)) {
                            addNode(node);
                            node.addNode(this);
                        }
                    }
                }
            }
        }
        setTarget(this.nodes.peekFirst());
    }

    private int indexOf(LaserNode node) {
        for (int i = 0; i < this.nodes.size(); i++) {
            if (this.nodes.get(i).nodePos().equals(node.nodePos())) {
                return i;
            }
        }
        return -1;
    }

    /** 1.12.2 addNode: nearer than the first goes first, as far as the last or further goes last (and so does the rest). */
    @Override
    public void addNode(LaserNode node) {
        final int index = indexOf(node);
        if (index != -1) {
            this.nodes.set(index, node);
            return;
        }
        if (this.nodes.isEmpty() || this.nodes.getFirst().compareTo(node, getBlockPos()) <= 0) {
            this.nodes.addFirst(node);
        } else {
            this.nodes.addLast(node);
        }
    }

    @Override
    public void removeNode(LaserNode node) {
        final int index = indexOf(node);
        if (node.nodePos().equals(this.targetPos)) {
            if (index == 0) {
                setTarget(this.nodes.size() > 1 ? this.nodes.get(1) : null);
            } else if (index > 0) {
                setTarget(this.nodes.get(index - 1));
            } else {
                setTarget(null);
            }
        }
        if (index != -1) {
            this.nodes.remove(index);
        }
    }

    /** 1.12.2 updateOrientation: the head turns to face the target's input point. */
    public void updateOrientation() {
        final LaserNode target = target();
        if (target != null) {
            final Vec3 direction = outputPoint().subtract(target.inputPoint()).normalize();
            final double angle = Math.acos(Mth.clamp(-direction.y, -1.0D, 1.0D));
            this.pitch = (float) (-angle * Mth.RAD_TO_DEG + 90.0D);
            this.yaw = (float) (-(Math.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) + 90.0D);
        }
    }

    @Override
    public BlockEntity blockEntity() {
        return this;
    }

    /** 1.12.2 onMachineActivated: aim at the next node in the list. */
    public boolean cycleTarget() {
        if (this.nodes.size() > 1) {
            final LaserNode target = target();
            final int index = target == null ? -1 : indexOf(target);
            if (index == -1) {
                initiateReflector();
            } else {
                setTarget(this.nodes.get((index + 1) % this.nodes.size()));
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public LaserNode target() {
        if (!this.targetPos.equals(NO_TARGET) && this.level != null && this.level.getBlockEntity(this.targetPos) instanceof LaserNode node) {
            return node;
        }
        return null;
    }

    /** The beam's end even when this node isn't currently sending (for rendering and tests). */
    public BlockPos targetPos() {
        return this.targetPos;
    }

    public void setTarget(@Nullable LaserNode target) {
        final BlockPos newTarget = target == null ? NO_TARGET : target.nodePos();
        if (!newTarget.equals(this.targetPos)) {
            this.targetPos = newTarget;
            changed();
        }
    }

    public java.util.List<LaserNode> nodes() {
        return java.util.List.copyOf(this.nodes);
    }

    protected void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.getBoolean("HasTarget")) {
            final BlockPos target = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
            if (this.level != null && this.level.isClientSide()) {
                this.targetPos = target;
            } else {
                this.preLoadTarget = target;
            }
        } else if (this.level != null && this.level.isClientSide()) {
            this.targetPos = NO_TARGET;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final BlockPos target = this.preLoadTarget != null ? this.preLoadTarget : this.targetPos;
        tag.putBoolean("HasTarget", !target.equals(NO_TARGET));
        if (!target.equals(NO_TARGET)) {
            tag.putInt("TargetX", target.getX());
            tag.putInt("TargetY", target.getY());
            tag.putInt("TargetZ", target.getZ());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
