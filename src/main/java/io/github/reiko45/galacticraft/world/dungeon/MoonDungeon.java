/*
 * Dungeon layout and room building derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.dungeon).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.dungeon;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.registry.GCStructures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * The Moon dungeon (1.12.2 MapGenDungeon + DungeonStart + the room classes). A crater on the surface leads down a shaft
 * into an entrance room at y 25-37; from there corridors and rooms (empty, with mob spawners and cobwebs, or with a
 * chest) wander off until, after 10 to 25 pieces, a corridor ends in the boss room, whose low corridor leads to the
 * treasure room. A layout that doesn't end boss - corridor - treasure is retried (10 attempts, as in 1.12.2). One
 * dungeon per 704x704 block region (44x44 chunks), at a random chunk of it (structure set in datagen).
 */
public class MoonDungeon extends Structure {
    public static final MapCodec<MoonDungeon> CODEC = simpleCodec(MoonDungeon::new);

    // 1.12.2 ChunkProviderMoon DungeonConfiguration(moon dungeon brick, 25, 8, 16, 5, 6, RoomBoss, RoomTreasure)
    public static final int Y = 25;
    static final int HALL_MIN = 8;
    static final int HALL_MAX = 16;
    static final int HALL_HEIGHT = 5;
    static final int ROOM_HEIGHT = 6;
    static final int CRATER_RANGE = 16;

    public MoonDungeon(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return GCStructures.MOON_DUNGEON.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        final int x = context.chunkPos().getMinBlockX() + 2;
        final int z = context.chunkPos().getMinBlockZ() + 2;
        // 1.12.2 scanned each column of the crater for the surface; sample the generator's height map instead.
        int surface = 0;
        for (int dx = -CRATER_RANGE; dx <= CRATER_RANGE; dx += 4) {
            for (int dz = -CRATER_RANGE; dz <= CRATER_RANGE; dz += 4) {
                surface = Math.max(surface, context.chunkGenerator().getBaseHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState()));
            }
        }
        final int surfaceTop = surface;
        return Optional.of(new GenerationStub(new BlockPos(x, Y, z), builder -> generate(builder, context.random(), x, z, surfaceTop)));
    }

    /** One planned piece of the layout. */
    public record Node(MoonDungeonPiece.Kind kind, BoundingBox box, Direction direction, int sizeX, int sizeY, int sizeZ) {
    }

    private static void generate(StructurePiecesBuilder builder, RandomSource random, int x, int z, int surface) {
        final List<Node> layout = plan(random, x, z);
        BlockPos chestPos = null;
        if (layout.get(layout.size() - 1).kind() == MoonDungeonPiece.Kind.TREASURE) {
            final Node treasure = layout.get(layout.size() - 1);
            chestPos = new BlockPos(treasure.box().minX() + treasure.sizeX() / 2, Y + 1, treasure.box().minZ() + treasure.sizeZ() / 2);
        }
        for (Node node : layout) {
            builder.addPiece(new MoonDungeonPiece(node, node.kind() == MoonDungeonPiece.Kind.CRATER ? surface : 0,
                    node.kind() == MoonDungeonPiece.Kind.BOSS ? chestPos : null));
        }
    }

    /** 1.12.2 DungeonStart#buildComponent with the getNextPiece chain of each piece. */
    public static List<Node> plan(RandomSource random, int x, int z) {
        // EntranceCrater (its random size and direction are unused, but drawn as in 1.12.2)
        final int craterSizeX = random.nextInt(4) + 6;
        final int craterSizeZ = random.nextInt(4) + 6;
        final Direction craterDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        final Node crater = new Node(MoonDungeonPiece.Kind.CRATER, new BoundingBox(x - CRATER_RANGE, Y + 11, z - CRATER_RANGE,
                x + CRATER_RANGE, 150, z + CRATER_RANGE), craterDir, craterSizeX, 12, craterSizeZ);
        List<Node> pieces = new ArrayList<>();
        for (int attempt = 0; attempt < 10; attempt++) {
            pieces = new ArrayList<>();
            pieces.add(crater);
            Node next = entrance(random, x, z);
            while (next != null) {
                pieces.add(next);
                next = nextPiece(next, pieces, random);
            }
            final int attached = pieces.size() - 1;
            if (attached >= 3 && pieces.get(pieces.size() - 1).kind() == MoonDungeonPiece.Kind.TREASURE
                    && pieces.get(pieces.size() - 3).kind() == MoonDungeonPiece.Kind.BOSS) {
                return pieces;
            }
        }
        // No complete layout: keep the last attempt, like 1.12.2 (which logged it as a bug)
        return pieces;
    }

    private static Node entrance(RandomSource random, int x, int z) {
        final int sizeX = random.nextInt(4) + 6;
        final int sizeZ = random.nextInt(4) + 6;
        final Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        final int minX = x - sizeX / 2;
        final int minZ = z - sizeZ / 2;
        return new Node(MoonDungeonPiece.Kind.ENTRANCE, new BoundingBox(minX, Y, minZ, minX + sizeX, Y + 12, minZ + sizeZ), dir, sizeX, 12, sizeZ);
    }

    private static Node nextPiece(Node node, List<Node> pieces, RandomSource random) {
        final BoundingBox start = pieces.get(0).box();
        return switch (node.kind()) {
            case ENTRANCE -> corridor(node, pieces, random, false);
            case BOSS -> corridor(node, pieces, random, true);
            case EMPTY, SPAWNER, CHEST -> farFromStart(node, start) ? null : corridor(node, pieces, random, false);
            case CORRIDOR -> roomAfter(node, pieces, random);
            default -> null;
        };
    }

    private static boolean farFromStart(Node node, BoundingBox start) {
        return Math.abs(start.maxZ() - node.box().minZ()) > 200 || Math.abs(start.maxX() - node.box().minX()) > 200;
    }

    /** 1.12.2 Piece#getExtension: a box of the given length and width off one side, at the dungeon's floor height. */
    static BoundingBox extension(BoundingBox box, Direction direction, int length, int width) {
        final int x;
        final int z;
        final int sizeX;
        final int sizeZ;
        switch (direction) {
            case NORTH -> {
                sizeX = width;
                sizeZ = length;
                x = box.minX() + (box.maxX() - box.minX()) / 2 - sizeX / 2;
                z = box.minZ() - sizeZ;
            }
            case EAST -> {
                sizeX = length;
                sizeZ = width;
                x = box.maxX();
                z = box.minZ() + (box.maxZ() - box.minZ()) / 2 - sizeZ / 2;
            }
            case SOUTH -> {
                sizeX = width;
                sizeZ = length;
                x = box.minX() + (box.maxX() - box.minX()) / 2 - sizeX / 2;
                z = box.maxZ();
            }
            default -> {
                sizeX = length;
                sizeZ = width;
                x = box.minX() - sizeX;
                z = box.minZ() + (box.maxZ() - box.minZ()) / 2 - sizeZ / 2;
            }
        }
        return new BoundingBox(x, Y, z, x + sizeX, Y + HALL_HEIGHT, z + sizeZ);
    }

    /** 1.12.2 DungeonStart#checkIntersection: against every piece but the newest. */
    private static boolean intersects(List<Node> pieces, BoundingBox box) {
        for (int i = 0; i < pieces.size() - 1; i++) {
            if (pieces.get(i).box().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /** 1.12.2 DirectionalPiece#getCorridor: up to 10 tries, never back the way it came. */
    private static Node corridor(Node from, List<Node> pieces, RandomSource random, boolean small) {
        int attempts = 10;
        int turn = random.nextInt(3);
        Direction dir;
        BoundingBox ext;
        boolean valid;
        do {
            dir = Direction.from2DDataValue((from.direction().getOpposite().get2DDataValue() + 1 + turn) % 4);
            ext = extension(from.box(), dir, HALL_MIN + random.nextInt(HALL_MAX - HALL_MIN), 3);
            valid = !intersects(pieces, ext);
            attempts--;
            turn++;
        } while (!valid && attempts > 0);
        if (!valid) {
            return null;
        }
        final int sizeX = ext.maxX() - ext.minX();
        final int sizeZ = ext.maxZ() - ext.minZ();
        final int sizeY = small ? 3 : HALL_HEIGHT;
        return new Node(MoonDungeonPiece.Kind.CORRIDOR, new BoundingBox(ext.minX(), Y, ext.minZ(), ext.minX() + sizeX, Y + sizeY, ext.minZ() + sizeZ),
                dir, sizeX, sizeY, sizeZ);
    }

    /** 1.12.2 Corridor#getNextPiece. */
    private static Node roomAfter(Node corridor, List<Node> pieces, RandomSource random) {
        final int count = pieces.size() - 1;
        if (count > 10 && pieces.get(pieces.size() - 2).kind() == MoonDungeonPiece.Kind.BOSS) {
            return room(MoonDungeonPiece.Kind.TREASURE, corridor, pieces, random);
        }
        final int bossChance = count > 10 ? Math.max((int) (20.0D / (count - 10)), 1) : 1;
        final boolean boss = count > 25 || count > 10 && random.nextInt(bossChance) == 0;
        if (boss) {
            return room(MoonDungeonPiece.Kind.BOSS, corridor, pieces, random);
        }
        final BoundingBox ext = extension(corridor.box(), corridor.direction(), random.nextInt(4) + 6, random.nextInt(4) + 6);
        if (intersects(pieces, ext) || farFromStart(corridor, pieces.get(0).box())) {
            return null;
        }
        final MoonDungeonPiece.Kind kind = switch (random.nextInt(3)) {
            case 0 -> MoonDungeonPiece.Kind.SPAWNER;
            case 1 -> MoonDungeonPiece.Kind.CHEST;
            default -> MoonDungeonPiece.Kind.EMPTY;
        };
        return roomNode(kind, ext, ROOM_HEIGHT, corridor.direction());
    }

    /** 1.12.2 Corridor#getRoom: the boss or treasure room's own random size, fitted to the corridor's end. */
    private static Node room(MoonDungeonPiece.Kind kind, Node corridor, List<Node> pieces, RandomSource random) {
        final int dummyX;
        final int dummyY;
        final int dummyZ;
        if (kind == MoonDungeonPiece.Kind.BOSS) {
            dummyX = random.nextInt(6) + 14;
            dummyY = random.nextInt(2) + 8;
            dummyZ = random.nextInt(6) + 14;
        } else {
            dummyX = random.nextInt(4) + 6;
            dummyY = ROOM_HEIGHT;
            dummyZ = random.nextInt(4) + 6;
        }
        final boolean alongX = corridor.direction().getAxis() == Direction.Axis.X;
        final BoundingBox ext = extension(corridor.box(), corridor.direction(), alongX ? dummyX : dummyZ, alongX ? dummyZ : dummyX);
        if (intersects(pieces, ext)) {
            return null;
        }
        return roomNode(kind, ext, dummyY, corridor.direction());
    }

    private static Node roomNode(MoonDungeonPiece.Kind kind, BoundingBox ext, int sizeY, Direction direction) {
        final int sizeX = ext.maxX() - ext.minX();
        final int sizeZ = ext.maxZ() - ext.minZ();
        return new Node(kind, new BoundingBox(ext.minX(), Y, ext.minZ(), ext.minX() + sizeX, Y + sizeY, ext.minZ() + sizeZ), direction, sizeX, sizeY, sizeZ);
    }
}
