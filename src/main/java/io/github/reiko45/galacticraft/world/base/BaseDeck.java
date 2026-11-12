/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base.BaseDeck and
 * BaseStart). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.base;

import io.github.reiko45.galacticraft.registry.GCStructures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.Nullable;

/**
 * One deck of a base: the central corridor with doorways off both sides into its rooms. The first deck (tier 1) has two
 * more stacked on it (a middle deck, tier 0, and a top deck, tier 2), or on a hangar base a control room deck (tier 6)
 * with a window over the hangar; it also plans every room, the hangar and the floor plate under the lowest deck.
 */
public class BaseDeck extends BasePiece {
    static final int ROOMSMALL = 6;
    static final int ROOMLARGE = 8;

    protected int roomsOnSide;
    protected int roomDepth;
    protected int largeRoomPosA;
    protected int largeRoomPosB;
    /** Bit 1: the lowest deck of a stack; 2: the highest; 4: the control room (0 for middle decks). */
    protected int deckTier;
    @Nullable
    private List<BaseDeck> otherDecks;

    public BaseDeck(BaseConfiguration configuration, RandomSource rand, int blockPosX, int blockPosY, int blockPosZ, int tier, Direction dir) {
        super(GCStructures.BASE_DECK.get(), configuration, configuration.corridorWidth() + 1, configuration.roomHeight() + 1,
                configuration.corridorLength(), dir);
        this.roomsOnSide = configuration.roomsNo();
        this.roomDepth = configuration.roomDepth() + (tier == 0 ? 1 : 0);
        this.deckTier = tier;
        setOrientation(Direction.NORTH);
        if (dir.getAxis() == Direction.Axis.X) {
            final int w = this.sizeX;
            this.sizeX = this.sizeZ;
            this.sizeZ = w;
        }
        int xMin = blockPosX - this.sizeX / 2;
        int zMin = blockPosZ - this.sizeZ / 2;
        setBox(xMin, blockPosY, zMin, xMin + this.sizeX, blockPosY + this.sizeY, zMin + this.sizeZ);
        if (this.roomsOnSide <= 1) {
            this.largeRoomPosA = 0;
            this.largeRoomPosB = 2;
        } else {
            this.largeRoomPosA = rand.nextInt(this.roomsOnSide - 1);
            this.largeRoomPosB = this.largeRoomPosA + 1 + rand.nextInt(this.roomsOnSide - this.largeRoomPosA - 1);
        }
        if (tier == 1) {
            createOtherDecks(configuration, rand, blockPosX, blockPosY, blockPosZ);
        }
        // Special settings for the Control Room
        if ((tier & 4) == 4) {
            this.roomDepth -= 2;
            if (dir.getAxis() == Direction.Axis.X) {
                this.sizeZ += 4;
            } else {
                this.sizeX += 4;
            }
            xMin = blockPosX - this.sizeX / 2;
            zMin = blockPosZ - this.sizeZ / 2;
            switch (dir) {
                case NORTH -> this.sizeZ++;
                case SOUTH -> {
                    zMin--;
                    this.sizeZ++;
                }
                case WEST -> this.sizeX++;
                default -> {
                    xMin--;
                    this.sizeX++;
                }
            }
            setBox(xMin, blockPosY, zMin, xMin + this.sizeX, blockPosY + this.sizeY, zMin + this.sizeZ);
        }
    }

    public BaseDeck(StructurePieceSerializationContext context, CompoundTag tag) {
        super(GCStructures.BASE_DECK.get(), tag);
        final int details = tag.getInt("dD");
        this.deckTier = details & 15;
        this.largeRoomPosB = details >> 8;
        this.largeRoomPosA = (details >> 4) & 15;
        this.roomsOnSide = this.configuration.roomsNo();
        this.roomDepth = this.configuration.roomDepth() + (this.deckTier == 0 ? 1 : 0) - ((this.deckTier & 4) == 4 ? 2 : 0);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putInt("dD", this.deckTier + (this.largeRoomPosA << 4) + (this.largeRoomPosB << 8));
    }

    public int deckTier() {
        return this.deckTier;
    }

    private void createOtherDecks(BaseConfiguration configuration, RandomSource rand, int blockPosX, int blockPosY, int blockPosZ) {
        this.otherDecks = new ArrayList<>();
        if (configuration.isHangarDeck()) {
            this.otherDecks.add(new BaseDeck(configuration, rand, blockPosX, blockPosY + this.sizeY + 1, blockPosZ, 6, this.direction));
        } else {
            this.otherDecks.add(new BaseDeck(configuration, rand, blockPosX, blockPosY + this.sizeY, blockPosZ, 0, this.direction));
            this.otherDecks.add(new BaseDeck(configuration, rand, blockPosX, blockPosY + this.sizeY + this.sizeY, blockPosZ, 2, this.direction));
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        final BaseConfiguration.BaseType type = this.configuration.deckType();
        final boolean hangar = this.configuration.isHangarDeck();
        final BlockState blockWall = this.configuration.wallBlock();
        final BlockState blockGrid = BaseBlocks.grid();
        final Block blockStair = BaseBlocks.moonStairs();
        final BlockState blockAir = Blocks.AIR.defaultBlockState();
        final boolean directionNS = this.direction.getAxis() == Direction.Axis.Z;
        final int ceilingSpacer = (directionNS ? this.sizeZ : this.sizeX) / this.roomsOnSide + 1;
        int ceilingDeco = ceilingSpacer;

        final int startX = 0;
        final int startZ = 0;
        int endX = this.sizeX;
        int endZ = this.sizeZ;
        int endY = this.sizeY;
        if (type == BaseConfiguration.BaseType.AVIAN && !hangar) {
            endY--;
        }
        final int hangarOffset = hangar ? 1 : 0;

        // This is the central corridor of every base
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = 0; y <= endY; y++) {
                    if (x == startX || x == endX || y == 0 || y == endY || z == startZ || z == endZ) {
                        boolean skipCap = false;
                        if (hangar && this.deckTier == 1 && (this.direction == Direction.NORTH && z == endZ || this.direction == Direction.SOUTH && z == startZ
                                || this.direction == Direction.WEST && x == endX || this.direction == Direction.EAST && x == startX)) {
                            // Make an airlock at the hangar end, if it's a hangar deck
                            if ((!directionNS && (z == startZ + 1 || z == endZ - 1) || directionNS && (x == startX + 1 || x == endX - 1)) && y < this.sizeY - 1) {
                                place(level, BaseBlocks.airLockFrame(), x, y, z, box);
                            } else if (y >= endY - 1 || directionNS && (x == startX || x == endX) || !directionNS && (z == startZ || z == endZ)) {
                                place(level, blockWall, x, y, z, box);
                            } else if (y == 0 || y == endY - 2) {
                                // Test mid point bottom
                                if (y == 0 && (!directionNS && z == (startZ + endZ) / 2 || directionNS && x == (startX + endX) / 2)) {
                                    place(level, BaseBlocks.airLockController(), x, y, z, box);
                                } else {
                                    place(level, BaseBlocks.airLockFrame(), x, y, z, box);
                                }
                                skipCap = true;
                            } else {
                                place(level, blockAir, x, y, z, box);
                            }
                        }
                        // Skip certain blocks at the bottom to give a chamfered edge (1.12.2 also meant to chamfer the
                        // top deck's roof, but tested (deckTier & 1) == 2, which never holds)
                        else if (!(y == 0 && (this.deckTier & 1) == 1
                                && (directionNS && (z == startZ || z == endZ) || !directionNS && (x == startX || x == endX)))) {
                            if (y == 0 && (this.deckTier & 1) == 1 && type == BaseConfiguration.BaseType.HUMANOID
                                    && (directionNS && (x == startX + 2 + hangarOffset || x == startX + 4 + hangarOffset)
                                    || !directionNS && (z == startZ + 2 + hangarOffset || z == startZ + 4 + hangarOffset))) {
                                place(level, blockGrid, x, y, z, box);
                            } else if (!hangar && (this.direction == Direction.SOUTH && z == endZ || this.direction == Direction.NORTH && z == startZ
                                    || this.direction == Direction.EAST && x == endX || this.direction == Direction.WEST && x == startX)) {
                                // Some end windows at the ends of the corridors
                                BlockState windowOrWall = blockWall;
                                final int w = directionNS ? x : z;
                                final int startW = directionNS ? startX : startZ;
                                final int endW = directionNS ? endX : endZ;
                                if (type == BaseConfiguration.BaseType.AVIAN) {
                                    if (y == 2 && w == startW + 2) {
                                        windowOrWall = BaseBlocks.spaceGlass();
                                    }
                                } else {
                                    final int edge = type == BaseConfiguration.BaseType.TUNNELER ? 1 : 0;
                                    if ((y == 2 || y == 3) && w > startW + edge && w < endW - edge) {
                                        windowOrWall = BaseBlocks.spaceGlass();
                                    }
                                }
                                place(level, windowOrWall, x, y, z, box);
                            } else {
                                place(level, blockWall, x, y, z, box);
                            }
                        }

                        // Special case: cap the grid ends
                        if (y == 0 && (this.deckTier & 1) == 1 && type == BaseConfiguration.BaseType.HUMANOID
                                && (directionNS && x >= startX + 2 + hangarOffset && x <= startX + 4 + hangarOffset
                                || !directionNS && z >= startZ + 2 + hangarOffset && z <= startZ + 4 + hangarOffset)) {
                            if ((directionNS && (z == startZ || z == endZ) || !directionNS && (x == startX || x == endX)) && !skipCap) {
                                place(level, blockWall, x, y, z, box);
                            }
                        }
                    } else if (type.ordinal() >= BaseConfiguration.BaseType.AVIAN.ordinal() && (y == 1 || y == endY - 1)) {
                        // Internal decoration - deck corridor corners
                        int top = y == 1 ? 0 : 4;
                        if (type == BaseConfiguration.BaseType.TUNNELER) {
                            top++;
                        }
                        if (directionNS) {
                            if (x == startX + 1) {
                                place(level, BaseBlocks.stairs(blockStair, top), x, y, z, box);
                            } else if (x == endX - 1) {
                                place(level, BaseBlocks.stairs(blockStair, 1 ^ top), x, y, z, box);
                            } else if (type == BaseConfiguration.BaseType.AVIAN) {
                                if (z == ceilingDeco && top >= 4) {
                                    place(level, BaseBlocks.stairs(blockStair, 3 ^ top), x, y, z, box);
                                } else if (z == ceilingDeco + 1 && top >= 4) {
                                    place(level, BaseBlocks.stairs(blockStair, 2 ^ top), x, y, z, box);
                                    if (x >= endX - 2) {
                                        ceilingDeco += ceilingSpacer;
                                    }
                                } else {
                                    place(level, blockAir, x, y, z, box);
                                }
                            } else {
                                place(level, blockAir, x, y, z, box);
                            }
                        } else {
                            if (z == startZ + 1) {
                                place(level, BaseBlocks.stairs(blockStair, 2 ^ top), x, y, z, box);
                            } else if (z == endZ - 1) {
                                place(level, BaseBlocks.stairs(blockStair, 3 ^ top), x, y, z, box);
                            } else if (type == BaseConfiguration.BaseType.AVIAN) {
                                if (x == ceilingDeco && top == 4) {
                                    place(level, BaseBlocks.stairs(blockStair, 1 ^ top), x, y, z, box);
                                } else if (x == ceilingDeco + 1 && top == 4) {
                                    place(level, BaseBlocks.stairs(blockStair, top), x, y, z, box);
                                    if (z >= endZ - 2) {
                                        ceilingDeco += ceilingSpacer;
                                    }
                                } else {
                                    place(level, blockAir, x, y, z, box);
                                }
                            } else {
                                place(level, blockAir, x, y, z, box);
                            }
                        }
                    } else {
                        place(level, blockAir, x, y, z, box);
                    }
                }

                // Infill for the extra height layer
                if (endY != this.sizeY) {
                    place(level, blockWall, x, this.sizeY, z, box);
                }
            }
        }

        int leftX = (this.deckTier & 4) == 4 && (this.direction == Direction.SOUTH || this.direction == Direction.EAST) ? 3 : 2;
        int rightX = leftX;
        int leftZ = leftX;
        int rightZ = rightX;
        for (int i = 0; i < this.roomsOnSide; i++) {
            final boolean largeRoom = i == this.largeRoomPosA || i == this.largeRoomPosB;
            final int roomsize = largeRoom ? ROOMLARGE : ROOMSMALL;
            final int largeRoomOffset = largeRoom ? 2 : 1;
            if (directionNS) {
                makeDoorway(level, this.sizeX, leftZ + largeRoomOffset, true, box);
                makeDoorway(level, 0, rightZ + largeRoomOffset, true, box);
                leftZ += roomsize;
                rightZ += roomsize;
            } else {
                makeDoorway(level, leftX + largeRoomOffset, this.sizeZ, false, box);
                makeDoorway(level, rightX + largeRoomOffset, 0, false, box);
                leftX += roomsize;
                rightX += roomsize;
            }
        }

        setOrientation(directionNS ? this.direction : this.direction.getOpposite()); // 1.12.2: "bug in Vanilla?"
        try {
            if (hangar && this.deckTier == 1) {
                // Create an access hole between the floors
                final int width = directionNS ? this.sizeX : this.sizeZ;
                place(level, blockAir, 0, this.sizeY, 1, box);
                place(level, blockAir, 1, this.sizeY, 1, box);
                place(level, blockAir, 1, this.sizeY - 1, 1, box);
                place(level, blockAir, width - 1, this.sizeY - 1, 1, box);
                place(level, blockAir, width - 1, this.sizeY, 1, box);
                place(level, blockAir, width, this.sizeY, 1, box);
            }

            // Special settings for the Control Room
            if ((this.deckTier & 4) == 4) {
                if (!directionNS) {
                    final int w = endX;
                    endX = endZ;
                    endZ = w;
                }
                for (int y = 1; y < 5; y++) {
                    for (int x = 2; x < endX - 1; x++) {
                        place(level, BaseBlocks.spaceGlass(), x, y, endZ, box);
                    }
                }
                // Two concealed detectors, each with a lever beside it (1.12.2 concealedDetector meta 8 + facing,
                // + 4 off humanoid bases; lever meta 4 or 3: on a wall, facing north or south)
                final BlockState detector = BaseBlocks.concealedDetector(type == BaseConfiguration.BaseType.HUMANOID);
                place(level, detector, endX / 2 - 2, this.sizeY - 1, endZ, box);
                place(level, detector, endX / 2 + 2, this.sizeY - 1, endZ, box);
                final BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                        .setValue(LeverBlock.FACING, this.direction.getAxis() == Direction.Axis.Z ? Direction.NORTH : Direction.SOUTH);
                place(level, lever, endX / 2 - 2, this.sizeY - 1, endZ - 1, box);
                place(level, lever, endX / 2 + 2, this.sizeY - 1, endZ - 1, box);
                // Two (concealed) redstone blocks
                place(level, BaseBlocks.concealedRedstone(), endX / 2 - 2, this.sizeY, endZ, box);
                place(level, BaseBlocks.concealedRedstone(), endX / 2 + 2, this.sizeY, endZ, box);
                // Create an access hole between the floors
                place(level, blockAir, 2, 0, 1, box);
                place(level, blockAir, 3, 0, 1, box);
                place(level, blockAir, 11, 0, 1, box);
                place(level, blockAir, 12, 0, 1, box);
            }
        } finally {
            setOrientation(Direction.NORTH);
        }
    }

    /** 1.12.2 getRooms: the rooms down both sides, the hangar and floor plate, and the decks above with their rooms. */
    public List<BasePiece> rooms(int roomIndex, RandomSource rand) {
        final List<BasePiece> rooms = new ArrayList<>();
        final boolean directionNS = this.direction.getAxis() == Direction.Axis.Z;
        final Direction left = directionNS ? Direction.WEST : Direction.SOUTH;
        final Direction right = directionNS ? Direction.EAST : Direction.NORTH;
        int leftX = directionNS ? this.boundingBox.maxX() + 1 : this.boundingBox.minX();
        int leftZ = directionNS ? this.boundingBox.minZ() : this.boundingBox.maxZ() + 1;
        int rightX = directionNS ? this.boundingBox.minX() - this.roomDepth - 1 : this.boundingBox.minX();
        int rightZ = directionNS ? this.boundingBox.minZ() : this.boundingBox.minZ() - this.roomDepth - 1;
        // now offset the effect of moving the control room 1 position north or west, in two cases
        if ((this.deckTier & 4) == 4 && this.direction == Direction.SOUTH) {
            rightZ++;
            leftZ++;
        }
        if ((this.deckTier & 4) == 4 && this.direction == Direction.EAST) {
            rightX++;
            leftX++;
        }

        for (int i = 0; i < this.roomsOnSide; i++) {
            final boolean largeRoom = i == this.largeRoomPosA || i == this.largeRoomPosB;
            final int roomsize = largeRoom ? ROOMLARGE : ROOMSMALL;
            rooms.add(room(roomIndex++, left, leftX, leftZ, largeRoom, true));
            rooms.add(room(roomIndex++, right, rightX, rightZ, largeRoom, false));
            if (directionNS) {
                leftZ += roomsize;
                rightZ += roomsize;
            } else {
                leftX += roomsize;
                rightX += roomsize;
            }
        }

        final boolean hangar = this.configuration.isHangarDeck();
        if (hangar && (this.deckTier & 1) == 1) {
            final int blockX;
            final int blockZ;
            switch (this.direction) {
                case NORTH -> {
                    blockX = this.boundingBox.minX() + (this.sizeX - BaseHangar.HANGARWIDTH) / 2;
                    blockZ = this.boundingBox.maxZ() + 1;
                }
                case SOUTH -> {
                    blockX = this.boundingBox.minX() + (this.sizeX - BaseHangar.HANGARWIDTH) / 2;
                    blockZ = this.boundingBox.minZ() - 1 - BaseHangar.HANGARLENGTH;
                }
                case WEST -> {
                    blockX = this.boundingBox.maxX() + 1;
                    blockZ = this.boundingBox.minZ() + (this.sizeZ - BaseHangar.HANGARWIDTH) / 2;
                }
                default -> {
                    blockX = this.boundingBox.minX() - 1 - BaseHangar.HANGARLENGTH;
                    blockZ = this.boundingBox.minZ() + (this.sizeZ - BaseHangar.HANGARWIDTH) / 2;
                }
            }
            rooms.add(new BaseHangar(this.configuration, blockX, blockZ, this.direction.getOpposite()));
        }

        if ((hangar || this.configuration.deckType() == BaseConfiguration.BaseType.HUMANOID) && (this.deckTier & 1) == 1) {
            int blockX = this.boundingBox.minX();
            int blockZ = this.boundingBox.minZ();
            int sX = this.sizeX;
            int sZ = this.sizeZ;
            switch (this.direction) {
                case NORTH -> {
                    blockZ++;
                    sZ--;
                    if (!hangar) {
                        sZ--;
                    }
                }
                case SOUTH -> {
                    sZ--;
                    if (!hangar) {
                        blockZ++;
                        sZ--;
                    }
                }
                case WEST -> {
                    blockX++;
                    sX--;
                    if (!hangar) {
                        sX--;
                    }
                }
                default -> {
                    sX--;
                    if (!hangar) {
                        blockX++;
                        sX--;
                    }
                }
            }
            rooms.add(new BasePlate(this.configuration, blockX, this.boundingBox.minY() - 1, blockZ, sX, sZ, this.direction));
        }

        if (this.otherDecks != null) {
            for (BaseDeck deck : this.otherDecks) {
                rooms.add(deck);
                final List<BasePiece> newRooms = deck.rooms(roomIndex, rand);
                rooms.addAll(newRooms);
                roomIndex += newRooms.size();
            }
        }
        return rooms;
    }

    protected void makeDoorway(WorldGenLevel level, int x, int z, boolean directionNS, BoundingBox box) {
        final BlockState blockAir = Blocks.AIR.defaultBlockState();
        final Block blockStair = BaseBlocks.moonStairs();
        final int meta = directionNS ? 2 : 0;
        switch (this.configuration.deckType()) {
            case HUMANOID -> {
                if (directionNS) {
                    z--;
                } else {
                    x--;
                }
                place(level, BaseBlocks.airLockFrame(), x, 1, z, box);
                place(level, BaseBlocks.airLockFrame(), x, 2, z, box);
                place(level, BaseBlocks.airLockFrame(), x, 3, z, box);
                if (directionNS) {
                    z++;
                } else {
                    x++;
                }
                place(level, blockAir, x, 1, z, box);
                place(level, blockAir, x, 2, z, box);
                place(level, BaseBlocks.airLockController(), x, 0, z, box);
                place(level, BaseBlocks.airLockFrame(), x, 3, z, box);
                if (directionNS) {
                    z++;
                } else {
                    x++;
                }
                place(level, BaseBlocks.airLockFrame(), x, 1, z, box);
                place(level, BaseBlocks.airLockFrame(), x, 2, z, box);
                place(level, BaseBlocks.airLockFrame(), x, 3, z, box);
            }
            case AVIAN -> {
                place(level, BaseBlocks.stairs(blockStair, meta), x, this.sizeY - 4, z, box);
                place(level, blockAir, x, this.sizeY - 3, z, box);
                place(level, BaseBlocks.stairs(blockStair, 4 + meta), x, this.sizeY - 2, z, box);
                if (directionNS) {
                    z++;
                } else {
                    x++;
                }
                place(level, BaseBlocks.stairs(blockStair, 1 + meta), x, this.sizeY - 4, z, box);
                place(level, blockAir, x, this.sizeY - 3, z, box);
                place(level, BaseBlocks.stairs(blockStair, 5 + meta), x, this.sizeY - 2, z, box);
            }
            case TUNNELER -> {
                if (directionNS) {
                    z--;
                } else {
                    x--;
                }
                place(level, BaseBlocks.stairs(blockStair, 1 + meta), x, 2, z, box);
                place(level, BaseBlocks.stairs(blockStair, 5 + meta), x, 3, z, box);
                if (directionNS) {
                    z++;
                } else {
                    x++;
                }
                place(level, blockAir, x, 2, z, box);
                place(level, blockAir, x, 3, z, box);
                if (directionNS) {
                    z++;
                } else {
                    x++;
                }
                place(level, BaseBlocks.stairs(blockStair, meta), x, 2, z, box);
                place(level, BaseBlocks.stairs(blockStair, 4 + meta), x, 3, z, box);
            }
        }
    }

    protected BaseRoom room(int i, Direction dir, int blockX, int blockZ, boolean large, boolean left) {
        int sX = large ? ROOMLARGE : ROOMSMALL;
        int sZ = sX;
        if (dir.getAxis() == Direction.Axis.Z) {
            sZ = this.roomDepth;
        } else {
            sX = this.roomDepth;
        }
        return new BaseRoom(this.configuration, blockX, this.boundingBox.minY(), blockZ, sX, this.sizeY, sZ, dir, this.configuration.randomRoom(i),
                left ? i == 0 : i == this.roomsOnSide - 1, left ? i == this.roomsOnSide - 1 : i == 0, this.deckTier);
    }
}
