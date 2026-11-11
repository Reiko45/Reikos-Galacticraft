package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.oxygen.OxygenReliantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Wall-mounted {@link SpaceTorchBlock}. */
public class SpaceWallTorchBlock extends WallTorchBlock implements OxygenReliantBlock {
    public static final MapCodec<WallTorchBlock> CODEC = simpleCodec(SpaceWallTorchBlock::new);

    public SpaceWallTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SpaceTorchBlock.LIT, true));
    }

    @Override
    public MapCodec<WallTorchBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SpaceTorchBlock.LIT);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SpaceTorchBlock.checkOxygen(level, pos, state, Blocks.WALL_TORCH.defaultBlockState().setValue(FACING, state.getValue(FACING)));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void onOxygenAdded(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(SpaceTorchBlock.LIT)) {
            level.setBlock(pos, state.setValue(SpaceTorchBlock.LIT, true), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onOxygenRemoved(Level level, BlockPos pos, BlockState state) {
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        final Direction opposite = state.getValue(FACING).getOpposite();
        final double x = pos.getX() + 0.5D + 0.27D * opposite.getStepX();
        final double y = pos.getY() + 0.7D + 0.22D;
        final double z = pos.getZ() + 0.5D + 0.27D * opposite.getStepZ();
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        if (state.getValue(SpaceTorchBlock.LIT)) {
            level.addParticle(this.flameParticle, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}
