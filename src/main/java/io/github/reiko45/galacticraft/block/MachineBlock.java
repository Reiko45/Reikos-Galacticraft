package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A horizontally facing machine with a block entity and a GUI. 1.12.2 grouped machines as metadata variants
 * of {@code machine}, {@code machine2}, {@code machine_tiered}; each is its own block here.
 */
public class MachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory;

    public MachineBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties);
        this.factory = factory;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        // Machines are not data-driven; the codec only needs to exist.
        return simpleCodec(properties -> this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** 1.12.2 BlockMachineMarsT2#randomDisplayTick: a working Gas Liquefier's mist. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity liquefier) {
            liquefier.animateTick(random);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof MachineBlockEntity machine) {
                MachineBlockEntity.serverTick(lvl, pos, st, machine);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            serverPlayer.openMenu(machine, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.dropContents(level, pos);
            machine.onBroken();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Sneak + wrench (1.12.2 BlockMachineBase#onSneakUseWrench): move the energy faces. @return whether it did anything */
    public boolean cycleSides(Level level, BlockPos pos, BlockState state) {
        return false;
    }

    /** Wrench rotation (1.12.2 BlockTileGC#onUseWrench): turn clockwise and refresh capabilities. */
    public void rotateWithWrench(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
        level.invalidateCapabilities(pos);
    }
}
