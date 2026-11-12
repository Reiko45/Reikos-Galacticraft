/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base.BasePlate).
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** The floor plate one block under the lowest deck of humanoid and hangar bases. */
public class BasePlate extends BasePiece {
    public BasePlate(BaseConfiguration configuration, int blockPosX, int yPos, int blockPosZ, int sizeX, int sizeZ, Direction dir) {
        super(GCStructures.BASE_PLATE.get(), configuration, sizeX, 1, sizeZ, dir);
        setOrientation(dir);
        setBox(blockPosX, yPos, blockPosZ, blockPosX + this.sizeX, yPos, blockPosZ + this.sizeZ);
    }

    public BasePlate(StructurePieceSerializationContext context, CompoundTag tag) {
        super(GCStructures.BASE_PLATE.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        final BlockState blockWall = this.configuration.wallBlock();
        final boolean axisEW = this.direction.getAxis() == Direction.Axis.X;
        final int maxX = axisEW ? this.sizeZ : this.sizeX;
        final int maxZ = axisEW ? this.sizeX : this.sizeZ;
        for (int xx = 0; xx <= maxX; xx++) {
            for (int zz = 0; zz <= maxZ; zz++) {
                place(level, blockWall, xx, 0, zz, box);
            }
        }
    }
}
