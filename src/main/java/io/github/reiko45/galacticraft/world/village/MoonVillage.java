/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.MapGenVillageMoon,
 * StructureVillageStartMoon, StructureVillagePiecesMoon, StructureVillagePieceWeightMoon and the buildComponent /
 * factory methods of the StructureComponentVillage subclasses). Copyright (c) 2023 Team Galacticraft. Licensed under
 * the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.village;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.registry.GCStructures;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * A Moon village (1.12.2 MapGenVillageMoon): a well in the middle, plank paths branching off it, and along the paths
 * domed houses, fields of saplings and crops, and lamp posts, laid out with the old (pre-1.14) vanilla village rules
 * that 1.12.2 copied: weighted pieces with per-village limits, at most 112 blocks from the well. One village per
 * 32x32 chunk region, in a random chunk of its first 24x24 (structure set in datagen, with 1.12.2's salt 10387312):
 * the rule 1.12.2's /locate used and its generator meant to follow.
 */
public class MoonVillage extends Structure {
    public static final MapCodec<MoonVillage> CODEC = simpleCodec(MoonVillage::new);

    /** 1.12.2 terrainType: always 0 on the Moon. */
    private static final int TERRAIN_TYPE = 0;
    private static final int RANGE = 112;

    public MoonVillage(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return GCStructures.MOON_VILLAGE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        final int x = context.chunkPos().getMinBlockX() + 2;
        final int z = context.chunkPos().getMinBlockZ() + 2;
        return Optional.of(new GenerationStub(new BlockPos(x, 64, z), builder -> generate(builder, context.random(), x, z)));
    }

    /** 1.12.2 StructureVillageStartMoon: the well, then each pending path or building in turn picked at random. */
    public static void generate(StructurePiecesBuilder builder, RandomSource random, int x, int z) {
        final Planner planner = new Planner(builder, random, weights(random));
        final MoonVillagePiece well = new MoonVillagePiece(MoonVillagePiece.Kind.WELL, 0, new BoundingBox(x, 64, z, x + 5, 78, z + 5),
                // 1.12.2 EnumFacing.byIndex(nextInt(4)): down, up, north or south. Up and down are no horizontal
                // facing, so half the wells place their blocks at the world's origin instead (none get built).
                Direction.from3DDataValue(random.nextInt(4)), null);
        planner.start = well;
        builder.addPiece(well);
        planner.buildWell(well);
        while (!planner.pendingRoads.isEmpty() || !planner.pendingHouses.isEmpty()) {
            if (planner.pendingRoads.isEmpty()) {
                planner.build(planner.pendingHouses.remove(random.nextInt(planner.pendingHouses.size())));
            } else {
                planner.build(planner.pendingRoads.remove(random.nextInt(planner.pendingRoads.size())));
            }
        }
    }

    /** 1.12.2 StructureVillagePieceWeightMoon: a kind of building, its weight and how many a village may have. */
    static final class Weight {
        final MoonVillagePiece.Kind kind;
        final int weight;
        final int limit;
        int spawned;

        Weight(MoonVillagePiece.Kind kind, int weight, int limit) {
            this.kind = kind;
            this.weight = weight;
            this.limit = (int) (limit / 1.5D);
        }

        boolean canSpawnMore() {
            return this.limit == 0 || this.spawned < this.limit;
        }
    }

    /** 1.12.2 getStructureVillageWeightedPieceList: wood huts, fields, crop fields and houses, each weight 5. */
    static List<Weight> weights(RandomSource random) {
        final List<Weight> weights = new ArrayList<>();
        weights.add(new Weight(MoonVillagePiece.Kind.WOOD_HUT, 5, Mth.nextInt(random, 2 + TERRAIN_TYPE, 5 + TERRAIN_TYPE * 3)));
        weights.add(new Weight(MoonVillagePiece.Kind.FIELD, 5, Mth.nextInt(random, 3 + TERRAIN_TYPE, 5 + TERRAIN_TYPE)));
        weights.add(new Weight(MoonVillagePiece.Kind.CROP_FIELD, 5, Mth.nextInt(random, 3 + TERRAIN_TYPE, 5 + TERRAIN_TYPE)));
        weights.add(new Weight(MoonVillagePiece.Kind.HOUSE, 5, Mth.nextInt(random, 3 + TERRAIN_TYPE, 4 + TERRAIN_TYPE * 2)));
        weights.removeIf(weight -> weight.limit == 0);
        return weights;
    }

    /** The village being laid out (1.12.2 kept this on StructureComponentVillageStartPiece). */
    static final class Planner {
        final StructurePiecesBuilder pieces;
        final RandomSource random;
        final List<Weight> weights;
        final List<MoonVillagePiece> pendingRoads = new ArrayList<>();
        final List<MoonVillagePiece> pendingHouses = new ArrayList<>();
        MoonVillagePiece start;
        @Nullable
        Weight last;

        Planner(StructurePiecesBuilder pieces, RandomSource random, List<Weight> weights) {
            this.pieces = pieces;
            this.random = random;
            this.weights = weights;
        }

        /** 1.12.2 buildComponent: only the well and the paths add more pieces. */
        void build(MoonVillagePiece piece) {
            if (piece.kind() == MoonVillagePiece.Kind.PATH) {
                buildPath(piece);
            }
        }

        /** 1.12.2 StructureComponentVillageWell#buildComponent: a path off each side. */
        void buildWell(MoonVillagePiece well) {
            final BoundingBox box = well.getBoundingBox();
            final int depth = well.getGenDepth();
            path(box.minX() - 1, box.maxY() - 4, box.minZ() + 1, Direction.WEST, depth);
            path(box.maxX() + 1, box.maxY() - 4, box.minZ() + 1, Direction.EAST, depth);
            path(box.minX() + 1, box.maxY() - 4, box.minZ() - 1, Direction.NORTH, depth);
            path(box.minX() + 1, box.maxY() - 4, box.maxZ() + 1, Direction.SOUTH, depth);
        }

        /** 1.12.2 StructureComponentVillagePathGen#buildComponent: buildings along both sides, then maybe two more paths. */
        void buildPath(MoonVillagePiece path) {
            final BoundingBox box = path.getBoundingBox();
            final int index = horizontalIndex(path.getOrientation());
            boolean placed = false;
            for (int offset = this.random.nextInt(5); offset < path.length() - 8; offset += 2 + this.random.nextInt(5)) {
                final MoonVillagePiece next = sideNN(path, 0, offset);
                if (next != null) {
                    offset += Math.max(next.getBoundingBox().getXSpan(), next.getBoundingBox().getZSpan());
                    placed = true;
                }
            }
            for (int offset = this.random.nextInt(5); offset < path.length() - 8; offset += 2 + this.random.nextInt(5)) {
                final MoonVillagePiece next = sidePP(path, 0, offset);
                if (next != null) {
                    offset += Math.max(next.getBoundingBox().getXSpan(), next.getBoundingBox().getZSpan());
                    placed = true;
                }
            }
            final int depth = path.getGenDepth();
            if (placed && this.random.nextInt(3) > 0) {
                switch (index) {
                    case 0 -> path(box.minX() - 1, box.minY(), box.maxZ() - 2, Direction.WEST, depth);
                    case 1 -> path(box.minX(), box.minY(), box.minZ() - 1, Direction.NORTH, depth);
                    case 2 -> path(box.minX() - 1, box.minY(), box.minZ(), Direction.WEST, depth);
                    case 3 -> path(box.maxX() - 2, box.minY(), box.minZ() - 1, Direction.NORTH, depth);
                    default -> {
                    }
                }
            }
            if (placed && this.random.nextInt(3) > 0) {
                switch (index) {
                    case 0 -> path(box.maxX() + 1, box.minY(), box.maxZ() - 2, Direction.EAST, depth);
                    case 1 -> path(box.minX(), box.minY(), box.maxZ() + 1, Direction.SOUTH, depth);
                    case 2 -> path(box.maxX() + 1, box.minY(), box.minZ(), Direction.EAST, depth);
                    case 3 -> path(box.maxX() - 2, box.minY(), box.maxZ() + 1, Direction.SOUTH, depth);
                    default -> {
                    }
                }
            }
        }

        /** 1.12.2 getHorizontalIndex (2D data value): -1 for up and down. */
        private static int horizontalIndex(@Nullable Direction direction) {
            return direction == null ? -1 : direction.get2DDataValue();
        }

        /** 1.12.2 StructureComponentVillage#getNextComponentNN: a building off the path's left side. */
        @Nullable
        private MoonVillagePiece sideNN(MoonVillagePiece path, int up, int along) {
            final BoundingBox box = path.getBoundingBox();
            return switch (horizontalIndex(path.getOrientation())) {
                case 0, 2 -> building(box.minX() - 1, box.minY() + up, box.minZ() + along, Direction.WEST, path.getGenDepth());
                case 1, 3 -> building(box.minX() + along, box.minY() + up, box.minZ() - 1, Direction.NORTH, path.getGenDepth());
                default -> null;
            };
        }

        /** 1.12.2 StructureComponentVillage#getNextComponentPP: a building off the path's right side. */
        @Nullable
        private MoonVillagePiece sidePP(MoonVillagePiece path, int up, int along) {
            final BoundingBox box = path.getBoundingBox();
            return switch (horizontalIndex(path.getOrientation())) {
                case 0, 2 -> building(box.maxX() + 1, box.minY() + up, box.minZ() + along, Direction.EAST, path.getGenDepth());
                case 1, 3 -> building(box.minX() + along, box.minY() + up, box.maxZ() + 1, Direction.SOUTH, path.getGenDepth());
                default -> null;
            };
        }

        private boolean inRange(int x, int z) {
            return Math.abs(x - this.start.getBoundingBox().minX()) <= RANGE && Math.abs(z - this.start.getBoundingBox().minZ()) <= RANGE;
        }

        /** 1.12.2 getNextVillageStructureComponent: a building, if the village isn't too deep or wide yet. */
        @Nullable
        private MoonVillagePiece building(int x, int y, int z, Direction facing, int depth) {
            if (depth > 50 || !inRange(x, z)) {
                return null;
            }
            final MoonVillagePiece piece = nextBuilding(x, y, z, facing, depth + 1);
            if (piece != null) {
                this.pieces.addPiece(piece);
                this.pendingHouses.add(piece);
            }
            return piece;
        }

        /** 1.12.2 func_75079_a: the total weight, or -1 when every kind has reached its limit. */
        private int totalWeight() {
            boolean any = false;
            int total = 0;
            for (Weight weight : this.weights) {
                if (weight.limit > 0 && weight.spawned < weight.limit) {
                    any = true;
                }
                total += weight.weight;
            }
            return any ? total : -1;
        }

        /** 1.12.2 getNextVillageComponent: five weighted tries for a building that fits, else a lamp post. */
        @Nullable
        private MoonVillagePiece nextBuilding(int x, int y, int z, Direction facing, int depth) {
            final int total = totalWeight();
            if (total <= 0) {
                return null;
            }
            for (int attempt = 0; attempt < 5; attempt++) {
                int pick = this.random.nextInt(total);
                final Iterator<Weight> iterator = this.weights.iterator();
                while (iterator.hasNext()) {
                    final Weight weight = iterator.next();
                    pick -= weight.weight;
                    if (pick < 0) {
                        if (!weight.canSpawnMore() || weight == this.last && this.weights.size() > 1) {
                            break;
                        }
                        final MoonVillagePiece piece = create(weight.kind, x, y, z, facing, depth);
                        if (piece != null) {
                            weight.spawned++;
                            this.last = weight;
                            if (!weight.canSpawnMore()) {
                                this.weights.remove(weight);
                            }
                            return piece;
                        }
                    }
                }
            }
            final BoundingBox torch = BoundingBox.orientBox(x, y, z, 0, 0, 0, 3, 4, 2, facing);
            return this.pieces.findCollisionPiece(torch) == null ? new MoonVillagePiece(MoonVillagePiece.Kind.TORCH, depth, torch, facing, null) : null;
        }

        /** The 1.12.2 factory methods (func_74908_a, func_74900_a, func_74921_a): the box, if it is free. */
        @Nullable
        private MoonVillagePiece create(MoonVillagePiece.Kind kind, int x, int y, int z, Direction facing, int depth) {
            final boolean field = kind == MoonVillagePiece.Kind.FIELD || kind == MoonVillagePiece.Kind.CROP_FIELD;
            final BoundingBox box = field ? BoundingBox.orientBox(x, y, z, 0, 0, 0, 13, 4, 9, facing)
                    : BoundingBox.orientBox(x, y, z, 0, 0, 0, 17, 9, 17, facing);
            if (kind == MoonVillagePiece.Kind.CROP_FIELD && box.minY() <= 10 || this.pieces.findCollisionPiece(box) != null) {
                return null;
            }
            Block[] crops = null;
            if (kind == MoonVillagePiece.Kind.CROP_FIELD) {
                crops = new Block[4];
                for (int i = 0; i < 4; i++) {
                    crops[i] = MoonVillagePiece.randomCrop(this.random);
                }
            }
            return new MoonVillagePiece(kind, depth, box, facing, crops);
        }

        /** 1.12.2 getNextComponentVillagePath: a path 7, 14 or 21 blocks long (the longest that fits). */
        private void path(int x, int y, int z, Direction facing, int depth) {
            if (depth > 3 + TERRAIN_TYPE || !inRange(x, z)) {
                return;
            }
            for (int length = 7 * Mth.nextInt(this.random, 3, 5); length >= 7; length -= 7) {
                final BoundingBox box = BoundingBox.orientBox(x, y, z, 0, 0, 0, 3, 3, length, facing);
                if (this.pieces.findCollisionPiece(box) == null) {
                    if (box.minY() > 10) {
                        final MoonVillagePiece path = new MoonVillagePiece(MoonVillagePiece.Kind.PATH, depth, box, facing, null);
                        this.pieces.addPiece(path);
                        this.pendingRoads.add(path);
                    }
                    return;
                }
            }
        }
    }
}
