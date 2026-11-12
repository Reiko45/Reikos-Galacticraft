/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base.MapGenAbandonedBase).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.base;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.registry.GCStructures;
import io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.Nullable;

/**
 * An abandoned alien base in the asteroid belt (1.12.2 MapGenAbandonedBase): one per 576 x 576 block region (36 x 36
 * chunks), at a random chunk of it (structure set in datagen), built into the asteroid nearest that chunk, 10 blocks
 * below its centre and pushed out from it by up to 20 blocks. 1.12.2 took the nearest asteroid generated so far,
 * however far; a 1.21 structure can't reach more than 8 chunks from its chunk, so here it is the nearest within 64
 * blocks, and with none, the base floats at y 148 as 1.12.2's did with no asteroids known.
 */
public class AbandonedBase extends Structure {
    public static final MapCodec<AbandonedBase> CODEC = simpleCodec(AbandonedBase::new);
    public static final int SEARCH_RADIUS = 64;

    public AbandonedBase(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return GCStructures.ABANDONED_BASE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        final int x = context.chunkPos().getMinBlockX() + 8;
        final int z = context.chunkPos().getMinBlockZ() + 8;
        AsteroidsChunkGenerator.Asteroid nearest = null;
        if (context.chunkGenerator() instanceof AsteroidsChunkGenerator asteroids) {
            nearest = nearest(asteroids.asteroidsNear(context.randomState(), x, z, SEARCH_RADIUS), x, z);
        }
        final List<BasePiece> pieces = plan(context.random(), x, z, nearest);
        final BlockPos start = pieces.get(0).getBoundingBox().getCenter();
        return Optional.of(new GenerationStub(start, builder -> pieces.forEach(builder::addPiece)));
    }

    /** 1.12.2 WorldProviderAsteroids#getClosestAsteroidXZ: the nearest centre in x and z (the first of equals). */
    @Nullable
    public static AsteroidsChunkGenerator.Asteroid nearest(List<AsteroidsChunkGenerator.Asteroid> asteroids, int x, int z) {
        AsteroidsChunkGenerator.Asteroid result = null;
        int lowestDistance = Integer.MAX_VALUE;
        for (AsteroidsChunkGenerator.Asteroid test : asteroids) {
            final int dx = x - test.x();
            final int dz = z - test.z();
            final int distance = dx * dx + dz * dz;
            if (distance < lowestDistance) {
                lowestDistance = distance;
                result = test;
            }
        }
        return result;
    }

    /**
     * 1.12.2 getStructureStart and MapGenAbandonedBase.Start: the base's configuration, then the first deck (BaseStart)
     * offset from the asteroid's centre in a random direction, and from it every other piece.
     */
    public static List<BasePiece> plan(RandomSource rand, int x, int z, @Nullable AsteroidsChunkGenerator.Asteroid asteroid) {
        final int posX;
        final int posZ;
        int size;
        final BaseConfiguration configuration;
        if (asteroid == null) {
            posX = x;
            posZ = z;
            size = 15;
            configuration = new BaseConfiguration(148, rand);
        } else {
            posX = asteroid.x();
            posZ = asteroid.z();
            size = asteroid.size() - 5;
            configuration = new BaseConfiguration(asteroid.y() - 10, rand);
        }
        if (size < 1) {
            size = 1;
        }
        size = size * (int) Mth.sqrt(size) / 4;
        if (configuration.isHangarDeck()) {
            size -= 6;
        }
        int xoffset = 0;
        int zoffset = 0;
        final Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
        switch (direction) {
            case NORTH -> zoffset = -size;
            case SOUTH -> zoffset = size;
            case WEST -> xoffset = -size;
            default -> xoffset = size;
        }
        final BaseDeck start = new BaseDeck(configuration, rand, posX + xoffset, configuration.yPosition(), posZ + zoffset, 1, direction);
        final List<BasePiece> pieces = new ArrayList<>();
        pieces.add(start);
        pieces.addAll(start.rooms(0, rand));
        return pieces;
    }
}
