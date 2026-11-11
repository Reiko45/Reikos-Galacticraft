/*
 * Room building derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.dungeon).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.dungeon;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.SpaceTorchBlock;
import io.github.reiko45.galacticraft.block.TreasureChestBlock;
import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * One piece of a dungeon: the surface crater, a corridor or one of the rooms (1.12.2 Room*, Corridor, EntranceCrater,
 * and on Mars RoomBossMars and RoomTreasureMars).
 */
public class DungeonPiece extends StructurePiece {
    /** 1.12.2 RoomTreasure.MOONCHEST: chests in Moon chest rooms and the Tier 1 treasure chest. */
    public static final ResourceKey<LootTable> MOON_CHEST = DungeonConfig.MOON.loot();

    public enum Kind {
        CRATER, ENTRANCE, CORRIDOR, EMPTY, SPAWNER, CHEST, BOSS, TREASURE
    }

    private final DungeonConfig config;
    private final Kind kind;
    private final Direction direction;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    /** Crater: the highest surface around it. */
    private final int surface;
    /** Boss room: where the treasure chest is. */
    @Nullable
    private final BlockPos chestPos;

    public DungeonPiece(DungeonConfig config, Dungeon.Node node, int surface, @Nullable BlockPos chestPos) {
        super(GCStructures.DUNGEON_PIECE.get(), 0, node.box());
        this.config = config;
        this.kind = node.kind();
        this.direction = node.direction();
        this.sizeX = node.sizeX();
        this.sizeY = node.sizeY();
        this.sizeZ = node.sizeZ();
        this.surface = surface;
        this.chestPos = chestPos;
        setOrientation(Direction.SOUTH); // 1.12.2 setCoordBaseMode(SOUTH): no rotation, block states mirrored north/south
    }

    public DungeonPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(GCStructures.DUNGEON_PIECE.get(), tag);
        this.config = DungeonConfig.values()[Math.min(tag.getInt("Config"), DungeonConfig.values().length - 1)];
        this.kind = Kind.values()[tag.getInt("Kind")];
        this.direction = Direction.from3DDataValue(tag.getInt("Direction"));
        this.sizeX = tag.getInt("SizeX");
        this.sizeY = tag.getInt("SizeY");
        this.sizeZ = tag.getInt("SizeZ");
        this.surface = tag.getInt("Surface");
        this.chestPos = tag.contains("ChestPos") ? NbtUtils.readBlockPos(tag, "ChestPos").orElse(null) : null;
        setOrientation(Direction.SOUTH);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Config", this.config.ordinal());
        tag.putInt("Kind", this.kind.ordinal());
        tag.putInt("Direction", this.direction.get3DDataValue());
        tag.putInt("SizeX", this.sizeX);
        tag.putInt("SizeY", this.sizeY);
        tag.putInt("SizeZ", this.sizeZ);
        tag.putInt("Surface", this.surface);
        if (this.chestPos != null) {
            tag.put("ChestPos", NbtUtils.writeBlockPos(this.chestPos));
        }
    }

    public Kind kind() {
        return this.kind;
    }

    public DungeonConfig config() {
        return this.config;
    }

    private BlockState brick() {
        return this.config.brick();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        switch (this.kind) {
            case CRATER -> crater(level, chunkBox);
            case ENTRANCE -> entrance(level, chunkBox);
            case CORRIDOR -> corridor(level, chunkBox);
            case EMPTY -> room(level, chunkBox, this.config.hallHeight(), null);
            case SPAWNER -> {
                room(level, chunkBox, this.config.hallHeight(), null);
                spawners(level, random, chunkBox);
            }
            case CHEST -> {
                room(level, chunkBox, this.config.hallHeight(), null);
                chest(level, random, chunkBox);
            }
            case BOSS -> {
                if (this.config == DungeonConfig.MARS) {
                    marsBossRoom(level, random, chunkBox);
                } else {
                    room(level, chunkBox, 3, Blocks.LAVA.defaultBlockState());
                }
                bossSpawner(level, chunkBox);
            }
            case TREASURE -> {
                room(level, chunkBox, 3, Blocks.GLOWSTONE.defaultBlockState());
                treasure(level, random, chunkBox);
            }
        }
    }

    /** 1.12.2 EntranceCrater: a funnel from just above the surface down towards the entrance room. */
    private void crater(WorldGenLevel level, BoundingBox chunkBox) {
        final int range = Dungeon.CRATER_RANGE;
        final int centreX = this.boundingBox.minX() + range;
        final int centreZ = this.boundingBox.minZ() + range;
        final int top = this.surface + 2;
        final int bottom = this.boundingBox.minY() + 2;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = -range; i < range; i++) {
            for (int k = -range; k < range; k++) {
                final double distance = (i / 20.0D) * (i / 20.0D) + (k / 20.0D) * (k / 20.0D);
                final int depth = (int) Math.abs(0.5D / (distance + 0.00001D));
                int removed = 0;
                for (int y = top; y >= bottom && removed <= depth; y--) {
                    pos.set(centreX + i, y, centreZ + k);
                    if (chunkBox.isInside(pos)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                    removed++;
                }
            }
        }
    }

    /** 1.12.2 RoomEntrance: brick walls and floor, open to the shaft above. */
    private void entrance(WorldGenLevel level, BoundingBox chunkBox) {
        for (int i = 0; i <= this.sizeX; i++) {
            for (int j = 0; j <= this.sizeY; j++) {
                for (int k = 0; k <= this.sizeZ; k++) {
                    final boolean wall = i == 0 || i == this.sizeX || j == 0 || k == 0 || k == this.sizeZ;
                    placeBlock(level, wall ? brick() : Blocks.AIR.defaultBlockState(), i, j, k, chunkBox);
                }
            }
        }
    }

    /** 1.12.2 Corridor: brick tube with (unlit) wall torches every 4 blocks. */
    private void corridor(WorldGenLevel level, BoundingBox chunkBox) {
        final int lenX = this.boundingBox.getXSpan();
        final int lenY = this.boundingBox.getYSpan();
        final int lenZ = this.boundingBox.getZSpan();
        final boolean alongZ = this.direction.getAxis() == Direction.Axis.Z;
        for (int i = 0; i < lenX; i++) {
            for (int j = 0; j < lenY; j++) {
                for (int k = 0; k < lenZ; k++) {
                    if (alongZ && (i == 0 || i == lenX - 1) || j == 0 || j == lenY - 1 || !alongZ && (k == 0 || k == lenZ - 1)) {
                        placeBlock(level, brick(), i, j, k, chunkBox);
                        continue;
                    }
                    if (j == lenY - 2) {
                        if (alongZ && (k + 1) % 4 == 0 && (i == 1 || i == lenX - 2)) {
                            placeBlock(level, torch(i == 1 ? Direction.EAST : Direction.WEST), i, j, k, chunkBox);
                            continue;
                        }
                        if (!alongZ && (i + 1) % 4 == 0 && (k == 1 || k == lenZ - 2)) {
                            placeBlock(level, torch(k == 1 ? Direction.SOUTH : Direction.NORTH), i, j, k, chunkBox);
                            continue;
                        }
                    }
                    placeBlock(level, Blocks.AIR.defaultBlockState(), i, j, k, chunkBox);
                }
            }
        }
    }

    private static BlockState torch(Direction facing) {
        return GCBlocks.UNLIT_WALL_TORCH.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing).setValue(SpaceTorchBlock.LIT, false);
    }

    /**
     * 1.12.2 RoomEmpty / RoomBoss / RoomTreasure walls: a brick box with a doorway (3 wide, {@code doorHeight} high) where
     * the corridor comes in. Boss and treasure rooms fill their four corner columns with lava or glowstone.
     */
    private void room(WorldGenLevel level, BoundingBox chunkBox, int doorHeight, @Nullable BlockState corners) {
        final boolean alongZ = this.direction.getAxis() == Direction.Axis.Z;
        final int doorStart = (alongZ ? this.boundingBox.maxX() - this.boundingBox.minX() : this.boundingBox.maxZ() - this.boundingBox.minZ()) / 2 - 1;
        final int doorEnd = doorStart + 2;
        for (int i = 0; i <= this.sizeX; i++) {
            for (int j = 0; j <= this.sizeY; j++) {
                for (int k = 0; k <= this.sizeZ; k++) {
                    BlockState state = Blocks.AIR.defaultBlockState();
                    if (i == 0 || i == this.sizeX || j == 0 || j == this.sizeY || k == 0 || k == this.sizeZ) {
                        final int across = alongZ ? i : k;
                        boolean door = across > doorStart && across <= doorEnd && j > 0 && j < doorHeight;
                        door &= this.direction == Direction.SOUTH && k == 0 || this.direction == Direction.NORTH && k == this.sizeZ
                                || this.direction == Direction.EAST && i == 0 || this.direction == Direction.WEST && i == this.sizeX;
                        state = door ? Blocks.AIR.defaultBlockState() : brick();
                    } else if (corners != null && (i == 1 || i == this.sizeX - 1) && (k == 1 || k == this.sizeZ - 1)) {
                        state = corners;
                    } else if (this.kind == Kind.BOSS && bossBars(i, j, k)) {
                        state = Blocks.IRON_BARS.defaultBlockState();
                    }
                    placeBlock(level, state, i, j, k, chunkBox);
                }
            }
        }
    }

    /**
     * 1.12.2 RoomBossMars: brick walls and floor with a doorway 2 high, a ceiling of brick with a scatter of glowstone
     * near the walls, and Creeper Eggs dotted round the edge of the floor.
     */
    private void marsBossRoom(WorldGenLevel level, RandomSource random, BoundingBox chunkBox) {
        final boolean alongZ = this.direction.getAxis() == Direction.Axis.Z;
        final int doorStart = (alongZ ? this.boundingBox.maxX() - this.boundingBox.minX() : this.boundingBox.maxZ() - this.boundingBox.minZ()) / 2 - 1;
        final int doorEnd = doorStart + 2;
        for (int i = 0; i <= this.sizeX; i++) {
            for (int j = 0; j <= this.sizeY; j++) {
                for (int k = 0; k <= this.sizeZ; k++) {
                    final BlockState state;
                    final boolean nearWall = i <= 2 || k <= 2 || i >= this.sizeX - 2 || k >= this.sizeZ - 2;
                    if (i == 0 || i == this.sizeX || j == 0 || k == 0 || k == this.sizeZ) {
                        final int across = alongZ ? i : k;
                        boolean door = across > doorStart && across <= doorEnd && j > 0 && j < 3;
                        door &= this.direction == Direction.SOUTH && k == 0 || this.direction == Direction.NORTH && k == this.sizeZ
                                || this.direction == Direction.EAST && i == 0 || this.direction == Direction.WEST && i == this.sizeX;
                        state = door ? Blocks.AIR.defaultBlockState() : brick();
                    } else if (j == this.sizeY) {
                        state = nearWall && random.nextInt(4) == 0 ? Blocks.GLOWSTONE.defaultBlockState() : brick();
                    } else if (j == 1 && nearWall && random.nextInt(6) == 0) {
                        state = GCBlocks.CREEPER_EGG.get().defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    placeBlock(level, state, i, j, k, chunkBox);
                }
            }
        }
    }

    /** 1.12.2 RoomBoss: iron bars ringing the lava pillars and, every third layer, the walls. */
    private boolean bossBars(int i, int j, int k) {
        final int x = this.sizeX;
        final int z = this.sizeZ;
        if (j % 3 == 0 && j >= 2 && (i == 1 || i == x - 1 || k == 1 || k == z - 1 || i == 2 && k == 2 || i == 2 && k == z - 2
                || i == x - 2 && k == 2 || i == x - 2 && k == z - 2)) {
            return true;
        }
        return i == 1 && k == 2 || i == 2 && k == 1 || i == 1 && k == z - 2 || i == 2 && k == z - 1
                || i == x - 1 && k == 2 || i == x - 2 && k == 1 || i == x - 1 && k == z - 2 || i == x - 2 && k == z - 1;
    }

    /** 1.12.2 RoomSpawner: cobwebs, and two evolved-mob spawners set into the floor. */
    private void spawners(WorldGenLevel level, RandomSource random, BoundingBox chunkBox) {
        for (int i = 1; i <= this.sizeX - 1; ++i) {
            for (int j = 1; j <= this.sizeY - 1; ++j) {
                for (int k = 1; k <= this.sizeZ - 1; ++k) {
                    if (random.nextFloat() < 0.05F) {
                        placeBlock(level, Blocks.COBWEB.defaultBlockState(), i, j, k, chunkBox);
                    }
                }
            }
        }
        for (int[] spot : new int[][]{{1, 1}, {this.sizeX - 1, this.sizeZ - 1}}) {
            placeBlock(level, Blocks.SPAWNER.defaultBlockState(), spot[0], 0, spot[1], chunkBox);
            final BlockPos pos = getWorldPos(spot[0], 0, spot[1]);
            if (chunkBox.isInside(pos) && level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
                spawner.setEntityId(mob(random), random);
            }
        }
    }

    /** 1.12.2 RoomSpawner#getMob. */
    private static EntityType<?> mob(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> GCEntities.EVOLVED_SPIDER.get();
            case 1 -> GCEntities.EVOLVED_CREEPER.get();
            case 2 -> GCEntities.EVOLVED_SKELETON.get();
            default -> GCEntities.EVOLVED_ZOMBIE.get();
        };
    }

    /** 1.12.2 RoomChest: a chest of the planet's dungeon loot in the middle. */
    private void chest(WorldGenLevel level, RandomSource random, BoundingBox chunkBox) {
        placeBlock(level, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, this.direction.getOpposite()), this.sizeX / 2, 1, this.sizeZ / 2, chunkBox);
        final BlockPos pos = getWorldPos(this.sizeX / 2, 1, this.sizeZ / 2);
        if (chunkBox.isInside(pos)) {
            RandomizableContainer.setBlockEntityLootTable(level, random, pos, this.config.loot());
        }
    }

    /** 1.12.2 RoomBoss: the boss spawner in the middle of the floor, told the room's extent and where the treasure is. */
    private void bossSpawner(WorldGenLevel level, BoundingBox chunkBox) {
        final BlockPos pos = getWorldPos(this.sizeX / 2, 1, this.sizeZ / 2);
        if (chunkBox.isInside(pos)) {
            level.setBlock(pos, this.config.bossSpawner().defaultBlockState(), 2);
            if (level.getBlockEntity(pos) instanceof DungeonSpawnerBlockEntity spawner) {
                spawner.setRoom(new BoundingBox(this.boundingBox.minX() + 1, this.boundingBox.minY() + 1, this.boundingBox.minZ() + 1,
                        this.boundingBox.minX() + this.sizeX - 1, this.boundingBox.minY() + this.sizeY - 1, this.boundingBox.minZ() + this.sizeZ - 1),
                        this.chestPos);
            }
        }
    }

    /** 1.12.2 RoomTreasure / RoomTreasureMars: the locked treasure chest of the planet's tier. */
    private void treasure(WorldGenLevel level, RandomSource random, BoundingBox chunkBox) {
        final BlockPos pos = getWorldPos(this.sizeX / 2, 1, this.sizeZ / 2);
        if (chunkBox.isInside(pos)) {
            level.setBlock(pos, this.config.treasureChest().defaultBlockState()
                    .setValue(TreasureChestBlock.FACING, this.direction.getOpposite()), 2);
            if (level.getBlockEntity(pos) instanceof TreasureChestBlockEntity chest) {
                chest.setLootTable(this.config.loot(), random.nextLong());
            }
        }
    }
}
