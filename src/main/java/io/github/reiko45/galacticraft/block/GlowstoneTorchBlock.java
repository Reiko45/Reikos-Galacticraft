package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Glowstone Torch: a flameless torch that keeps working without oxygen. No smoke or flame particles. */
public class GlowstoneTorchBlock extends TorchBlock {
    public static final MapCodec<GlowstoneTorchBlock> CODEC = simpleCodec(GlowstoneTorchBlock::new);

    public GlowstoneTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
    }

    @Override
    public MapCodec<? extends TorchBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }
}
