package io.github.reiko45.galacticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Space Station Arrival Block (1.12.2 BlockSpaceStationBase): the unbreakable heart of every space station, where the
 * Spin Thrusters' search for the station's shape starts. It can't be picked or mined. (1.12.2 also stacked two
 * invisible blocks on top; this is a single block.)
 */
public class SpaceStationBaseBlock extends Block {
    public SpaceStationBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Player player) {
        return false;
    }
}
