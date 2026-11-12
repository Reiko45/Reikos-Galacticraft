/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base.Piece and
 * SizedPiece). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.base;

import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.block.FluidTankBlock;
import io.github.reiko45.galacticraft.block.WalkwayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * A piece of an abandoned base (1.12.2 Piece + SizedPiece): the base's configuration, the direction it was built in and
 * its size. Coordinates inside a piece follow 1.12.2 SizedPiece, which (unlike vanilla) counts z from the north side
 * when the piece faces north and from the south side when it faces south; block states are mirrored and rotated by the
 * orientation, as 1.12.2 setBlockState did.
 */
public abstract class BasePiece extends StructurePiece {
    protected final BaseConfiguration configuration;
    protected Direction direction;
    protected int sizeX;
    protected int sizeY;
    protected int sizeZ;

    protected BasePiece(StructurePieceType type, BaseConfiguration configuration, int sizeX, int sizeY, int sizeZ, Direction direction) {
        super(type, 0, new BoundingBox(0, 0, 0, 0, 0, 0));
        this.configuration = configuration;
        this.direction = direction;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    protected BasePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.configuration = BaseConfiguration.load(tag);
        this.sizeX = tag.getInt("sX");
        this.sizeY = tag.getInt("sY");
        this.sizeZ = tag.getInt("sZ");
        this.direction = tag.contains("dir") ? Direction.from3DDataValue(tag.getInt("dir")) : Direction.NORTH;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        this.configuration.save(tag);
        tag.putInt("dir", this.direction.get3DDataValue());
        tag.putInt("sX", this.sizeX);
        tag.putInt("sY", this.sizeY);
        tag.putInt("sZ", this.sizeZ);
    }

    public Direction direction() {
        return this.direction;
    }

    public BaseConfiguration configuration() {
        return this.configuration;
    }

    protected void setBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** 1.12.2 SizedPiece#getXWithOffset. */
    @Override
    protected int getWorldX(int x, int z) {
        final Direction orientation = getOrientation();
        if (orientation == null) {
            return x;
        }
        return switch (orientation) {
            case NORTH -> this.boundingBox.minX() + x;
            case SOUTH -> this.boundingBox.maxX() - x;
            case WEST -> this.boundingBox.maxX() - z;
            case EAST -> this.boundingBox.minX() + z;
            default -> x;
        };
    }

    /** 1.12.2 SizedPiece#getZWithOffset. */
    @Override
    protected int getWorldZ(int x, int z) {
        final Direction orientation = getOrientation();
        if (orientation == null) {
            return z;
        }
        return switch (orientation) {
            case NORTH -> this.boundingBox.minZ() + z;
            case SOUTH -> this.boundingBox.maxZ() - z;
            case WEST -> this.boundingBox.minZ() + x;
            case EAST -> this.boundingBox.maxZ() - x;
            default -> z;
        };
    }

    protected BlockPos worldPos(int x, int y, int z) {
        return new BlockPos(getWorldX(x, z), getWorldY(y), getWorldZ(x, z));
    }

    /** Where a position inside this piece is in the world (with the piece's current orientation). */
    public BlockPos localToWorld(int x, int y, int z) {
        return worldPos(x, y, z);
    }

    /**
     * 1.12.2 setBlockState: mirrored and rotated by the orientation, inside this chunk only. 1.12.2 worked out wall,
     * stair, bar, walkway and wire connections as each block was drawn; here those blocks are queued to settle against
     * their neighbours when the chunk is finished.
     */
    protected void place(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        final BlockPos pos = worldPos(x, y, z);
        if (box.isInside(pos)) {
            placeBlock(level, state, x, y, z, box);
            settle(level, pos, state);
        }
    }

    /** A block placed as it is (1.12.2 world.setBlockState from room contents: no mirror or rotation). */
    protected static void setRaw(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        settle(level, pos, state);
    }

    private static void settle(WorldGenLevel level, BlockPos pos, BlockState state) {
        final Block block = state.getBlock();
        if (block instanceof StairBlock || block instanceof WallBlock || block instanceof IronBarsBlock || block instanceof WalkwayBlock
                || block instanceof AluminumWireBlock || block instanceof FluidTankBlock) {
            level.getChunk(pos).markPosForPostprocessing(pos);
        }
    }
}
