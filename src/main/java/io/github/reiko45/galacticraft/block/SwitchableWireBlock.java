package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.energy.network.EnergyNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Switchable (Heavy) Aluminum Wire: connects like the plain wire unless it gets a redstone signal, which cuts it out of
 * the network and disconnects it from its neighbours (1.12.2 BlockAluminumWire switch variants).
 */
public class SwitchableWireBlock extends AluminumWireBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SwitchableWireBlock(Properties properties, boolean heavy) {
        super(properties, heavy);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public boolean isConductive(BlockState state) {
        return !state.getValue(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(),
                defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos())));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTIONS.get(direction), isConductive(state) && connects(level, pos, direction));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        final boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED) || level.isClientSide) {
            return;
        }
        level.setBlock(pos, withConnections(level, pos, state.setValue(POWERED, powered)), Block.UPDATE_ALL);
        if (level instanceof ServerLevel serverLevel) {
            final EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);
            if (powered) {
                manager.wireRemoved(pos);
            } else {
                manager.wireAdded(pos);
            }
        }
    }

    private BlockState withConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(dir), isConductive(state) && connects(level, pos, dir));
        }
        return state;
    }
}
