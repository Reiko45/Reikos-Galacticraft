/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityBeamReceiver).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.beam.LaserNode;
import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.block.BeamReceiverBlock;
import io.github.reiko45.galacticraft.energy.GCEnergy;
import io.github.reiko45.galacticraft.energy.IGCEnergyHandler;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Energy Beam Receiver (1.12.2 TileEntityBeamReceiver), stuck on the side of a machine. On an energy output (a storage
 * module's output face, a generator) it takes the energy and beams it to its target; on an energy input it takes beamed
 * energy into a buffer as big as the machine's free space (15,000 gJ for others) and feeds it in, up to 7,500 gJ a tick.
 * Its ring spins while it is attached to something it can use; the head shows green (receiving) or blue (sending).
 */
public class BeamReceiverBlockEntity extends BeamOutputBlockEntity {
    public enum Mode {
        UNDEFINED, RECEIVE, EXTRACT
    }

    /** 1.12.2 maxRate: 1,500 gJ a tick from other mods' machines (and the buffer's intake). */
    public static final float MAX_RATE = 1500.0F;

    @Nullable
    private Direction facing;
    private int preLoadFacing = -1;
    private boolean facingInitialised;
    private Mode mode = Mode.UNDEFINED;
    private float stored;
    private float capacity = 10.0F * MAX_RATE;

    public BeamReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.BEAM_RECEIVER.get(), pos, state);
    }

    @Nullable
    public Direction facing() {
        return this.facing;
    }

    public Mode mode() {
        return this.mode;
    }

    public float stored() {
        return this.stored;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide() && !this.facingInitialised) {
            this.facingInitialised = true;
            final Direction initial = this.preLoadFacing >= 0 ? Direction.from3DDataValue(this.preLoadFacing)
                    : getBlockState().hasProperty(BeamReceiverBlock.FACING) ? getBlockState().getValue(BeamReceiverBlock.FACING) : null;
            setFacing(initial);
            this.preLoadFacing = -1;
        }
        super.tick();
        if (this.level.isClientSide()) {
            return;
        }
        final LaserNode target = target();
        if (target != null && this.mode == Mode.EXTRACT && this.facing != null) {
            final Attached attached = attached();
            if (attached != null && attached.handler.canExtract()) {
                float offer = attached.handler.extractGJ(Float.MAX_VALUE / 4.0F, true);
                if (attached.foreign) {
                    offer = Math.min(offer, MAX_RATE);
                }
                if (offer > 0.0F) {
                    final List<LaserNode> path = new ArrayList<>();
                    path.add(this);
                    final float transmitted = target.receiveBeam(path, offer, false);
                    if (transmitted > 0.0F) {
                        attached.handler.extractGJ(transmitted, false);
                    }
                }
            }
        } else if (this.mode == Mode.RECEIVE && this.stored > 0.0F) {
            // One receiver may be fed by several beams: up to five times the rate
            final float maxTransfer = Math.min(this.stored, MAX_RATE * 5.0F);
            if (maxTransfer < 0.01F) {
                this.stored = 0.0F;
            } else {
                final Attached attached = attached();
                if (attached != null) {
                    this.stored -= attached.handler.receiveGJ(maxTransfer, false);
                }
            }
            setChanged();
        }
    }

    private record Attached(IGCEnergyHandler handler, boolean foreign) {
    }

    /** 1.12.2 getAttachedTile: the machine on the facing side (not a wire); the buffer follows its free space. */
    @Nullable
    private Attached attached() {
        if (this.facing == null) {
            return null;
        }
        final BlockPos neighbor = getBlockPos().relative(this.facing);
        if (this.level.getBlockState(neighbor).getBlock() instanceof AluminumWireBlock || this.level.getBlockEntity(neighbor) == null) {
            setFacing(null);
            return null;
        }
        final Attached attached = handlerAt(this.level, neighbor, this.facing.getOpposite());
        if (attached != null && !attached.foreign) {
            this.capacity = Math.max(0.0F, attached.handler.getCapacityGJ() - attached.handler.getStoredGJ());
        }
        return attached;
    }

    @Nullable
    private static Attached handlerAt(Level level, BlockPos pos, Direction side) {
        final IGCEnergyHandler gc = level.getCapability(GCEnergy.BLOCK, pos, side);
        if (gc != null) {
            return new Attached(gc, false);
        }
        final IEnergyStorage fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
        return fe == null ? null : new Attached(GCEnergy.fromFE(fe), true);
    }

    /**
     * 1.12.2 getModeFromDirection / otherModCanReceive / otherModCanProduce: an energy input makes a receiving end, an
     * output a sending one. Null when the block there can't be used (nothing, a wire, a face with no energy).
     */
    @Nullable
    public static Mode modeFor(Level level, BlockPos receiver, Direction toward) {
        final BlockPos neighbor = receiver.relative(toward);
        if (level.getBlockState(neighbor).getBlock() instanceof AluminumWireBlock) {
            return null;
        }
        final Attached attached = handlerAt(level, neighbor, toward.getOpposite());
        if (attached == null) {
            return null;
        }
        return attached.handler.canReceive() ? Mode.RECEIVE : attached.handler.canExtract() ? Mode.EXTRACT : null;
    }

    /** 1.12.2 setFacing: a new side means a new mode. */
    public void setFacing(@Nullable Direction direction) {
        if (direction == this.facing) {
            return;
        }
        if (direction == null) {
            this.mode = Mode.UNDEFINED;
        } else {
            final Mode found = modeFor(this.level, getBlockPos(), direction);
            this.mode = found == null ? Mode.UNDEFINED : found;
        }
        this.facing = direction;
        changed();
    }

    /** The block changed side (1.12.2 BlockBeamReceiver#neighborChanged): attach again and look for nodes anew. */
    public void reattach(Direction direction) {
        setFacing(direction);
        invalidateReflector();
        initiateReflector();
    }

    @Override
    public Vec3 inputPoint() {
        return outputPoint();
    }

    /** 1.12.2: the centre, 0.1 towards the machine. */
    @Override
    public Vec3 outputPoint() {
        final BlockPos pos = getBlockPos();
        Vec3 head = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (this.facing != null) {
            head = head.add(this.facing.getStepX() * 0.1D, this.facing.getStepY() * 0.1D, this.facing.getStepZ() * 0.1D);
        }
        return head;
    }

    @Override
    public int color() {
        return 0x00FF00;
    }

    @Override
    public boolean canConnectTo(LaserNode node) {
        if (this.mode != Mode.UNDEFINED && color() == node.color()) {
            return !(node instanceof BeamReceiverBlockEntity receiver) || receiver.mode != this.mode;
        }
        return false;
    }

    /** Only a sending receiver has a target. */
    @Nullable
    @Override
    public LaserNode target() {
        return this.mode == Mode.EXTRACT ? super.target() : null;
    }

    @Override
    public float receiveBeam(List<LaserNode> path, float amount, boolean simulate) {
        if (this.mode != Mode.RECEIVE) {
            return 0.0F;
        }
        attached();
        if (this.facing == null) {
            return 0.0F;
        }
        final float accepted = Math.max(0.0F, Math.min(Math.min(amount, MAX_RATE), this.capacity - this.stored));
        if (!simulate && accepted > 0.0F) {
            this.stored += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stored = tag.getFloat("Energy");
        if (this.level != null && this.level.isClientSide()) {
            final int side = tag.getInt("FacingSide");
            this.facing = side >= 0 && side < 6 ? Direction.from3DDataValue(side) : null;
            this.mode = Mode.values()[Math.floorMod(tag.getInt("Mode"), Mode.values().length)];
        } else {
            this.preLoadFacing = tag.contains("FacingSide") ? tag.getInt("FacingSide") : -1;
            this.facingInitialised = false;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Energy", this.stored);
        tag.putInt("FacingSide", this.facing == null ? this.preLoadFacing : this.facing.get3DDataValue());
        tag.putInt("Mode", this.mode.ordinal());
    }
}
