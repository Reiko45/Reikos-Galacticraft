/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockWalkway, type walkway).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Walkway (1.12.2 BlockWalkway type walkway): a metal grating to walk on, its top at the block's top, with struts
 * underneath reaching out to other walkways and to solid faces beside it.
 */
public class WalkwayBlock extends Block {
    public static final MapCodec<WalkwayBlock> CODEC = simpleCodec(WalkwayBlock::new);
    public static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, PipeBlock.NORTH, Direction.EAST, PipeBlock.EAST,
            Direction.SOUTH, PipeBlock.SOUTH, Direction.WEST, PipeBlock.WEST);
    /** 1.12.2 AABB_UNCONNECTED: from 0.32 up; walkways joined to something below fill the block (AABB_CONNECTED_DOWN). */
    public static final VoxelShape SHAPE = Block.box(0.0D, 5.12D, 0.0D, 16.0D, 16.0D, 16.0D);
    public static final VoxelShape SHAPE_DOWN = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public WalkwayBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) {
            state = state.setValue(side, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 1.12.2 getActualState: another walkway (of any kind), or a solid face turned this way. */
    public static boolean connects(BlockGetter level, BlockPos pos, Direction direction) {
        final BlockPos neighbor = pos.relative(direction);
        final BlockState state = level.getBlockState(neighbor);
        return isWalkway(state) || state.isFaceSturdy(level, neighbor, direction.getOpposite());
    }

    public static boolean isWalkway(BlockState state) {
        return state.getBlock() instanceof WalkwayBlock || state.getBlock() instanceof WalkwayWireBlock || state.getBlock() instanceof WalkwayPipeBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> side : SIDES.entrySet()) {
            state = state.setValue(side.getValue(), connects(context.getLevel(), context.getClickedPos(), side.getKey()));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        final BooleanProperty side = SIDES.get(direction);
        return side == null ? state : state.setValue(side, connects(level, pos, direction));
    }
}
