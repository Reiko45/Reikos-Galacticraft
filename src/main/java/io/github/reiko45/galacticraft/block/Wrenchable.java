package io.github.reiko45.galacticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A block the Standard Wrench turns (1.12.2 BlockAdvanced#onUseWrench) other than a machine. */
public interface Wrenchable {
    /** Server side; returns whether the wrench did anything. */
    boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player);
}
