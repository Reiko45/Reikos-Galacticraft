/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.StructureComponentVillage and its
 * Well/StartPiece, PathGen, Torch, Field, Field2, House and WoodHut subclasses). Copyright (c) 2023 Team Galacticraft.
 * Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.village;

import io.github.reiko45.galacticraft.entity.AlienVillagerEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One piece of a Moon village (1.12.2 StructureComponentVillage and its subclasses): the well at the centre, a path, a
 * lamp post, a field of saplings, a field of crops, or a domed house (the 1.12.2 "house" and "wood hut" differ only in
 * which way two of their torches face). Pieces settle onto the ground when built: each takes the average height of the
 * surface under it in the first chunk that builds it, as 1.12.2 did.
 */
public class MoonVillagePiece extends StructurePiece {
    public enum Kind {
        WELL, PATH, TORCH, FIELD, CROP_FIELD, HOUSE, WOOD_HUT
    }

    private final Kind kind;
    private int averageGroundLevel = -1;
    private int villagersSpawned;
    /** Crop field: its four crops (1.12.2 cropTypeA-D). */
    private final Block[] crops;

    public MoonVillagePiece(Kind kind, int genDepth, BoundingBox box, Direction orientation, @org.jetbrains.annotations.Nullable Block[] crops) {
        super(GCStructures.MOON_VILLAGE_PIECE.get(), genDepth, box);
        this.kind = kind;
        this.crops = crops;
        setOrientation(orientation);
        if (kind == Kind.PATH) {
            // 1.12.2 PathGen kept the path's length in its "averageGroundLevel"
            this.averageGroundLevel = Math.max(box.getXSpan(), box.getZSpan());
        }
    }

    public MoonVillagePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(GCStructures.MOON_VILLAGE_PIECE.get(), tag);
        this.kind = Kind.values()[Mth.clamp(tag.getInt("Kind"), 0, Kind.values().length - 1)];
        this.averageGroundLevel = tag.getInt("AvgGroundLevel");
        this.villagersSpawned = tag.getInt("VCount");
        this.crops = new Block[4];
        for (int i = 0; i < 4; i++) {
            this.crops[i] = cropById(tag.getInt("Crop" + i));
        }
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Kind", this.kind.ordinal());
        tag.putInt("AvgGroundLevel", this.averageGroundLevel);
        tag.putInt("VCount", this.villagersSpawned);
        for (int i = 0; i < 4; i++) {
            tag.putInt("Crop" + i, this.crops == null ? 0 : cropId(this.crops[i]));
        }
    }

    public Kind kind() {
        return this.kind;
    }

    /** For the path's branches (1.12.2 PathGen#averageGroundLevel, which was its length). */
    int length() {
        return this.averageGroundLevel;
    }

    private static int cropId(Block crop) {
        return crop == Blocks.CARROTS ? 1 : crop == Blocks.POTATOES ? 2 : 0;
    }

    private static Block cropById(int id) {
        return id == 1 ? Blocks.CARROTS : id == 2 ? Blocks.POTATOES : Blocks.WHEAT;
    }

    /** 1.12.2 Field2#getRandomCrop: carrots and potatoes one time in five each, wheat otherwise. */
    static Block randomCrop(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> Blocks.CARROTS;
            case 1 -> Blocks.POTATOES;
            default -> Blocks.WHEAT;
        };
    }

    // ------------------------------------------------------------------ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pivot) {
        if (this.kind == Kind.PATH) {
            path(level, box);
            return;
        }
        if (!settle(level, box)) {
            return;
        }
        switch (this.kind) {
            case WELL -> well(level, box);
            case TORCH -> torchPost(level, box);
            case FIELD -> field(level, random, box);
            case CROP_FIELD -> cropField(level, random, box);
            case HOUSE, WOOD_HUT -> house(level, random, box);
            default -> {
            }
        }
    }

    /** How far above its floor each kind's box ends once settled (1.12.2 "averageGroundLevel - maxY + n - 1"). */
    private int settleHeight() {
        return switch (this.kind) {
            case WELL -> 3;
            case TORCH -> 4 - 1;
            case FIELD -> 7 - 1;
            case CROP_FIELD -> 4 - 1;
            default -> 9 - 1;
        };
    }

    /** 1.12.2: on first build, move the box so it sits on the average surface height (false: nothing to stand on here). */
    private boolean settle(WorldGenLevel level, BoundingBox box) {
        if (this.averageGroundLevel < 0) {
            this.averageGroundLevel = averageGroundLevel(level, box);
            if (this.averageGroundLevel < 0) {
                return false;
            }
            this.boundingBox.move(0, this.averageGroundLevel - this.boundingBox.maxY() + settleHeight(), 0);
        }
        return true;
    }

    /** 1.12.2 StructureComponentVillage#getAverageGroundLevel: over the part of the piece in this chunk. */
    private int averageGroundLevel(WorldGenLevel level, BoundingBox box) {
        int total = 0;
        int count = 0;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); ++z) {
            for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); ++x) {
                pos.set(x, 64, z);
                if (box.isInside(pos)) {
                    total += level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    count++;
                }
            }
        }
        return count == 0 ? -1 : total / count;
    }

    /** 1.12.2 clearCurrentPositionBlocksUpwards. */
    private void clearUpwards(WorldGenLevel level, int x, int y, int z, BoundingBox box) {
        final BlockPos.MutableBlockPos pos = getWorldPos(x, y, z);
        if (box.isInside(pos)) {
            while (!level.isEmptyBlock(pos) && pos.getY() < level.getMaxBuildHeight() - 1) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                pos.move(Direction.UP);
            }
        }
    }

    private BlockState torch(Direction facing) {
        return GCBlocks.GLOWSTONE_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING, facing);
    }

    /** 1.12.2 PathGen: spruce planks over the Moon turf (or air) at the surface of each column. */
    private void path(WorldGenLevel level, BoundingBox box) {
        final BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); ++x) {
            for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); ++z) {
                final BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                if (box.isInside(new BlockPos(x, 64, z))) {
                    final BlockState state = level.getBlockState(pos);
                    if (state.is(GCBlocks.MOON_TURF.get()) || state.isAir()) {
                        level.setBlock(pos, planks, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** 1.12.2 Well: a 12-deep shaft of water walled in tin decoration blocks under a roof on four dark oak posts. */
    private void well(WorldGenLevel level, BoundingBox box) {
        final BlockState deco2 = GCBlocks.TIN_DECORATION_BLOCK_2.get().defaultBlockState();
        final BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
        generateBox(level, box, 1, 0, 1, 4, 12, 4, deco2, Blocks.WATER.defaultBlockState(), false);
        placeBlock(level, Blocks.AIR.defaultBlockState(), 2, 12, 2, box);
        placeBlock(level, Blocks.AIR.defaultBlockState(), 3, 12, 2, box);
        placeBlock(level, Blocks.AIR.defaultBlockState(), 2, 12, 3, box);
        placeBlock(level, Blocks.AIR.defaultBlockState(), 3, 12, 3, box);
        for (int[] post : new int[][]{{1, 1}, {4, 1}, {1, 4}, {4, 4}}) {
            placeBlock(level, fence, post[0], 13, post[1], box);
            placeBlock(level, fence, post[0], 14, post[1], box);
        }
        generateBox(level, box, 1, 15, 1, 4, 15, 4, deco2, deco2, false);
        for (int z = 0; z <= 5; ++z) {
            for (int x = 0; x <= 5; ++x) {
                if (x == 0 || x == 5 || z == 0 || z == 5) {
                    placeBlock(level, Blocks.SPRUCE_PLANKS.defaultBlockState(), x, 11, z, box);
                    clearUpwards(level, x, 12, z, box);
                }
            }
        }
    }

    /** 1.12.2 Torch: a dark oak post with a block of black wool and four glowstone torches on top. */
    private void torchPost(WorldGenLevel level, BoundingBox box) {
        final Direction o = getOrientation();
        generateBox(level, box, 0, 0, 0, 2, 3, 1, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
        placeBlock(level, Blocks.DARK_OAK_FENCE.defaultBlockState(), 1, 0, 0, box);
        placeBlock(level, Blocks.DARK_OAK_FENCE.defaultBlockState(), 1, 1, 0, box);
        placeBlock(level, Blocks.DARK_OAK_FENCE.defaultBlockState(), 1, 2, 0, box);
        placeBlock(level, Blocks.BLACK_WOOL.defaultBlockState(), 1, 3, 0, box);
        final boolean flag = o == Direction.EAST || o == Direction.NORTH;
        placeBlock(level, torch(o.getClockWise()), flag ? 2 : 0, 3, 0, box);
        placeBlock(level, torch(o), 1, 3, 1, box);
        placeBlock(level, torch(o.getCounterClockWise()), flag ? 0 : 2, 3, 0, box);
        placeBlock(level, torch(o.getOpposite()), 1, 3, -1, box);
    }

    /** The log border and water channels both fields share (1.12.2 Field and Field2). */
    private void fieldFrame(WorldGenLevel level, BoundingBox box, BlockState soil) {
        final BlockState log = Blocks.OAK_LOG.defaultBlockState();
        generateBox(level, box, 0, 1, 0, 12, 4, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
        for (int x : new int[]{1, 4, 7, 10}) {
            generateBox(level, box, x, 0, 1, x + 1, 0, 7, soil, soil, false);
        }
        generateBox(level, box, 0, 0, 0, 0, 0, 8, log, log, false);
        generateBox(level, box, 6, 0, 0, 6, 0, 8, log, log, false);
        generateBox(level, box, 12, 0, 0, 12, 0, 8, log, log, false);
        generateBox(level, box, 1, 0, 0, 11, 0, 0, log, log, false);
        generateBox(level, box, 1, 0, 8, 11, 0, 8, log, log, false);
        generateBox(level, box, 3, 0, 1, 3, 0, 7, Blocks.WATER.defaultBlockState(), Blocks.WATER.defaultBlockState(), false);
        generateBox(level, box, 9, 0, 1, 9, 0, 7, Blocks.WATER.defaultBlockState(), Blocks.WATER.defaultBlockState(), false);
    }

    private void fieldBase(WorldGenLevel level, BoundingBox box) {
        for (int z = 0; z < 9; ++z) {
            for (int x = 0; x < 13; ++x) {
                clearUpwards(level, x, 4, z, box);
                fillColumnDown(level, Blocks.DIRT.defaultBlockState(), x, -1, z, box);
            }
        }
    }

    /** 1.12.2 Field: oak, spruce and birch saplings (one spot in three) on dirt, raised three blocks above the ground. */
    private void field(WorldGenLevel level, RandomSource random, BoundingBox box) {
        fieldFrame(level, box, Blocks.DIRT.defaultBlockState());
        final Block[] saplings = {Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING};
        for (int z = 1; z <= 7; ++z) {
            for (int x = 1; x < 12; x++) {
                if (x % 3 != 0 && random.nextInt(3) == 0) {
                    placeBlock(level, saplings[Mth.nextInt(random, 0, 2)].defaultBlockState(), x, 1, z, box);
                }
            }
        }
        fieldBase(level, box);
    }

    /** 1.12.2 Field2: four rows of half-grown wheat, carrots or potatoes on farmland. */
    private void cropField(WorldGenLevel level, RandomSource random, BoundingBox box) {
        fieldFrame(level, box, Blocks.FARMLAND.defaultBlockState());
        for (int z = 1; z <= 7; ++z) {
            for (int row = 0; row < 4; row++) {
                final CropBlock crop = (CropBlock) this.crops[row];
                placeBlock(level, crop.getStateForAge(Mth.nextInt(random, 2, 7)), row * 3 + 1, 1, z, box);
                placeBlock(level, crop.getStateForAge(Mth.nextInt(random, 2, 7)), row * 3 + 2, 1, z, box);
            }
        }
        fieldBase(level, box);
    }

    /**
     * 1.12.2 House / WoodHut: a round domed house of tin decoration blocks, its walls reaching 8 blocks into the ground,
     * with four doorways, glowstone torches, a glass skylight, and up to four Alien Villagers.
     */
    private void house(WorldGenLevel level, RandomSource random, BoundingBox box) {
        final BlockState deco1 = GCBlocks.TIN_DECORATION_BLOCK_1.get().defaultBlockState();
        final BlockState deco2 = GCBlocks.TIN_DECORATION_BLOCK_2.get().defaultBlockState();
        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState glass = Blocks.GLASS.defaultBlockState();
        final Direction o = getOrientation();
        generateAirBox(level, box, 3, 0, 3, 13, 9, 13);
        generateAirBox(level, box, 5, 0, 2, 11, 9, 14);
        generateAirBox(level, box, 2, 0, 5, 14, 9, 11);
        for (int i = 3; i <= 13; i++) {
            for (int j = 3; j <= 13; j++) {
                placeBlock(level, deco1, i, 0, j, box);
            }
        }
        for (int i = 5; i <= 11; i++) {
            for (int j = 2; j <= 14; j++) {
                placeBlock(level, deco1, i, 0, j, box);
            }
        }
        for (int i = 2; i <= 14; i++) {
            for (int j = 5; j <= 11; j++) {
                placeBlock(level, deco1, i, 0, j, box);
            }
        }
        int yLevel = 0;
        for (yLevel = -8; yLevel < 4; yLevel++) {
            placeBlock(level, deco2, 4, yLevel, 2, box);
            placeBlock(level, deco2, 3, yLevel, 2, box);
            placeBlock(level, deco2, 2, yLevel, 3, box);
            placeBlock(level, deco2, 2, yLevel, 4, box);
            placeBlock(level, deco2, 1, yLevel, 5, box);
            placeBlock(level, deco2, 1, yLevel, 6, box);
            placeBlock(level, deco2, 1, yLevel, 7, box);
            placeBlock(level, yLevel <= 1 ? deco2 : air, 1, yLevel, 8, box);
            placeBlock(level, deco2, 1, yLevel, 9, box);
            placeBlock(level, deco2, 1, yLevel, 10, box);
            placeBlock(level, deco2, 1, yLevel, 11, box);
            placeBlock(level, deco2, 2, yLevel, 12, box);
            placeBlock(level, deco2, 2, yLevel, 13, box);
            placeBlock(level, deco2, 3, yLevel, 14, box);
            placeBlock(level, deco2, 4, yLevel, 14, box);
            placeBlock(level, deco2, 5, yLevel, 15, box);
            placeBlock(level, deco2, 6, yLevel, 15, box);
            placeBlock(level, deco2, 7, yLevel, 15, box);
            placeBlock(level, yLevel <= 1 ? deco2 : air, 8, yLevel, 15, box);
            placeBlock(level, deco2, 9, yLevel, 15, box);
            placeBlock(level, deco2, 10, yLevel, 15, box);
            placeBlock(level, deco2, 11, yLevel, 15, box);
            placeBlock(level, deco2, 12, yLevel, 14, box);
            placeBlock(level, deco2, 13, yLevel, 14, box);
            placeBlock(level, deco2, 14, yLevel, 13, box);
            placeBlock(level, deco2, 14, yLevel, 12, box);
            placeBlock(level, deco2, 15, yLevel, 11, box);
            placeBlock(level, deco2, 15, yLevel, 10, box);
            placeBlock(level, deco2, 15, yLevel, 9, box);
            placeBlock(level, yLevel <= 1 ? deco2 : air, 15, yLevel, 8, box);
            placeBlock(level, deco2, 15, yLevel, 7, box);
            placeBlock(level, deco2, 15, yLevel, 6, box);
            placeBlock(level, deco2, 15, yLevel, 5, box);
            placeBlock(level, deco2, 14, yLevel, 4, box);
            placeBlock(level, deco2, 14, yLevel, 3, box);
            placeBlock(level, deco2, 13, yLevel, 2, box);
            placeBlock(level, deco2, 12, yLevel, 2, box);
            placeBlock(level, deco2, 11, yLevel, 1, box);
            placeBlock(level, deco2, 10, yLevel, 1, box);
            placeBlock(level, deco2, 9, yLevel, 1, box);
            placeBlock(level, yLevel <= 1 ? deco2 : air, 8, yLevel, 1, box);
            placeBlock(level, deco2, 7, yLevel, 1, box);
            placeBlock(level, deco2, 6, yLevel, 1, box);
            placeBlock(level, deco2, 5, yLevel, 1, box);
        }
        yLevel = 4;
        placeBlock(level, deco2, 4, yLevel, 2, box);
        placeBlock(level, deco2, 3, yLevel, 3, box);
        placeBlock(level, deco2, 2, yLevel, 4, box);
        placeBlock(level, deco2, 1, yLevel, 5, box);
        placeBlock(level, deco2, 1, yLevel, 6, box);
        placeBlock(level, deco2, 1, yLevel, 7, box);
        placeBlock(level, deco2, 1, yLevel, 8, box);
        placeBlock(level, deco2, 1, yLevel, 9, box);
        placeBlock(level, deco2, 1, yLevel, 10, box);
        placeBlock(level, deco2, 1, yLevel, 11, box);
        placeBlock(level, deco2, 2, yLevel, 12, box);
        placeBlock(level, deco2, 3, yLevel, 13, box);
        placeBlock(level, deco2, 4, yLevel, 14, box);
        placeBlock(level, deco2, 5, yLevel, 15, box);
        placeBlock(level, deco2, 6, yLevel, 15, box);
        placeBlock(level, deco2, 7, yLevel, 15, box);
        placeBlock(level, deco2, 8, yLevel, 15, box);
        placeBlock(level, deco2, 9, yLevel, 15, box);
        placeBlock(level, deco2, 10, yLevel, 15, box);
        placeBlock(level, deco2, 11, yLevel, 15, box);
        placeBlock(level, deco2, 12, yLevel, 14, box);
        placeBlock(level, deco2, 13, yLevel, 13, box);
        placeBlock(level, deco2, 14, yLevel, 12, box);
        placeBlock(level, deco2, 15, yLevel, 11, box);
        placeBlock(level, deco2, 15, yLevel, 10, box);
        placeBlock(level, deco2, 15, yLevel, 9, box);
        placeBlock(level, deco2, 15, yLevel, 8, box);
        placeBlock(level, deco2, 15, yLevel, 7, box);
        placeBlock(level, deco2, 15, yLevel, 6, box);
        placeBlock(level, deco2, 15, yLevel, 5, box);
        placeBlock(level, deco2, 14, yLevel, 4, box);
        placeBlock(level, deco2, 13, yLevel, 3, box);
        placeBlock(level, deco2, 12, yLevel, 2, box);
        placeBlock(level, deco2, 11, yLevel, 1, box);
        placeBlock(level, deco2, 10, yLevel, 1, box);
        placeBlock(level, deco2, 9, yLevel, 1, box);
        placeBlock(level, deco2, 8, yLevel, 1, box);
        placeBlock(level, deco2, 7, yLevel, 1, box);
        placeBlock(level, deco2, 6, yLevel, 1, box);
        placeBlock(level, deco2, 5, yLevel, 1, box);
        placeBlock(level, torch(o), 8, yLevel, 2, box);
        placeBlock(level, torch(this.kind == Kind.WOOD_HUT ? o.getCounterClockWise() : o.getClockWise()), 14, yLevel, 8, box);
        placeBlock(level, torch(o.getOpposite()), 8, yLevel, 14, box);
        placeBlock(level, torch(this.kind == Kind.WOOD_HUT ? o.getClockWise() : o.getCounterClockWise()), 2, yLevel, 8, box);
        yLevel = 5;
        placeBlock(level, deco2, 5, yLevel, 2, box);
        placeBlock(level, deco2, 4, yLevel, 2, box);
        placeBlock(level, deco2, 3, yLevel, 3, box);
        placeBlock(level, deco2, 2, yLevel, 4, box);
        placeBlock(level, deco2, 2, yLevel, 5, box);
        placeBlock(level, deco2, 1, yLevel, 6, box);
        placeBlock(level, deco2, 1, yLevel, 7, box);
        placeBlock(level, deco2, 1, yLevel, 8, box);
        placeBlock(level, deco2, 1, yLevel, 9, box);
        placeBlock(level, deco2, 1, yLevel, 10, box);
        placeBlock(level, deco2, 2, yLevel, 11, box);
        placeBlock(level, deco2, 2, yLevel, 12, box);
        placeBlock(level, deco2, 3, yLevel, 13, box);
        placeBlock(level, deco2, 4, yLevel, 14, box);
        placeBlock(level, deco2, 5, yLevel, 14, box);
        placeBlock(level, deco2, 6, yLevel, 15, box);
        placeBlock(level, deco2, 7, yLevel, 15, box);
        placeBlock(level, deco2, 8, yLevel, 15, box);
        placeBlock(level, deco2, 9, yLevel, 15, box);
        placeBlock(level, deco2, 10, yLevel, 15, box);
        placeBlock(level, deco2, 11, yLevel, 14, box);
        placeBlock(level, deco2, 12, yLevel, 14, box);
        placeBlock(level, deco2, 13, yLevel, 13, box);
        placeBlock(level, deco2, 14, yLevel, 12, box);
        placeBlock(level, deco2, 14, yLevel, 11, box);
        placeBlock(level, deco2, 15, yLevel, 10, box);
        placeBlock(level, deco2, 15, yLevel, 9, box);
        placeBlock(level, deco2, 15, yLevel, 8, box);
        placeBlock(level, deco2, 15, yLevel, 7, box);
        placeBlock(level, deco2, 15, yLevel, 6, box);
        placeBlock(level, deco2, 14, yLevel, 5, box);
        placeBlock(level, deco2, 14, yLevel, 4, box);
        placeBlock(level, deco2, 13, yLevel, 3, box);
        placeBlock(level, deco2, 12, yLevel, 2, box);
        placeBlock(level, deco2, 11, yLevel, 2, box);
        placeBlock(level, deco2, 10, yLevel, 1, box);
        placeBlock(level, deco2, 9, yLevel, 1, box);
        placeBlock(level, deco2, 8, yLevel, 1, box);
        placeBlock(level, deco2, 7, yLevel, 1, box);
        placeBlock(level, deco2, 6, yLevel, 1, box);
        yLevel = 6;
        placeBlock(level, deco2, 4, yLevel, 3, box);
        placeBlock(level, deco2, 3, yLevel, 4, box);
        placeBlock(level, deco2, 2, yLevel, 5, box);
        placeBlock(level, deco2, 2, yLevel, 6, box);
        placeBlock(level, deco2, 2, yLevel, 7, box);
        placeBlock(level, deco2, 2, yLevel, 8, box);
        placeBlock(level, deco2, 2, yLevel, 9, box);
        placeBlock(level, deco2, 2, yLevel, 10, box);
        placeBlock(level, deco2, 2, yLevel, 11, box);
        placeBlock(level, deco2, 3, yLevel, 12, box);
        placeBlock(level, deco2, 4, yLevel, 13, box);
        placeBlock(level, deco2, 5, yLevel, 14, box);
        placeBlock(level, deco2, 6, yLevel, 14, box);
        placeBlock(level, deco2, 7, yLevel, 14, box);
        placeBlock(level, deco2, 8, yLevel, 14, box);
        placeBlock(level, deco2, 9, yLevel, 14, box);
        placeBlock(level, deco2, 10, yLevel, 14, box);
        placeBlock(level, deco2, 11, yLevel, 14, box);
        placeBlock(level, deco2, 12, yLevel, 13, box);
        placeBlock(level, deco2, 13, yLevel, 12, box);
        placeBlock(level, deco2, 14, yLevel, 11, box);
        placeBlock(level, deco2, 14, yLevel, 10, box);
        placeBlock(level, deco2, 14, yLevel, 9, box);
        placeBlock(level, deco2, 14, yLevel, 8, box);
        placeBlock(level, deco2, 14, yLevel, 7, box);
        placeBlock(level, deco2, 14, yLevel, 6, box);
        placeBlock(level, deco2, 14, yLevel, 5, box);
        placeBlock(level, deco2, 13, yLevel, 4, box);
        placeBlock(level, deco2, 12, yLevel, 3, box);
        placeBlock(level, deco2, 11, yLevel, 2, box);
        placeBlock(level, deco2, 10, yLevel, 2, box);
        placeBlock(level, deco2, 9, yLevel, 2, box);
        placeBlock(level, deco2, 8, yLevel, 2, box);
        placeBlock(level, deco2, 7, yLevel, 2, box);
        placeBlock(level, deco2, 6, yLevel, 2, box);
        placeBlock(level, deco2, 5, yLevel, 2, box);
        yLevel = 7;
        placeBlock(level, deco2, 6, yLevel, 3, box);
        placeBlock(level, deco2, 5, yLevel, 3, box);
        placeBlock(level, deco2, 4, yLevel, 4, box);
        placeBlock(level, deco2, 3, yLevel, 5, box);
        placeBlock(level, deco2, 3, yLevel, 6, box);
        placeBlock(level, deco2, 2, yLevel, 7, box);
        placeBlock(level, deco2, 2, yLevel, 8, box);
        placeBlock(level, deco2, 2, yLevel, 9, box);
        placeBlock(level, deco2, 3, yLevel, 10, box);
        placeBlock(level, deco2, 3, yLevel, 11, box);
        placeBlock(level, deco2, 4, yLevel, 12, box);
        placeBlock(level, deco2, 5, yLevel, 13, box);
        placeBlock(level, deco2, 6, yLevel, 13, box);
        placeBlock(level, deco2, 7, yLevel, 14, box);
        placeBlock(level, deco2, 8, yLevel, 14, box);
        placeBlock(level, deco2, 9, yLevel, 14, box);
        placeBlock(level, deco2, 10, yLevel, 13, box);
        placeBlock(level, deco2, 11, yLevel, 13, box);
        placeBlock(level, deco2, 12, yLevel, 12, box);
        placeBlock(level, deco2, 13, yLevel, 11, box);
        placeBlock(level, deco2, 13, yLevel, 10, box);
        placeBlock(level, deco2, 14, yLevel, 9, box);
        placeBlock(level, deco2, 14, yLevel, 8, box);
        placeBlock(level, deco2, 14, yLevel, 7, box);
        placeBlock(level, deco2, 13, yLevel, 6, box);
        placeBlock(level, deco2, 13, yLevel, 5, box);
        placeBlock(level, deco2, 12, yLevel, 4, box);
        placeBlock(level, deco2, 11, yLevel, 3, box);
        placeBlock(level, deco2, 10, yLevel, 3, box);
        placeBlock(level, deco2, 9, yLevel, 2, box);
        placeBlock(level, deco2, 8, yLevel, 2, box);
        placeBlock(level, deco2, 7, yLevel, 2, box);
        yLevel = 8;
        placeBlock(level, deco2, 6, yLevel, 4, box);
        placeBlock(level, deco2, 5, yLevel, 4, box);
        placeBlock(level, deco2, 4, yLevel, 5, box);
        placeBlock(level, deco2, 4, yLevel, 6, box);
        placeBlock(level, deco2, 3, yLevel, 7, box);
        placeBlock(level, deco2, 3, yLevel, 8, box);
        placeBlock(level, deco2, 3, yLevel, 9, box);
        placeBlock(level, deco2, 4, yLevel, 10, box);
        placeBlock(level, deco2, 4, yLevel, 11, box);
        placeBlock(level, deco2, 5, yLevel, 12, box);
        placeBlock(level, deco2, 6, yLevel, 12, box);
        placeBlock(level, deco2, 7, yLevel, 13, box);
        placeBlock(level, deco2, 8, yLevel, 13, box);
        placeBlock(level, deco2, 9, yLevel, 13, box);
        placeBlock(level, deco2, 10, yLevel, 12, box);
        placeBlock(level, deco2, 11, yLevel, 12, box);
        placeBlock(level, deco2, 12, yLevel, 11, box);
        placeBlock(level, deco2, 12, yLevel, 10, box);
        placeBlock(level, deco2, 13, yLevel, 9, box);
        placeBlock(level, deco2, 13, yLevel, 8, box);
        placeBlock(level, deco2, 13, yLevel, 7, box);
        placeBlock(level, deco2, 12, yLevel, 6, box);
        placeBlock(level, deco2, 12, yLevel, 5, box);
        placeBlock(level, deco2, 11, yLevel, 4, box);
        placeBlock(level, deco2, 10, yLevel, 4, box);
        placeBlock(level, deco2, 9, yLevel, 3, box);
        placeBlock(level, deco2, 8, yLevel, 3, box);
        placeBlock(level, deco2, 7, yLevel, 3, box);
        placeBlock(level, deco2, 5, yLevel, 5, box);
        placeBlock(level, deco2, 5, yLevel, 11, box);
        placeBlock(level, deco2, 11, yLevel, 11, box);
        placeBlock(level, deco2, 11, yLevel, 5, box);
        placeBlock(level, deco2, 4, yLevel, 7, box);
        placeBlock(level, deco2, 4, yLevel, 8, box);
        placeBlock(level, deco2, 4, yLevel, 9, box);
        placeBlock(level, deco2, 7, yLevel, 12, box);
        placeBlock(level, deco2, 8, yLevel, 12, box);
        placeBlock(level, deco2, 9, yLevel, 12, box);
        placeBlock(level, deco2, 9, yLevel, 4, box);
        placeBlock(level, deco2, 8, yLevel, 4, box);
        placeBlock(level, deco2, 7, yLevel, 4, box);
        placeBlock(level, deco2, 12, yLevel, 7, box);
        placeBlock(level, deco2, 12, yLevel, 8, box);
        placeBlock(level, deco2, 12, yLevel, 9, box);
        yLevel = 9;
        for (int i = 5; i <= 11; i++) {
            for (int j = 5; j <= 11; j++) {
                if (!(j == 5 && i == 5 || j == 5 && i == 11 || j == 11 && i == 5 || j == 11 && i == 11)) {
                    if (i >= 7 && i <= 9 && j >= 7 && j <= 9) {
                        placeBlock(level, glass, i, yLevel, j, box);
                    } else {
                        placeBlock(level, deco2, i, yLevel, j, box);
                    }
                }
            }
        }
        spawnVillagers(level, random, box, 6, 5, 6, 4);
    }

    /** 1.12.2 spawnVillagers: up to {@code count} Alien Villagers in a row, each a block or so off. */
    private void spawnVillagers(WorldGenLevel level, RandomSource random, BoundingBox box, int x, int y, int z, int count) {
        for (int i = this.villagersSpawned; i < count; ++i) {
            final int wx = getWorldX(x + i, z) + random.nextInt(3) - 1;
            final int wy = getWorldY(y);
            final int wz = getWorldZ(x + i, z) + random.nextInt(3) - 1;
            final BlockPos pos = new BlockPos(wx, wy, wz);
            if (!box.isInside(pos)) {
                break;
            }
            ++this.villagersSpawned;
            final AlienVillagerEntity villager = GCEntities.ALIEN_VILLAGER.get().create(level.getLevel());
            if (villager != null) {
                villager.moveTo(wx + 0.5D, wy, wz + 0.5D, 0.0F, 0.0F);
                villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                level.addFreshEntityWithPassengers(villager);
            }
        }
    }
}
