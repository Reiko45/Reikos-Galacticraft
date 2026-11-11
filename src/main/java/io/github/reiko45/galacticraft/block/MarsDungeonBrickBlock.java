/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.blocks.BlockBasicMars#randomDisplayTick).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.registry.GCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mars Dungeon Brick: drips (1 in 10 display ticks) and now and then the sound of a single drop (1 in 1,000).
 *
 * <p>1.12.2 meant this but showed neither: its "sludgeDrip" particle was left as a TODO (a water drip), and it played
 * the sound for a null player on the client, which plays nothing. Here the drip is a water drip, and only under bricks
 * with room below them (the ceilings), where it can be seen.
 */
public class MarsDungeonBrickBlock extends Block {
    public MarsDungeonBrickBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) != 0) {
            return;
        }
        final BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + random.nextDouble(), pos.getY() - 0.05D, pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
        }
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), GCSounds.SINGLE_DRIP.get(), SoundSource.AMBIENT, 1.0F,
                    0.8F + random.nextFloat() / 5.0F, false);
        }
    }
}
