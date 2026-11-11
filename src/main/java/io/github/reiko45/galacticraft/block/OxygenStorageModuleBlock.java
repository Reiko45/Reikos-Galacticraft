package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.machine.StorageSides;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Oxygen Storage Module block: SIDES holds the oxygen input/output faces (sneak + wrench cycles them). */
public class OxygenStorageModuleBlock extends MachineBlock {
    public static final EnumProperty<StorageSides> SIDES = EnergyStorageBlock.SIDES;

    public OxygenStorageModuleBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties, factory);
        registerDefaultState(defaultBlockState().setValue(SIDES, StorageSides.LEFT_RIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIDES);
    }

    @Override
    public boolean cycleSides(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(SIDES, state.getValue(SIDES).next()));
        level.invalidateCapabilities(pos);
        return true;
    }
}
