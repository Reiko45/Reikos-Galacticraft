/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityPlatform). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.HydraulicPlatformBlock;
import io.github.reiko45.galacticraft.block.HydraulicPlatformBlock.Corner;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.12.2 TileEntityPlatform: an unjoined platform looks for a whole 2 x 2 of unjoined ones (none with a platform just
 * above or below) to join, and a joined one checks its square is still whole when loaded. The north-west corner of a
 * platform runs the lift on the client ({@code io.github.reiko45.galacticraft.client.PlatformLift}); while a lift
 * passes through a platform, the blocks of that platform don't collide (on both sides).
 */
public class HydraulicPlatformBlockEntity extends BlockEntity {
    /** 1.12.2 MAXRANGE. */
    public static final int RANGE = 16;
    /** 0 unjoined, 1 north-west, 2 south-west, 3 north-east, 4 south-east. */
    private int corner;
    private boolean firstTickCheck;
    private boolean noCollide;
    /** Client: the lift is moving through or from this platform. */
    private boolean moving;
    /** Client: someone stands on it (1.12.2 lightOn), and its light's colour, 1 red for a moment after a refusal. */
    private boolean lightOn;
    private int colorState;
    private int colorTicks;

    public HydraulicPlatformBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.HYDRAULIC_PLATFORM.get(), pos, state);
    }

    public void tick() {
        if (this.level.isClientSide) {
            if (this.colorTicks > 0 && --this.colorTicks == 0) {
                this.colorState = 0;
            }
            this.lightOn = false;
            if (getBlockState().getValue(HydraulicPlatformBlock.TYPE) == Corner.NW) {
                io.github.reiko45.galacticraft.client.PlatformLift.check(this);
            }
            return;
        }
        if (this.firstTickCheck) {
            this.firstTickCheck = !checkIntact();
        }
        if (this.corner == 0) {
            // 1.12.2: this block as the south-east of a square of four unjoined platforms with nothing above or below
            final List<HydraulicPlatformBlockEntity> square = new ArrayList<>();
            for (int x = -1; x < 1; x++) {
                for (int z = -1; z < 1; z++) {
                    final BlockPos pos = this.worldPosition.offset(x, 0, z);
                    if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof HydraulicPlatformBlockEntity platform && !platform.isRemoved()
                            && platform.corner == 0 && !(this.level.getBlockEntity(pos.above()) instanceof HydraulicPlatformBlockEntity)
                            && !(this.level.getBlockEntity(pos.below()) instanceof HydraulicPlatformBlockEntity)) {
                        square.add(platform);
                    }
                }
            }
            if (square.size() == 4) {
                int index = 1;
                for (HydraulicPlatformBlockEntity platform : square) {
                    platform.setWhole(index++);
                }
            }
        }
    }

    private void setWhole(int index) {
        this.corner = index;
        setChanged();
        this.level.setBlock(this.worldPosition, getBlockState().setValue(HydraulicPlatformBlock.TYPE, Corner.byIndex(index)), Block.UPDATE_ALL);
    }

    public int corner() {
        return this.corner;
    }

    /** 1.12.2 onDestroy: the rest of a broken platform goes back to unjoined blocks. */
    public void onBroken() {
        if (this.corner > 0) {
            for (BlockPos pos : partners()) {
                if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof HydraulicPlatformBlockEntity platform) {
                    platform.setWhole(0);
                }
            }
        }
    }

    /** 1.12.2 getPositions: the other three blocks of the platform. */
    private List<BlockPos> partners() {
        final int dx = this.corner == 1 || this.corner == 2 ? 1 : -1;
        final int dz = this.corner == 1 || this.corner == 3 ? 1 : -1;
        return this.corner == 0 ? List.of() : List.of(this.worldPosition.offset(dx, 0, 0), this.worldPosition.offset(0, 0, dz),
                this.worldPosition.offset(dx, 0, dz));
    }

    /** 1.12.2 checkIntact: on loading a joined block, see that its square still is one. */
    private boolean checkIntact() {
        if (getBlockState().getValue(HydraulicPlatformBlock.TYPE).ordinal() != this.corner) {
            onBroken();
            return false;
        }
        final int[][] expected = switch (this.corner) {
            case 1 -> new int[][] {{1, 0, 3}, {0, 1, 2}, {1, 1, 4}};
            case 2 -> new int[][] {{1, 0, 4}, {0, -1, 1}, {1, -1, 3}};
            case 3 -> new int[][] {{-1, 0, 1}, {0, 1, 4}, {-1, 1, 2}};
            case 4 -> new int[][] {{-1, 0, 2}, {0, -1, 3}, {-1, -1, 1}};
            default -> new int[0][];
        };
        if (expected.length == 0) {
            return true;
        }
        int count = 0;
        for (int[] part : expected) {
            final BlockPos pos = this.worldPosition.offset(part[0], 0, part[1]);
            if (!this.level.isLoaded(pos)) {
                count += 4;
            } else if (this.level.getBlockState(pos).getBlock() instanceof HydraulicPlatformBlock
                    && this.level.getBlockState(pos).getValue(HydraulicPlatformBlock.TYPE).ordinal() == part[2]
                    && this.level.getBlockEntity(pos) instanceof HydraulicPlatformBlockEntity platform) {
                platform.corner = part[2];
                platform.firstTickCheck = false;
                count++;
            }
        }
        if (count >= 3) {
            return count == 3;
        }
        onBroken();
        return true;
    }

    // ------------------------------------------------------------------ the lift

    public boolean noCollide() {
        return this.noCollide;
    }

    /** 1.12.2 markNoCollide: the whole square (this the north-west corner) lets the lift through, or not. */
    public void markNoCollide(boolean noCollide) {
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                if (this.level.getBlockEntity(this.worldPosition.offset(x, 0, z)) instanceof HydraulicPlatformBlockEntity platform) {
                    platform.noCollide = noCollide;
                }
            }
        }
    }

    public boolean isMoving() {
        return this.moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public boolean lightOn() {
        return this.lightOn;
    }

    public void setLightOn() {
        this.lightOn = true;
    }

    /** 0 green, 1 red. */
    public int lightColor() {
        return this.colorState;
    }

    /** 1.12.2: the light shows red for 16 ticks when the way is blocked. */
    public void refuse() {
        this.colorState = 1;
        this.colorTicks = 16;
    }

    /** 1.12.2 checkCorner: 0 air or other, 1 the expected corner with room above, 2 one without room, 3 a solid block. */
    private int checkCorner(BlockPos pos, Corner corner) {
        final BlockState state = this.level.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        if (state.getBlock() instanceof HydraulicPlatformBlock && state.getValue(HydraulicPlatformBlock.TYPE) == corner) {
            return this.level.getBlockState(pos.above()).isSuffocating(this.level, pos.above())
                    || this.level.getBlockState(pos.above(2)).isSuffocating(this.level, pos.above(2)) ? 2 : 1;
        }
        return state.isSuffocating(this.level, pos) || state.isCollisionShapeFullBlock(this.level, pos) ? 3 : 0;
    }

    /**
     * 1.12.2 checkNextPlatform: the next whole platform up (1) or down (-1) within 16 blocks, as a distance; -1 if the way
     * there is blocked (or it has no head room), 0 if there is none.
     */
    public int nextPlatform(int dir, AABB playerBox) {
        final int x = this.worldPosition.getX();
        final int y0 = this.worldPosition.getY();
        final int z = this.worldPosition.getZ();
        final int limit = Math.max(this.level.getMinBuildHeight(), Math.min(this.level.getMaxBuildHeight() - 1, y0 + RANGE * dir));
        for (int y = y0 + dir; y != limit; y += dir) {
            int c = checkCorner(new BlockPos(x, y, z), Corner.NW);
            if (c >= 2) {
                return c - 3;
            }
            c += checkCorner(new BlockPos(x + 1, y, z), Corner.NE) * 4;
            if (c >= 8) {
                return c - 3;
            }
            c += checkCorner(new BlockPos(x, y, z + 1), Corner.SW) * 16;
            if (c >= 32) {
                return c - 3;
            }
            c += checkCorner(new BlockPos(x + 1, y, z + 1), Corner.SE) * 64;
            if (c >= 128) {
                return c - 3;
            }
            if (c == 0) {
                continue;
            }
            if (motionObstructed(y0 + 1, y - y0, playerBox)) {
                return -1;
            }
            return (y - y0) * dir;
        }
        return 0;
    }

    /** 1.12.2 motionObstructed: a player-sized column over the middle, from the platform to the next, is clear. */
    private boolean motionObstructed(double y, double velocityY, AABB playerBox) {
        final double cx = this.worldPosition.getX() + 1;
        final double cz = this.worldPosition.getZ() + 1;
        final double size = 9 / 16D;
        final double height = playerBox.getYsize() + velocityY;
        final double depth = velocityY < 0 ? 0.179D : 0.0D;
        final AABB box = new AABB(cx - size, Math.min(y - depth, y + height), cz - size, cx + size, Math.max(y - depth, y + height), cz + size);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            final BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() instanceof HydraulicPlatformBlock) {
                continue; // 1.12.2 ignoreCollisionTests
            }
            if (state.getCollisionShape(this.level, pos).toAabbs().stream().anyMatch(shape -> shape.move(pos).intersects(box))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("co", this.corner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.corner = tag.getInt("co");
        this.firstTickCheck = this.corner != 0;
    }

    public AABB renderBox() {
        return new AABB(this.worldPosition.offset(-1, -18, -1).getCenter(), this.worldPosition.offset(2, 18, 2).getCenter()).inflate(0.5D);
    }
}
