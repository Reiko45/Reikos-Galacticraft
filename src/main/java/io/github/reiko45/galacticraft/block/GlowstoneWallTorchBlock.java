package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Wall-mounted variant of {@link GlowstoneTorchBlock}. */
public class GlowstoneWallTorchBlock extends WallTorchBlock {
    public static final MapCodec<WallTorchBlock> CODEC = simpleCodec(GlowstoneWallTorchBlock::new);

    public GlowstoneWallTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
    }

    @Override
    public MapCodec<WallTorchBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }
}
