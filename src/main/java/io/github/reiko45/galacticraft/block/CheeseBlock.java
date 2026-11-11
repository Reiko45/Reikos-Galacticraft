package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Block of Cheese. The 1.12.2 block was a copy of the vanilla cake (7 bites, 2 hunger / 0.1 saturation
 * per bite, identical bounding boxes), so this simply reuses {@link CakeBlock} minus candle placement.
 */
public class CheeseBlock extends CakeBlock {
    public static final MapCodec<CakeBlock> CODEC = simpleCodec(CheeseBlock::new);

    public CheeseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CakeBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        // Skip CakeBlock's candle handling; fall through to eating a slice.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
