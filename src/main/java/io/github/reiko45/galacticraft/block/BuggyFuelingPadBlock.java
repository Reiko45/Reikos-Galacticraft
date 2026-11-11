package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

/**
 * Buggy Fueling Pad (1.12.2 BlockLandingPad / BlockLandingPadFull type 1, TileEntityBuggyFueler). Joins into a 3x3 pad
 * like the launch pad, but only refuels a Moon Buggy parked on it from an attached Fuel Loader; rockets can't use it.
 */
public class BuggyFuelingPadBlock extends LandingPadBlock {
    public static final MapCodec<BuggyFuelingPadBlock> CODEC = simpleCodec(BuggyFuelingPadBlock::new);

    public BuggyFuelingPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
