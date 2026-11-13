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
 * The blocks an abandoned base is built from. A few Galacticraft blocks 1.12.2 used are not ported yet (phase 7g part
 * 2); until they are, the block each one looks like stands in for it.
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

    // ---------------------------------------------------------------- phase 7g blocks

    /** 1.12.2 airLockFrame meta 0 (frame) and meta 1 (controller). */
    static BlockState airLockFrame() {
        return GCBlocks.AIR_LOCK_FRAME.get().defaultBlockState();
    }

    static BlockState airLockController() {
        return GCBlocks.AIR_LOCK_CONTROLLER.get().defaultBlockState();
    }

    /** 1.12.2 spaceGlassTinVanilla (avian bases), spaceGlassTinStrong (tunnelers) or spaceGlassTinClear (humanoids). */
    static BlockState spaceGlass(BaseConfiguration.BaseType type) {
        return switch (type) {
            case AVIAN -> GCBlocks.SPACE_GLASS_VANILLA_TIN.get().defaultBlockState();
            case TUNNELER -> GCBlocks.SPACE_GLASS_STRONG_TIN.get().defaultBlockState();
            default -> GCBlocks.SPACE_GLASS_CLEAR_TIN.get().defaultBlockState();
        };
    }

    /** 1.12.2 concealedRedstone (meta = power). */
    static BlockState concealedRedstone(int power) {
        return GCBlocks.CONCEALED_REDSTONE.get().defaultBlockState().setValue(io.github.reiko45.galacticraft.block.concealed.ConcealedRedstoneBlock.POWER, power);
    }

    static BlockState concealedRedstone() {
        return concealedRedstone(0);
    }

    /** 1.12.2 concealedRepeater_Powered meta 0 (facing south) along z, meta 2 (north) along x. */
    static BlockState concealedRepeater(boolean alongZ) {
        return GCBlocks.CONCEALED_REPEATER.get().defaultBlockState().setValue(net.minecraft.world.level.block.RepeaterBlock.FACING,
                alongZ ? Direction.SOUTH : Direction.NORTH).setValue(net.minecraft.world.level.block.RepeaterBlock.POWERED, true);
    }

    /** 1.12.2 concealedDetector meta 8 + facing (+ 4 off humanoid bases): already "detecting", disguised to match. */
    static BlockState concealedDetector(boolean humanoid, int facing) {
        return GCBlocks.CONCEALED_DETECTOR.get().defaultBlockState()
                .setValue(io.github.reiko45.galacticraft.block.concealed.ConcealedDetectorBlock.FACING, facing & 3)
                .setValue(io.github.reiko45.galacticraft.block.concealed.ConcealedDetectorBlock.VARIANT, humanoid ? 0 : 1)
                .setValue(io.github.reiko45.galacticraft.block.concealed.ConcealedDetectorBlock.DETECTED, true);
    }

    /** 1.12.2 brightLamp meta (the mounting side, as a 3D direction index). */
    static BlockState arcLamp(int meta) {
        return GCBlocks.ARC_LAMP.get().defaultBlockState().setValue(io.github.reiko45.galacticraft.block.ArcLampBlock.FACING, Direction.from3DDataValue(meta));
    }

    // ---------------------------------------------------------------- stand-ins for blocks not ported yet

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

    /** 1.12.2 sealableBlock meta 15 (Sealable Heavy Aluminium Wire). */
    static BlockState enclosedHeavyWire() {
        return GCBlocks.SEALABLE_HEAVY_ALUMINUM_WIRE.get().defaultBlockState();
    }
}
