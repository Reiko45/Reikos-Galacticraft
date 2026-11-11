package io.github.reiko45.galacticraft.oxygen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Blocks that change when oxygen arrives or leaves (1.12.2 IOxygenReliantBlock), e.g. torches on the Moon. */
public interface OxygenReliantBlock {
    void onOxygenAdded(Level level, BlockPos pos, BlockState state);

    void onOxygenRemoved(Level level, BlockPos pos, BlockState state);
}
