package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Breathable Air (1.12.2 BlockBreathableAir): invisible air that an Oxygen Sealer fills a sealed room with. When a
 * neighbouring block is removed, the new gap fills with breathable air too and nearby sealers re-check the seal.
 * THERMAL is set by a sealer with an Ambient Thermal Controller (used once thermal effects arrive).
 */
public class BreathableAirBlock extends AirBlock {
    public static final MapCodec<AirBlock> CODEC = simpleCodec(BreathableAirBlock::new);
    public static final BooleanProperty THERMAL = BooleanProperty.create("thermal");

    public BreathableAirBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(THERMAL, false));
    }

    @Override
    public MapCodec<AirBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THERMAL);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        final BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.is(net.minecraft.world.level.block.Blocks.AIR)) {
            final Direction travel = Direction.fromDelta(neighborPos.getX() - pos.getX(), neighborPos.getY() - pos.getY(), neighborPos.getZ() - pos.getZ());
            if (travel != null && OxygenUtil.canBlockPassAir(level, neighbor, neighborPos, travel)) {
                level.setBlock(neighborPos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        OxygenSealerBlockEntity.requestRecheck(level, pos);
    }
}
