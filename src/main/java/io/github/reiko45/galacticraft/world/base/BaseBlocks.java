/*
 * Block choices of Galacticraft Legacy's abandoned base (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.base;

import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The blocks an abandoned base is built from. 1.12.2 also used Galacticraft blocks this port doesn't have yet; until it
 * does, those are stood in for by the block each one looks like (the concealed redstone, repeater and detector were
 * disguised as a Tin Decoration Block or Moon Rock in 1.12.2 too, so only their circuits are missing).
 */
final class BaseBlocks {
    private BaseBlocks() {
    }

    /** 1.12.2 basicBlock meta 4 ("deco_block_1"): plain tin. */
    static BlockState plain() {
        return GCBlocks.TIN_DECORATION_BLOCK_2.get().defaultBlockState();
    }

    /** 1.12.2 basicBlock meta 3 ("deco_block_0"): patterned tin. */
    static BlockState pattern() {
        return GCBlocks.TIN_DECORATION_BLOCK_1.get().defaultBlockState();
    }

    /** 1.12.2 AsteroidBlocks.blockWalkway. */
    static BlockState grid() {
        return GCBlocks.WALKWAY.get().defaultBlockState();
    }

    /** 1.12.2 AsteroidBlocks.blockBasic meta 6. */
    static BlockState dark() {
        return GCBlocks.DARK_DECORATION_BLOCK.get().defaultBlockState();
    }

    /** 1.12.2 slabGCHalf meta 6 (asteroids_slab), bottom half. */
    static BlockState darkSlab() {
        return GCBlocks.DARK_DECORATION_SLAB.get().defaultBlockState();
    }

    /** 1.12.2 slabGCHalf meta 1 (bottom) or 9 (top): the plain tin slab. */
    static BlockState tinSlab(boolean top) {
        return GCBlocks.TIN_DECORATION_SLAB_2.get().defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    /** 1.12.2 wallGC metas 0-3: tin 1, tin 2, moon rock, moon dungeon brick. */
    static BlockState wall(int meta) {
        return switch (meta) {
            case 0 -> GCBlocks.TIN_DECORATION_WALL_1.get().defaultBlockState();
            case 1 -> GCBlocks.TIN_DECORATION_WALL_2.get().defaultBlockState();
            case 2 -> GCBlocks.MOON_ROCK_WALL.get().defaultBlockState();
            default -> GCBlocks.MOON_DUNGEON_BRICK_WALL.get().defaultBlockState();
        };
    }

    /** 1.12.2 GCBlocks.moonStoneStairs. */
    static Block moonStairs() {
        return GCBlocks.MOON_ROCK_STAIRS.get();
    }

    /** 1.12.2 GCBlocks.tinStairs2. */
    static Block tinStairs() {
        return GCBlocks.TIN_DECORATION_STAIRS_2.get();
    }

    /** 1.12.2 BlockStairs#getStateFromMeta: facing 5 - (meta & 3) (east, west, south, north), upside down with bit 4. */
    static BlockState stairs(Block stairs, int meta) {
        return stairs.defaultBlockState().setValue(StairBlock.FACING, Direction.from3DDataValue(5 - (meta & 3)))
                .setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
    }

    /** 1.12.2 MarsBlocks.marsBlock meta 8. */
    static BlockState desh() {
        return GCBlocks.DESH_BLOCK.get().defaultBlockState();
    }

    // ---------------------------------------------------------------- stand-ins for blocks not ported yet

    /** 1.12.2 airLockFrame (frame, meta 0, and controller, meta 1): dark frames, like the Dark Decoration Block. */
    static BlockState airLockFrame() {
        return dark();
    }

    static BlockState airLockController() {
        return dark();
    }

    /** 1.12.2 spaceGlassTinClear / TinVanilla / TinStrong: plain glass. */
    static BlockState spaceGlass() {
        return Blocks.GLASS.defaultBlockState();
    }

    /** 1.12.2 concealedRedstone and concealedRepeater_Powered: drawn as deco_block_1. */
    static BlockState concealedRedstone() {
        return plain();
    }

    static BlockState concealedRepeater() {
        return plain();
    }

    /** 1.12.2 concealedDetector: drawn as deco_block_1 (variant 0, humanoid bases) or moon stone (variant 1). */
    static BlockState concealedDetector(boolean humanoid) {
        return humanoid ? plain() : GCBlocks.MOON_ROCK.get().defaultBlockState();
    }

    /** 1.12.2 brightLamp (Arc Lamp). */
    static BlockState arcLamp() {
        return Blocks.SEA_LANTERN.defaultBlockState();
    }

    /** 1.12.2 crafting (Magnetic Crafting Table). */
    static BlockState magneticTable() {
        return Blocks.CRAFTING_TABLE.defaultBlockState();
    }

    /** 1.12.2 telemetry (Telemetry Unit). */
    static BlockState telemetry() {
        return pattern();
    }

    /** 1.12.2 screen (Display Screen), whose sides are the Dark Decoration Block's texture. */
    static BlockState screen() {
        return dark();
    }

    /** 1.12.2 sealableBlock meta 14 (Sealable Heavy Aluminium Wire): the wire itself. */
    static BlockState enclosedHeavyWire() {
        return GCBlocks.HEAVY_ALUMINUM_WIRE.get().defaultBlockState();
    }
}
