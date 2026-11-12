/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockSpaceWart). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Space Wart (1.12.2 BlockSpaceWart): nether wart that never grows, found in the abandoned asteroid base's empty rooms.
 * It looks and drops like nether wart of its age, and like it, it needs soul sand under it to stay put.
 */
public class SpaceWartBlock extends NetherWartBlock {
    public static final MapCodec<SpaceWartBlock> CODEC = simpleCodec(SpaceWartBlock::new);

    public SpaceWartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<NetherWartBlock> codec() {
        @SuppressWarnings("unchecked")
        final MapCodec<NetherWartBlock> codec = (MapCodec<NetherWartBlock>) (MapCodec<?>) CODEC;
        return codec;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }
}
