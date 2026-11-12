/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.api.power.ILaserNode and EnergySource
 * .EnergySourceWireless). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.beam;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A block that sends or passes on an energy beam (1.12.2 ILaserNode): Beam Reflectors and Beam Receivers. Each points at
 * one target among the nodes it can see; energy travels along the chain of targets.
 */
public interface LaserNode {
    /** Where an incoming beam ends. */
    Vec3 inputPoint();

    /** Where this node's own beam starts. */
    Vec3 outputPoint();

    @Nullable
    LaserNode target();

    BlockEntity blockEntity();

    default BlockPos nodePos() {
        return blockEntity().getBlockPos();
    }

    boolean canConnectTo(LaserNode node);

    /** 0xRRGGBB; 1.12.2's beams were all green. */
    int color();

    void addNode(LaserNode node);

    void removeNode(LaserNode node);

    /**
     * 1.12.2 receiveEnergyGC with an EnergySourceWireless: {@code path} holds the nodes the energy has passed so far, so a
     * loop can't carry it round forever. Returns the gJ taken.
     */
    float receiveBeam(List<LaserNode> path, float amount, boolean simulate);

    /** 1.12.2 compareTo: 1 when this node is nearer {@code origin} than the other, -1 when further, 0 when as near. */
    default int compareTo(LaserNode other, BlockPos origin) {
        final long thisDistance = squared(nodePos(), origin);
        final long otherDistance = squared(other.nodePos(), origin);
        return Long.compare(otherDistance, thisDistance);
    }

    private static long squared(BlockPos a, BlockPos b) {
        final long dx = a.getX() - b.getX();
        final long dy = a.getY() - b.getY();
        final long dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
