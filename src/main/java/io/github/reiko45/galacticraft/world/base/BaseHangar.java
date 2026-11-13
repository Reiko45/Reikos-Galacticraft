/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base.BaseHangar).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.base;

import io.github.reiko45.galacticraft.registry.GCStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The hangar of a hangar base (26 x 15 x 42), open to space at its far end: tin walls, walkway gantries, a lit ceiling
 * of desh and tin beams, and a far third that breaks off raggedly where the hangar was never finished (or was torn
 * away).
 */
public class BaseHangar extends BasePiece {
    public static final int HANGARWIDTH = 26;
    public static final int HANGARLENGTH = 42;
    public static final int HANGARHEIGHT = 15;

    public BaseHangar(BaseConfiguration configuration, int blockPosX, int blockPosZ, Direction direction) {
        super(GCStructures.BASE_HANGAR.get(), configuration, HANGARWIDTH, HANGARHEIGHT, HANGARLENGTH, direction);
        if (direction.getAxis() == Direction.Axis.X) {
            final int w = this.sizeX;
            this.sizeX = this.sizeZ;
            this.sizeZ = w;
            setOrientation(direction.getOpposite()); // 1.12.2: "Maybe a bug in vanilla here?"
        } else {
            setOrientation(direction);
        }
        final int yPos = configuration.yPosition();
        setBox(blockPosX, yPos, blockPosZ, blockPosX + this.sizeX, yPos + this.sizeY, blockPosZ + this.sizeZ);
    }

    public BaseHangar(StructurePieceSerializationContext context, CompoundTag tag) {
        super(GCStructures.BASE_HANGAR.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        final BlockState blockAir = Blocks.AIR.defaultBlockState();
        final BlockState blockPlain = BaseBlocks.plain();
        final BlockState blockPattern = BaseBlocks.pattern();
        final BlockState blockGrid = BaseBlocks.grid();
        final BlockState blockSlab = BaseBlocks.tinSlab(false);
        final BlockState upsideSlab = BaseBlocks.tinSlab(true);
        final BlockState blockWall = BaseBlocks.wall(1);
        final BlockState decoWall = BaseBlocks.wall(0);
        final BlockState moonWall = BaseBlocks.wall(2);
        final BlockState blockBars = Blocks.IRON_BARS.defaultBlockState();
        final BlockState blockDesh = BaseBlocks.desh();
        final BlockState blockRedstone = BaseBlocks.concealedRedstone();
        final BlockState wallBlock = this.configuration.wallBlock();
        final Block blockStair = BaseBlocks.tinStairs();
        int lampmeta2 = 5;
        int stairmeta = 1;
        int stairmetaB = 2;
        if (this.direction == Direction.SOUTH) {
            stairmeta ^= 1;
            stairmetaB ^= 1;
            lampmeta2 ^= 1;
        } else if (this.direction == Direction.EAST) {
            stairmeta ^= 2;
            stairmetaB ^= 3;
            lampmeta2 = 3;
        } else if (this.direction == Direction.WEST) {
            stairmeta ^= 3;
            stairmetaB ^= 2;
            lampmeta2 = 2;
        }

        final int maxX = HANGARWIDTH;
        final int maxY = this.sizeY;
        final int midPoint = HANGARLENGTH - 22;

        // AIR
        for (int zz = HANGARLENGTH; zz >= 0; zz--) {
            for (int xx = 0; xx <= maxX; xx++) {
                for (int y = 0; y <= maxY; y++) {
                    if (y <= 1 && (xx == 0 || xx == maxX)) {
                        continue;
                    }
                    if (y == maxY && (xx < 4 || xx > maxX - 4)) {
                        continue;
                    }
                    if (y == maxY - 1 && (xx < 2 || xx > maxX - 2)) {
                        continue;
                    }
                    place(level, blockAir, xx, y, zz, box);
                }
            }
        }

        // End wall
        for (int y = 7; y <= 8; y++) {
            place(level, blockWall, 1, y, HANGARLENGTH, box);
            place(level, blockWall, 2, y, HANGARLENGTH, box);
            place(level, blockWall, 3, y, HANGARLENGTH, box);
            place(level, blockWall, 23, y, HANGARLENGTH, box);
            place(level, blockWall, 24, y, HANGARLENGTH, box);
            place(level, blockWall, 25, y, HANGARLENGTH, box);
            for (int x = 5; x < 22; x++) {
                place(level, wallBlock, x, y, HANGARLENGTH, box);
                if (y == 7 && (x < 9 || x > 17)) {
                    place(level, wallBlock, x, maxY, HANGARLENGTH, box);
                }
            }
        }
        for (int y = 7; y <= maxY; y++) {
            place(level, blockPlain, 4, y, HANGARLENGTH, box);
            place(level, blockPlain, 22, y, HANGARLENGTH, box);
        }
        for (int y = 9; y <= maxY; y++) {
            place(level, wallBlock, 6, y, HANGARLENGTH, box);
            place(level, wallBlock, 20, y, HANGARLENGTH, box);
        }
        for (int xx = 0; xx <= 3; xx++) {
            place(level, moonWall, xx, 1, HANGARLENGTH, box);
        }
        for (int xx = maxX - 3; xx <= maxX; xx++) {
            place(level, moonWall, xx, 1, HANGARLENGTH, box);
        }

        place(level, blockDesh, 9, maxY, HANGARLENGTH - 1, box);
        place(level, blockRedstone, 11, maxY, HANGARLENGTH - 1, box);
        place(level, blockPattern, 13, maxY, HANGARLENGTH - 1, box);
        place(level, blockRedstone, 15, maxY, HANGARLENGTH - 1, box);
        place(level, blockDesh, 17, maxY, HANGARLENGTH - 1, box);
        place(level, upsideSlab, 10, maxY, HANGARLENGTH - 1, box);
        place(level, upsideSlab, 12, maxY, HANGARLENGTH - 1, box);
        place(level, upsideSlab, 14, maxY, HANGARLENGTH - 1, box);
        place(level, upsideSlab, 16, maxY, HANGARLENGTH - 1, box);

        // Ceiling struts
        final BlockState strut = BaseBlocks.stairs(blockStair, stairmetaB + 4);
        place(level, strut, 4, maxY - 1, HANGARLENGTH - 3, box);
        place(level, strut, 4, maxY - 2, HANGARLENGTH - 2, box);
        place(level, strut, 4, maxY - 3, HANGARLENGTH - 1, box);
        place(level, strut, 22, maxY - 1, HANGARLENGTH - 3, box);
        place(level, strut, 22, maxY - 2, HANGARLENGTH - 2, box);
        place(level, strut, 22, maxY - 3, HANGARLENGTH - 1, box);

        // FIRST SECTION
        for (int zz = HANGARLENGTH; zz > HANGARLENGTH - 5; zz--) {
            for (int xx = 0; xx <= maxX; xx += maxX) {
                place(level, moonWall, xx, 1, zz, box);
                for (int y = 2; y < 7; y++) {
                    place(level, blockAir, xx, y, zz, box);
                }
                for (int y = 9; y < maxY - 1; y++) {
                    place(level, blockAir, xx, y, zz, box);
                }
                place(level, blockWall, xx, 7, zz, box);
                place(level, blockWall, xx, 8, zz, box);
            }

            if (zz % 3 == 1) {
                place(level, blockPlain, 4, maxY, zz, box);
                place(level, blockPlain, 22, maxY, zz, box);
            } else {
                place(level, blockPattern, 4, maxY, zz, box);
                place(level, blockPattern, 22, maxY, zz, box);
            }

            // Top middle
            if (zz < HANGARLENGTH - 1) {
                place(level, blockGrid, 9, maxY, zz, box);
                place(level, blockRedstone, 11, maxY, zz, box);
                place(level, blockGrid, 13, maxY, zz, box);
                place(level, blockRedstone, 15, maxY, zz, box);
                place(level, blockGrid, 17, maxY, zz, box);
            }

            // Floor
            place(level, blockPattern, 7, 0, zz, box);
            place(level, blockPattern, 9, 0, zz, box);
            place(level, blockPattern, 19, 0, zz, box);
            place(level, blockPattern, 17, 0, zz, box);
            place(level, blockGrid, 8, 0, zz, box);
            place(level, blockGrid, 18, 0, zz, box);
            if (zz > HANGARLENGTH - 5) {
                place(level, blockPlain, 10, 0, zz, box);
                place(level, blockPlain, 16, 0, zz, box);
                place(level, blockPlain, 11, 0, zz, box);
                place(level, blockPlain, 15, 0, zz, box);
            }
            if (zz > HANGARLENGTH - 4) {
                place(level, blockPlain, 12, 0, zz, box);
                place(level, blockPlain, 14, 0, zz, box);
            }
            if (zz > HANGARLENGTH - 3) {
                place(level, blockPlain, 13, 0, zz, box);
            }
        }

        // Floor end
        final BlockState floorStair = BaseBlocks.stairs(blockStair, stairmetaB);
        place(level, floorStair, 10, 0, HANGARLENGTH - 6, box);
        place(level, floorStair, 16, 0, HANGARLENGTH - 6, box);
        place(level, floorStair, 11, 0, HANGARLENGTH - 5, box);
        place(level, floorStair, 15, 0, HANGARLENGTH - 5, box);
        place(level, floorStair, 12, 0, HANGARLENGTH - 4, box);
        place(level, floorStair, 14, 0, HANGARLENGTH - 4, box);
        place(level, floorStair, 13, 0, HANGARLENGTH - 3, box);
        place(level, blockPlain, 10, 0, HANGARLENGTH - 5, box);
        place(level, blockPlain, 16, 0, HANGARLENGTH - 5, box);

        // Join the first and second sections
        place(level, moonWall, 0, 1, HANGARLENGTH - 5, box);
        place(level, moonWall, maxX, 1, HANGARLENGTH - 5, box);

        place(level, BaseBlocks.arcLamp(lampmeta2 ^ 1), 12, maxY, HANGARLENGTH - 3, box);
        place(level, BaseBlocks.arcLamp(lampmeta2), 14, maxY, HANGARLENGTH - 3, box);

        // SECOND SECTION
        for (int zz = HANGARLENGTH - 5; zz > midPoint; zz--) {
            // Top sides
            if (zz % 3 == 1) {
                place(level, blockSlab, 3, maxY, zz, box);
                place(level, blockPlain, 4, maxY, zz, box);
                place(level, blockPlain, 22, maxY, zz, box);
                place(level, blockSlab, 23, maxY, zz, box);
                place(level, BaseBlocks.stairs(blockStair, stairmeta + 4), 2, maxY - 1, zz, box);
                place(level, BaseBlocks.stairs(blockStair, stairmeta + 4), 1, maxY - 2, zz, box);
                place(level, BaseBlocks.stairs(blockStair, (stairmeta ^ 1) + 4), 24, maxY - 1, zz, box);
                place(level, BaseBlocks.stairs(blockStair, (stairmeta ^ 1) + 4), 25, maxY - 2, zz, box);
                if (zz != HANGARLENGTH - 11) {
                    place(level, BaseBlocks.stairs(blockStair, stairmeta), 1, 1, zz, box);
                    place(level, BaseBlocks.stairs(blockStair, stairmeta ^ 1), 25, 1, zz, box);
                    floorStrut(level, blockWall, decoWall, zz, box);
                }
            } else {
                place(level, blockPattern, 22, maxY, zz, box);
                place(level, blockPattern, 4, maxY, zz, box);
            }

            // Top middle
            if (zz == midPoint + 1) {
                place(level, blockDesh, 9, maxY, zz, box);
                place(level, blockPattern, 11, maxY, zz, box);
                place(level, blockDesh, 13, maxY, zz, box);
                place(level, blockPattern, 15, maxY, zz, box);
                place(level, blockDesh, 17, maxY, zz, box);
            } else {
                place(level, blockGrid, 9, maxY, zz, box);
                place(level, blockGrid, 13, maxY, zz, box);
                place(level, blockGrid, 17, maxY, zz, box);
                if (zz == HANGARLENGTH - 12) {
                    // 1.12.2 concealedRepeater_Powered
                    place(level, BaseBlocks.concealedRepeater(this.direction.getAxis() == Direction.Axis.Z), 11, maxY, zz, box);
                    place(level, BaseBlocks.concealedRepeater(this.direction.getAxis() == Direction.Axis.Z), 15, maxY, zz, box);
                } else {
                    place(level, blockRedstone, 11, maxY, zz, box);
                    place(level, blockRedstone, 15, maxY, zz, box);
                }
            }
            if ((zz - midPoint - 1) % 6 == 0) {
                place(level, BaseBlocks.arcLamp(lampmeta2), 10, maxY, zz, box);
                place(level, BaseBlocks.arcLamp(lampmeta2 ^ 1), 16, maxY, zz, box);
            }

            // Walls
            for (int xx = 0; xx <= maxX; xx += maxX) {
                place(level, blockPlain, xx, maxY - 2, zz, box);
                place(level, blockPlain, xx, maxY - 4, zz, box);
                place(level, blockPlain, xx, 2, zz, box);
                place(level, blockPlain, xx, 4, zz, box);
                if (zz % 3 == 1) {
                    place(level, blockPattern, xx, maxY - 3, zz, box);
                    place(level, blockPlain, xx, 3, zz, box);
                    place(level, blockWall, xx, 5, zz, box);
                    place(level, blockWall, xx, 6, zz, box);
                    place(level, blockWall, xx, 9, zz, box);
                    place(level, blockWall, xx, 10, zz, box);
                }
                place(level, blockWall, xx, 7, zz, box);
                place(level, blockWall, xx, 8, zz, box);
            }

            // Floor
            final BlockState blockBeam = zz % 2 == 0 ? blockPattern : blockPlain;
            place(level, blockBeam, 7, 0, zz, box);
            place(level, blockBeam, 9, 0, zz, box);
            place(level, blockBeam, 19, 0, zz, box);
            place(level, blockBeam, 17, 0, zz, box);
            place(level, blockGrid, 8, 0, zz, box);
            place(level, blockGrid, 18, 0, zz, box);
        }

        // THIRD SECTION: each beam runs on towards the open end, stopping at random
        final int zz = midPoint;

        // Top sides
        extrudeTrioOff(level, random, 2, blockPlain, blockPattern, 4, maxY, zz, box);
        extrudeTrioOff(level, random, 2, blockPlain, blockPattern, 22, maxY, zz, box);
        extrude(level, random, 2, blockGrid, 23, maxY, zz, box);
        extrude(level, random, 2, blockGrid, 3, maxY, zz, box);

        // Walls
        for (int xx = 0; xx <= maxX; xx += maxX) {
            extrudeTrio(level, random, 1, blockPlain, blockPattern, xx, maxY - 2, zz, box);
            extrudeTrio(level, random, 1, blockPlain, blockPattern, xx, maxY - 4, zz, box);
            extrudeTrio(level, random, 1, blockPlain, blockPattern, xx, 2, zz, box);
            extrudeTrio(level, random, 1, blockPlain, blockPattern, xx, 4, zz, box);
            extrudeTrio(level, random, 0, blockBars, blockAir, xx, 5, zz, box);
            extrudeTrio(level, random, 0, blockBars, blockAir, xx, 6, zz, box);
            extrudeTrio(level, random, 0, blockBars, blockAir, xx, 7, zz, box);
            extrudeTrio(level, random, 0, blockBars, blockAir, xx, 8, zz, box);
            extrudeTrio(level, random, 0, blockBars, blockAir, xx, 9, zz, box);
            extrudeTrio(level, random, 0, blockBars, blockAir, xx, 10, zz, box);
            extrude(level, random, 2, blockBars, xx, maxY - 3, zz, box);
            extrude(level, random, 2, blockBars, xx, 3, zz, box);
        }

        // Floor
        extrude(level, random, 2, blockGrid, 1, 1, zz, box);
        extrude(level, random, 2, blockGrid, 2, 1, zz, box);
        extrude(level, random, 2, blockGrid, 24, 1, zz, box);
        extrude(level, random, 2, blockGrid, 25, 1, zz, box);
        extrudeDuo(level, random, 3, blockPlain, blockPattern, 7, 0, zz, box);
        extrudeDuo(level, random, 3, blockPlain, blockPattern, 9, 0, zz, box);
        extrudeDuo(level, random, 3, blockPlain, blockPattern, 19, 0, zz, box);
        extrudeDuo(level, random, 3, blockPlain, blockPattern, 17, 0, zz, box);
        extrude(level, random, 3, blockGrid, 8, 0, zz, box);
        extrude(level, random, 3, blockGrid, 18, 0, zz, box);
        extrude(level, random, 3, blockGrid, 5, 0, zz, box);
        extrude(level, random, 3, blockGrid, 21, 0, zz, box);
        for (int xx = 10; xx <= 16; xx++) {
            place(level, moonWall, xx, 0, zz, box);
        }
        extrude(level, random, 2, blockGrid, 10, 0, zz - 1, box);
        extrude(level, random, 2, blockGrid, 16, 0, zz - 1, box);

        // WHERE THE SECOND AND THIRD SECTIONS MEET
        // Extend the mid-side walls a little way past the midPoint
        for (int xx = 0; xx <= maxX; xx += maxX) {
            place(level, blockWall, xx, 7, midPoint, box);
            place(level, blockWall, xx, 8, midPoint, box);
            place(level, blockBars, xx, 3, midPoint + 1, box);
            place(level, blockBars, xx, maxY - 3, midPoint + 1, box);
        }
        // Extend the walkways a little way also
        place(level, blockGrid, 1, 1, midPoint + 1, box);
        place(level, blockGrid, 2, 1, midPoint + 1, box);
        place(level, blockGrid, 5, 0, midPoint + 1, box);
        place(level, blockGrid, 21, 0, midPoint + 1, box);
        place(level, blockGrid, 24, 1, midPoint + 1, box);
        place(level, blockGrid, 25, 1, midPoint + 1, box);
    }

    private void floorStrut(WorldGenLevel level, BlockState blockWall, BlockState decoWall, int zz, BoundingBox box) {
        place(level, blockWall, 1, 0, zz, box);
        place(level, decoWall, 2, 0, zz, box);
        place(level, blockWall, 3, 0, zz, box);
        place(level, decoWall, 4, 0, zz, box);
        place(level, blockWall, 5, 0, zz, box);
        place(level, decoWall, 6, 0, zz, box);
        place(level, decoWall, 20, 0, zz, box);
        place(level, blockWall, 21, 0, zz, box);
        place(level, decoWall, 22, 0, zz, box);
        place(level, blockWall, 23, 0, zz, box);
        place(level, decoWall, 24, 0, zz, box);
        place(level, blockWall, 25, 0, zz, box);
    }

    /** 1.12.2 extrude: towards z 0 until a fresh random draw (up to 4 x (4 - solid)) stops it. */
    private void extrude(WorldGenLevel level, RandomSource rand, int solid, BlockState state, int x, int y, int z, BoundingBox box) {
        for (int zz = z; zz >= rand.nextInt(4 * (4 - solid)); zz--) {
            place(level, state, x, y, zz, box);
        }
    }

    private void extrudeDuo(WorldGenLevel level, RandomSource rand, int solid, BlockState blockA, BlockState blockB, int x, int y, int z, BoundingBox box) {
        for (int zz = z; zz >= rand.nextInt(4 * (4 - solid)); zz--) {
            place(level, zz % 2 == 0 ? blockA : blockB, x, y, zz, box);
        }
    }

    private void extrudeTrio(WorldGenLevel level, RandomSource rand, int solid, BlockState blockA, BlockState blockB, int x, int y, int z, BoundingBox box) {
        for (int zz = z; zz >= rand.nextInt(4 * (4 - solid)); zz--) {
            place(level, zz % 3 == 0 ? blockA : blockB, x, y, zz, box);
        }
    }

    private void extrudeTrioOff(WorldGenLevel level, RandomSource rand, int solid, BlockState blockA, BlockState blockB, int x, int y, int z, BoundingBox box) {
        for (int zz = z; zz >= rand.nextInt(4 * (4 - solid)); zz--) {
            place(level, zz % 3 == 1 ? blockA : blockB, x, y, zz, box);
        }
    }
}
