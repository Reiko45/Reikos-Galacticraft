/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockScreen). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.DisplayScreenBlockEntity;
import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.12.2 BlockScreen (Display Screen): faces the player who placed it (the wrench turns it round); right-clicking shows
 * the next picture and joins it with matching screens beside it into one big screen (up to 9 blocks across). Its frame
 * edges (UP, DOWN, LEFT, RIGHT: joined that way) are set by its block entity. It seals all round and is slightly thinner
 * at the front.
 */
public class DisplayScreenBlock extends BaseEntityBlock implements Wrenchable, PartialSealable {
    public static final MapCodec<DisplayScreenBlock> CODEC = simpleCodec(DisplayScreenBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    private static final double FRONT = 0.094D * 16.0D;
    private static final VoxelShape NORTH = box(0, 0, FRONT, 16, 16, 16);
    private static final VoxelShape SOUTH = box(0, 0, 0, 16, 16, 16 - FRONT);
    private static final VoxelShape WEST = box(FRONT, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = box(0, 0, 0, 16 - FRONT, 16, 16);

    public DisplayScreenBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false)
                .setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, UP, DOWN, LEFT, RIGHT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (level.getBlockEntity(pos) instanceof DisplayScreenBlockEntity screen) {
            screen.breakScreen(state);
        }
        level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()).setValue(UP, false).setValue(DOWN, false)
                .setValue(LEFT, false).setValue(RIGHT, false), Block.UPDATE_ALL);
        return true;
    }

    /** 1.12.2 onMachineActivated: the next picture. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DisplayScreenBlockEntity screen) {
            screen.changeChannel();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DisplayScreenBlockEntity screen) {
            screen.refreshConnections(true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof DisplayScreenBlockEntity screen) {
            screen.setRemoved();
            screen.breakScreen(state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayScreenBlockEntity(pos, state);
    }
}
