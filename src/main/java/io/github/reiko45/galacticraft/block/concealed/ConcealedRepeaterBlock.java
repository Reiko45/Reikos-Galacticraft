/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockConcealedRepeater). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.concealed;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Concealed Repeater (1.12.2 BlockConcealedRepeater): a redstone repeater inside a full tin decoration block, with all
 * its delays and locking; it needs nothing under it. (1.12.2 had a separate powered block; a repeater's own POWERED
 * state does that here.)
 */
public class ConcealedRepeaterBlock extends RepeaterBlock {
    public ConcealedRepeaterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    /** No redstone particles: it is hidden. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }
}
