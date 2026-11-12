/*
 * Dungeon layout and room building derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.dungeon).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.dungeon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * A Moon, Mars or Venus dungeon (1.12.2 MapGenDungeon / MapGenDungeonVenus + DungeonStart + the room classes, with the planet's
 * {@link DungeonConfig}). A crater on the surface leads down a shaft into an entrance room 12 blocks tall; from there
 * corridors and rooms (empty, with mob spawners and cobwebs, or with a chest) wander off until, after 10 to 25 pieces,
 * a corridor ends in the boss room, whose low corridor leads to the treasure room. A layout that doesn't end boss -
 * corridor - treasure is retried (10 attempts, as in 1.12.2). One dungeon per 704x704 block region (44x44 chunks), at a
 * random chunk of it (structure sets in datagen).
 */
public class Dungeon extends Structure {
    public static final MapCodec<Dungeon> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            DungeonConfig.CODEC.optionalFieldOf("config", DungeonConfig.MOON).forGetter(dungeon -> dungeon.config)
    ).apply(instance, Dungeon::new));

    static final int CRATER_RANGE = 16;

    private final DungeonConfig config;

    public Dungeon(StructureSettings settings, DungeonConfig config) {
        super(settings);
        this.config = config;
    }

    public DungeonConfig config() {
        return this.config;
    }

    @Override
    public StructureType<?> type() {
        return GCStructures.DUNGEON.get();
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
        return Optional.of(new GenerationStub(new BlockPos(x, this.config.y(), z), builder -> generate(this.config, builder, context.random(), x, z, surfaceTop)));
    }

    /**
     * One planned piece of the layout; a Venus boss room also knows the way out to its treasure (1.12.2
     * RoomBossVenus#exitDirection, in the same sense as {@code direction}), for its second doorway.
     */
    public record Node(DungeonPiece.Kind kind, BoundingBox box, Direction direction, int sizeX, int sizeY, int sizeZ, @org.jetbrains.annotations.Nullable Direction exit) {
        public Node(DungeonPiece.Kind kind, BoundingBox box, Direction direction, int sizeX, int sizeY, int sizeZ) {
            this(kind, box, direction, sizeX, sizeY, sizeZ, null);
        }
    }

    private static void generate(DungeonConfig config, StructurePiecesBuilder builder, RandomSource random, int x, int z, int surface) {
        final List<Node> layout = plan(config, random, x, z);
        BlockPos chestPos = null;
        if (layout.get(layout.size() - 1).kind() == DungeonPiece.Kind.TREASURE) {
            final Node treasure = layout.get(layout.size() - 1);
            chestPos = new BlockPos(treasure.box().minX() + treasure.sizeX() / 2, config.y() + 1, treasure.box().minZ() + treasure.sizeZ() / 2);
        }
        for (Node node : layout) {
            builder.addPiece(new DungeonPiece(config, node, node.kind() == DungeonPiece.Kind.CRATER ? surface : 0,
                    node.kind() == DungeonPiece.Kind.BOSS ? chestPos : null));
        }
    }

    /** 1.12.2 DungeonStart#buildComponent with the getNextPiece chain of each piece. */
    public static List<Node> plan(DungeonConfig config, RandomSource random, int x, int z) {
        final int floor = config.y();
        // EntranceCrater (its random size and direction are unused, but drawn as in 1.12.2)
        final int craterSizeX = random.nextInt(4) + 6;
        final int craterSizeZ = random.nextInt(4) + 6;
        final Direction craterDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        final Node crater = new Node(DungeonPiece.Kind.CRATER, new BoundingBox(x - CRATER_RANGE, floor + 11, z - CRATER_RANGE,
                x + CRATER_RANGE, 150, z + CRATER_RANGE), craterDir, craterSizeX, 12, craterSizeZ);
        List<Node> pieces = new ArrayList<>();
        for (int attempt = 0; attempt < 10; attempt++) {
            pieces = new ArrayList<>();
            pieces.add(crater);
            Node next = entrance(config, random, x, z);
            while (next != null) {
                pieces.add(next);
                final Node following = nextPiece(config, next, pieces, random);
                if (next.kind() == DungeonPiece.Kind.BOSS && config == DungeonConfig.VENUS) {
                    // RoomBossVenus#getNextPiece remembers which way its corridor leaves
                    pieces.set(pieces.size() - 1, new Node(next.kind(), next.box(), next.direction(), next.sizeX(), next.sizeY(), next.sizeZ(),
                            following == null ? null : following.direction().getOpposite()));
                }
                next = following;
            }
            final int attached = pieces.size() - 1;
            if (attached >= 3 && pieces.get(pieces.size() - 1).kind() == DungeonPiece.Kind.TREASURE
                    && pieces.get(pieces.size() - 3).kind() == DungeonPiece.Kind.BOSS) {
                return pieces;
            }
        }
        // No complete layout: keep the last attempt, like 1.12.2 (which logged it as a bug)
        return pieces;
    }

    private static Node entrance(DungeonConfig config, RandomSource random, int x, int z) {
        final int floor = config.y();
        final int sizeX = random.nextInt(4) + 6;
        final int sizeZ = random.nextInt(4) + 6;
        final Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        final int minX = x - sizeX / 2;
        final int minZ = z - sizeZ / 2;
        return new Node(DungeonPiece.Kind.ENTRANCE, new BoundingBox(minX, floor, minZ, minX + sizeX, floor + 12, minZ + sizeZ), dir, sizeX, 12, sizeZ);
    }

    private static Node nextPiece(DungeonConfig config, Node node, List<Node> pieces, RandomSource random) {
        final BoundingBox start = pieces.get(0).box();
        return switch (node.kind()) {
            case ENTRANCE -> corridor(config, node, pieces, random, false);
            case BOSS -> corridor(config, node, pieces, random, true);
            case EMPTY, SPAWNER, CHEST -> farFromStart(node, start) ? null : corridor(config, node, pieces, random, false);
            case CORRIDOR -> roomAfter(config, node, pieces, random);
            default -> null;
        };
    }

    private static boolean farFromStart(Node node, BoundingBox start) {
        return Math.abs(start.maxZ() - node.box().minZ()) > 200 || Math.abs(start.maxX() - node.box().minX()) > 200;
    }

    /** 1.12.2 Piece#getExtension: a box of the given length and width off one side, at the dungeon's floor height. */
    static BoundingBox extension(DungeonConfig config, BoundingBox box, Direction direction, int length, int width) {
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
        return new BoundingBox(x, config.y(), z, x + sizeX, config.y() + config.hallHeight(), z + sizeZ);
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
    private static Node corridor(DungeonConfig config, Node from, List<Node> pieces, RandomSource random, boolean small) {
        final int floor = config.y();
        int attempts = 10;
        int turn = random.nextInt(3);
        Direction dir;
        BoundingBox ext;
        boolean valid;
        do {
            dir = Direction.from2DDataValue((from.direction().getOpposite().get2DDataValue() + 1 + turn) % 4);
            ext = extension(config, from.box(), dir, DungeonConfig.HALL_MIN + random.nextInt(DungeonConfig.HALL_MAX - DungeonConfig.HALL_MIN),
                    config.hallWidth());
            valid = !intersects(pieces, ext);
            attempts--;
            turn++;
        } while (!valid && attempts > 0);
        if (!valid) {
            return null;
        }
        final int sizeX = ext.maxX() - ext.minX();
        final int sizeZ = ext.maxZ() - ext.minZ();
        final int sizeY = small ? 3 : config.hallHeight();
        return new Node(DungeonPiece.Kind.CORRIDOR, new BoundingBox(ext.minX(), floor, ext.minZ(), ext.minX() + sizeX, floor + sizeY, ext.minZ() + sizeZ),
                dir, sizeX, sizeY, sizeZ);
    }

    /** 1.12.2 Corridor#getNextPiece. */
    private static Node roomAfter(DungeonConfig config, Node corridor, List<Node> pieces, RandomSource random) {
        final int count = pieces.size() - 1;
        if (count > 10 && pieces.get(pieces.size() - 2).kind() == DungeonPiece.Kind.BOSS) {
            return room(config, DungeonPiece.Kind.TREASURE, corridor, pieces, random);
        }
        final int bossChance = count > 10 ? Math.max((int) (20.0D / (count - 10)), 1) : 1;
        final boolean boss = count > 25 || count > 10 && random.nextInt(bossChance) == 0;
        if (boss) {
            return room(config, DungeonPiece.Kind.BOSS, corridor, pieces, random);
        }
        final BoundingBox ext = extension(config, corridor.box(), corridor.direction(), random.nextInt(4) + 6, random.nextInt(4) + 6);
        if (intersects(pieces, ext) || farFromStart(corridor, pieces.get(0).box())) {
            return null;
        }
        final DungeonPiece.Kind kind;
        if (config == DungeonConfig.VENUS) {
            // CorridorVenus#getNextPiece: a spawner room unless the room before was one, then empty or chest
            final Node last = count <= 2 ? null : pieces.get(pieces.size() - 2);
            if (last == null || last.kind() != DungeonPiece.Kind.SPAWNER) {
                kind = DungeonPiece.Kind.SPAWNER;
            } else {
                kind = random.nextInt(2) == 0 ? DungeonPiece.Kind.EMPTY : DungeonPiece.Kind.CHEST;
            }
        } else {
            kind = switch (random.nextInt(3)) {
                case 0 -> DungeonPiece.Kind.SPAWNER;
                case 1 -> DungeonPiece.Kind.CHEST;
                default -> DungeonPiece.Kind.EMPTY;
            };
        }
        return roomNode(config, kind, ext, config.roomHeight(), corridor.direction());
    }

    /**
     * 1.12.2 Corridor#getRoom: the boss or treasure room's own size, fitted to the corridor's end. The Moon's boss room
     * is 14-19 x 8-9 x 14-19; RoomBossMars is always 24 x 11 x 24; RoomBossVenus is 20-25 x 10-11 x 20-25 and sunk 2
     * blocks below the dungeon floor.
     */
    private static Node room(DungeonConfig config, DungeonPiece.Kind kind, Node corridor, List<Node> pieces, RandomSource random) {
        final int dummyX;
        final int dummyY;
        final int dummyZ;
        if (kind == DungeonPiece.Kind.BOSS && config == DungeonConfig.MARS) {
            dummyX = 24;
            dummyY = 11;
            dummyZ = 24;
        } else if (kind == DungeonPiece.Kind.BOSS && config == DungeonConfig.VENUS) {
            dummyX = random.nextInt(6) + 20;
            dummyY = random.nextInt(2) + 10;
            dummyZ = random.nextInt(6) + 20;
        } else if (kind == DungeonPiece.Kind.BOSS) {
            dummyX = random.nextInt(6) + 14;
            dummyY = random.nextInt(2) + 8;
            dummyZ = random.nextInt(6) + 14;
        } else {
            dummyX = random.nextInt(4) + 6;
            dummyY = config.roomHeight();
            dummyZ = random.nextInt(4) + 6;
        }
        final boolean alongX = corridor.direction().getAxis() == Direction.Axis.X;
        final BoundingBox ext = extension(config, corridor.box(), corridor.direction(), alongX ? dummyX : dummyZ, alongX ? dummyZ : dummyX);
        if (intersects(pieces, ext)) {
            return null;
        }
        final Node node = roomNode(config, kind, ext, dummyY, corridor.direction());
        if (kind == DungeonPiece.Kind.BOSS && config == DungeonConfig.VENUS) {
            final BoundingBox box = node.box();
            return new Node(kind, new BoundingBox(box.minX(), box.minY() - 2, box.minZ(), box.maxX(), box.maxY(), box.maxZ()), node.direction(),
                    node.sizeX(), node.sizeY(), node.sizeZ());
        }
        return node;
    }

    private static Node roomNode(DungeonConfig config, DungeonPiece.Kind kind, BoundingBox ext, int sizeY, Direction direction) {
        final int sizeX = ext.maxX() - ext.minX();
        final int sizeZ = ext.maxZ() - ext.minZ();
        final int y = config.y();
        return new Node(kind, new BoundingBox(ext.minX(), y, ext.minZ(), ext.minX() + sizeX, y + sizeY, ext.minZ() + sizeZ), direction, sizeX, sizeY, sizeZ);
    }
}
