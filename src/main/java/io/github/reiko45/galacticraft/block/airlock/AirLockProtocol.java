/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.AirLockProtocol). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.airlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 1.12.2 AirLockProtocol: from the controller, follows touching air lock blocks (frames and controllers) up to 26 steps,
 * staying in the controller's own row or column (or, horizontally, its layer), and checks they make a closed rectangle:
 * a vertical one at least 3 x 3 (or a flat one, horizontally) with no more than 24 blocks across its sides together, and
 * the same number of frames along opposite edges. The result is the number of frame blocks, or -1 if it isn't a frame.
 */
final class AirLockProtocol {
    private static final int MAX_LOOPS = 26;

    private final Level level;
    private final BlockPos head;
    private List<BlockPos> adjacent = new ArrayList<>();
    private final Set<BlockPos> checked = new HashSet<>();
    private int dimension1Min;
    private int dimension1Max;
    private int dimension2Min;
    private int dimension2Max;
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxZ = Integer.MIN_VALUE;

    AirLockProtocol(Level level, BlockPos head) {
        this.level = level;
        this.head = head;
    }

    private boolean isAirLock(BlockPos pos) {
        // never load a chunk to look (1.12.2 did; a controller at the edge of the loaded area would keep loading them)
        return this.level.isLoaded(pos) && AirLockFrameBlock.isAirLock(this.level.getBlockState(pos));
    }

    /** Found a frame whose whole box (and a block around it) is loaded. */
    boolean loaded() {
        return this.minX <= this.maxX && this.minY <= this.maxY && this.minZ <= this.maxZ
                && this.level.hasChunksAt(this.minX - 1, this.minY - 1, this.minZ - 1, this.maxX + 1, this.maxY + 1, this.maxZ + 1);
    }

    private void loopThrough(BlockPos pos, int loops) {
        final int xAligned = this.head.getX();
        final int zAligned = this.head.getZ();
        for (int x = -1; x <= 1; x++) {
            final int xTest = pos.getX() + x;
            for (int z = -1; z <= 1; z++) {
                final int zTest = pos.getZ() + z;
                if (xTest == xAligned || zTest == zAligned) {
                    for (int y = -1; y <= 1; y++) {
                        if (!(x == 0 && y == 0 && z == 0)) {
                            final BlockPos test = new BlockPos(xTest, pos.getY() + y, zTest);
                            if (this.checked.add(test) && isAirLock(test)) {
                                this.adjacent.add(test);
                                if (loops > 1) {
                                    loopThrough(test, loops - 1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void loopThroughHorizontal(BlockPos pos, int loops) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!(x == 0 && z == 0)) {
                    final BlockPos test = new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + z);
                    if (this.checked.add(test) && isAirLock(test)) {
                        this.adjacent.add(test);
                        if (loops > 1) {
                            loopThroughHorizontal(test, loops - 1);
                        }
                    }
                }
            }
        }
    }

    /** 1.12.2 calculate (its +-6000000 box start is widened: past that, the box came out wrong). */
    int calculate(boolean horizontal) {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.minZ = Integer.MAX_VALUE;
        this.maxZ = Integer.MIN_VALUE;
        this.adjacent = new ArrayList<>();
        this.checked.clear();
        this.checked.add(this.head);
        this.adjacent.add(this.head);
        if (horizontal) {
            loopThroughHorizontal(this.head, MAX_LOOPS);
        } else {
            loopThrough(this.head, MAX_LOOPS);
        }
        for (BlockPos airLock : this.adjacent) {
            this.minX = Math.min(this.minX, airLock.getX());
            this.maxX = Math.max(this.maxX, airLock.getX());
            this.minY = Math.min(this.minY, airLock.getY());
            this.maxY = Math.max(this.maxY, airLock.getY());
            this.minZ = Math.min(this.minZ, airLock.getZ());
            this.maxZ = Math.max(this.maxZ, airLock.getZ());
        }
        final int count = this.maxX - this.minX + this.maxZ - this.minZ + this.maxY - this.minY;
        if (count > 24 || this.maxX - this.minX <= 1 && this.maxZ - this.minZ <= 1 || !horizontal && this.maxY - this.minY <= 1) {
            return -1;
        }
        if (horizontal && (this.maxX - this.minX <= 1 || this.maxZ - this.minZ <= 1)) {
            return -1;
        }
        this.dimension1Min = 0;
        this.dimension1Max = 0;
        this.dimension2Min = 0;
        this.dimension2Max = 0;
        if (horizontal) {
            checkDimensionsHorizontal();
        } else {
            checkDimensions();
        }
        if (this.dimension2Max == 0 || this.dimension2Min == 0 || this.dimension1Min == 0 || this.dimension1Max == 0
                || this.dimension2Max != this.dimension2Min || this.dimension1Max != this.dimension1Min) {
            return -1;
        }
        return this.adjacent.size();
    }

    private void checkDimensions() {
        for (int y = this.minY; y <= this.maxY; y++) {
            if (isAirLock(new BlockPos(this.minX, y, this.minZ))) {
                this.dimension1Min++;
            }
            if (isAirLock(new BlockPos(this.maxX, y, this.maxZ))) {
                this.dimension1Max++;
            }
        }
        if (this.minX != this.maxX) {
            for (int x = this.minX; x <= this.maxX; x++) {
                if (isAirLock(new BlockPos(x, this.maxY, this.maxZ))) {
                    this.dimension2Max++;
                }
                if (isAirLock(new BlockPos(x, this.minY, this.maxZ))) {
                    this.dimension2Min++;
                }
            }
        } else if (this.minZ != this.maxZ) {
            for (int z = this.minZ; z <= this.maxZ; z++) {
                if (isAirLock(new BlockPos(this.maxX, this.maxY, z))) {
                    this.dimension2Max++;
                }
                if (isAirLock(new BlockPos(this.maxX, this.minY, z))) {
                    this.dimension2Min++;
                }
            }
        }
    }

    private void checkDimensionsHorizontal() {
        if (this.minX != this.maxX) {
            for (int x = this.minX; x <= this.maxX; x++) {
                if (isAirLock(new BlockPos(x, this.minY, this.maxZ))) {
                    this.dimension1Max++;
                }
                if (isAirLock(new BlockPos(x, this.minY, this.minZ))) {
                    this.dimension1Min++;
                }
            }
        }
        if (this.minZ != this.maxZ) {
            for (int z = this.minZ; z <= this.maxZ; z++) {
                if (isAirLock(new BlockPos(this.maxX, this.minY, z))) {
                    this.dimension2Max++;
                }
                if (isAirLock(new BlockPos(this.minX, this.minY, z))) {
                    this.dimension2Min++;
                }
            }
        }
    }
}
