package io.github.reiko45.galacticraft.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Oil and fuel blocks (1.12.2 BlockFluidGC). Both count as flammable; fuel explodes (power 6, breaking blocks)
 * as soon as fire checks it, like 1.12.2.
 */
public class GCLiquidBlock extends LiquidBlock {
    private final boolean explosive;

    public GCLiquidBlock(FlowingFluid fluid, Properties properties, boolean explosive) {
        super(fluid, properties);
        this.explosive = explosive;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter getter, BlockPos pos, Direction direction) {
        if (this.explosive && getter instanceof Level level && !level.isClientSide) {
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6.0F, Level.ExplosionInteraction.BLOCK);
        }
        return true;
    }
}
