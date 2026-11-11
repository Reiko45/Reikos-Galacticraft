package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.oxygen.OxygenReliantBlock;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A torch on an airless world (1.12.2 BlockUnlitTorch, unlit_torch / unlit_torch_lit): it only burns where there is
 * oxygen (a sealed room or a distributor bubble) and smoulders at light 3 otherwise. Vanilla torches held on such a
 * world turn into these; back on a world with air they turn back into vanilla torches.
 */
public class SpaceTorchBlock extends TorchBlock implements OxygenReliantBlock {
    public static final MapCodec<SpaceTorchBlock> CODEC = simpleCodec(SpaceTorchBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SpaceTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
        registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    /** Light 14 when burning (1.12.2 0.9375), 3 when out (0.2). */
    public static int light(BlockState state) {
        return state.getValue(LIT) ? 14 : 3;
    }

    @Override
    public MapCodec<? extends TorchBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        checkOxygen(level, pos, state, Blocks.TORCH.defaultBlockState());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Burn or go out depending on oxygen; outside an airless world become the vanilla torch again. */
    static void checkOxygen(Level level, BlockPos pos, BlockState state, BlockState vanilla) {
        if (!CelestialPhysics.isAirless(level)) {
            level.setBlock(pos, vanilla, Block.UPDATE_ALL);
            return;
        }
        final boolean oxygen = OxygenUtil.torchHasOxygen(level, pos);
        if (oxygen != state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, oxygen), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onOxygenAdded(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onOxygenRemoved(Level level, BlockPos pos, BlockState state) {
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        final double x = pos.getX() + 0.5D;
        final double y = pos.getY() + 0.7D;
        final double z = pos.getZ() + 0.5D;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        if (state.getValue(LIT)) {
            level.addParticle(this.flameParticle, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}
