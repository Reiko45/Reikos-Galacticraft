/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityBeamReflector).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.beam.LaserNode;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Energy Beam Reflector (1.12.2 TileEntityBeamReflector): passes a beam on to its own target. With no target it soaks up
 * the beam into a 10 gJ store, 1 gJ at a time, as 1.12.2 did.
 */
public class BeamReflectorBlockEntity extends BeamOutputBlockEntity {
    /** 1.12.2: the head's centre, 1.13228 model units up at half scale. */
    private static final double HEAD_Y = 1.13228D / 2.0D;

    private float stored;

    public BeamReflectorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.BEAM_REFLECTOR.get(), pos, state);
    }

    /** 1.12.2 getInputPoint: 0.15 behind the head, opposite the way it points. */
    @Override
    public Vec3 inputPoint() {
        final double radians = Math.toRadians(this.yaw - 180.0F);
        final BlockPos pos = getBlockPos();
        return new Vec3(pos.getX() + 0.5D - Math.sin(radians) * 0.15D, pos.getY() + HEAD_Y, pos.getZ() + 0.5D - Math.cos(radians) * 0.15D);
    }

    @Override
    public Vec3 outputPoint() {
        final BlockPos pos = getBlockPos();
        return new Vec3(pos.getX() + 0.5D, pos.getY() + HEAD_Y, pos.getZ() + 0.5D);
    }

    @Override
    public int color() {
        return 0x00FF00;
    }

    @Override
    public boolean canConnectTo(LaserNode node) {
        return color() == node.color();
    }

    @Override
    public float receiveBeam(List<LaserNode> path, float amount, boolean simulate) {
        final LaserNode target = target();
        if (target != null) {
            if (path.contains(target)) {
                return 0.0F;
            }
            path.add(this);
            return target.receiveBeam(path, amount, simulate);
        }
        final float accepted = Math.min(Math.min(amount, 1.0F), 10.0F - this.stored);
        if (!simulate && accepted > 0.0F) {
            this.stored += accepted;
            setChanged();
        }
        return Math.max(0.0F, accepted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stored = tag.getFloat("Energy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Energy", this.stored);
    }
}
