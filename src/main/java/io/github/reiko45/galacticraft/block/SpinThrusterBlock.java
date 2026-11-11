package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.world.station.StationSpin;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Spin Thruster (1.12.2 BlockSpinThruster): fixed to the side of a space station block, it makes the station spin.
 * {@link #FACING} points away from the wall; the wrench flips {@link #REVERSED} to spin the other way, and a redstone
 * signal switches it off. Placing, breaking or wrenching one makes the station work out its shape again
 * ({@link StationSpin#refresh}).
 */
public class SpinThrusterBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SpinThrusterBlock> CODEC = simpleCodec(SpinThrusterBlock::new);
    public static final BooleanProperty REVERSED = BooleanProperty.create("rev");
    // 1.12.2 NORTH_AABB etc.: 0.6 deep against the wall
    private static final VoxelShape NORTH = Block.box(3.2D, 3.2D, 6.4D, 12.8D, 12.8D, 16.0D);
    private static final VoxelShape SOUTH = Block.box(3.2D, 3.2D, 0.0D, 12.8D, 12.8D, 9.6D);
    private static final VoxelShape WEST = Block.box(6.4D, 3.2D, 3.2D, 16.0D, 12.8D, 12.8D);
    private static final VoxelShape EAST = Block.box(0.0D, 3.2D, 3.2D, 9.6D, 12.8D, 12.8D);

    public SpinThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(REVERSED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, REVERSED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
            default -> NORTH;
        };
    }

    private static boolean canStay(LevelReader level, BlockPos pos, Direction facing) {
        final BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canStay(level, pos, state.getValue(FACING));
    }

    /** 1.12.2 getStateForPlacement: on the clicked wall if it holds, otherwise any wall that does. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal() && canStay(context.getLevel(), context.getClickedPos(), clicked)) {
            return defaultBlockState().setValue(FACING, clicked);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (canStay(context.getLevel(), context.getClickedPos(), direction)) {
                return defaultBlockState().setValue(FACING, direction);
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            StationSpin.refreshAt(server, pos.relative(state.getValue(FACING).getOpposite()), true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (level instanceof ServerLevel server && !newState.is(this)) {
            StationSpin.refreshAt(server, pos.relative(state.getValue(FACING).getOpposite()), false);
        }
    }

    /** A redstone change switches it on or off. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level instanceof ServerLevel server) {
            StationSpin.refreshAt(server, pos, false);
        }
    }

    /** 1.12.2 onUseWrench: reverse the thrust. */
    public void reverse(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(REVERSED, !state.getValue(REVERSED)), Block.UPDATE_CLIENTS);
        if (level instanceof ServerLevel server) {
            StationSpin.refreshAt(server, pos, false);
        }
    }

    /** 1.12.2 randomDisplayTick: smoke while the station spins up or down, now and then otherwise. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!StationSpin.Client.firingNear(pos) && random.nextInt(80) != 0) {
            return;
        }
        final Direction facing = state.getValue(FACING);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D + facing.getStepX() * 0.27D, pos.getY() + 0.92D,
                pos.getZ() + 0.5D + facing.getStepZ() * 0.27D, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.galacticraftcore.spin_thruster.desc").withStyle(ChatFormatting.GRAY));
    }
}
