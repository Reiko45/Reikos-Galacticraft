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

/**
 * Machine with two configurable faces, 1.12.2 IMachineSides TWOFACES_HORIZ: SIDES holds them (left and right of the
 * front by default) and sneak + wrench steps them round the 1.12.2 cycle.
 */
public class SidedMachineBlock extends MachineBlock {
    public static final EnumProperty<StorageSides> SIDES = EnergyStorageBlock.SIDES;

    public SidedMachineBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties, factory);
        registerDefaultState(defaultBlockState().setValue(SIDES, StorageSides.LEFT_RIGHT));
    }

    /** The faces of a block that may predate SIDES (or be another block), defaulting to 1.12.2's left / right. */
    public static StorageSides sides(BlockState state) {
        return state.hasProperty(SIDES) ? state.getValue(SIDES) : StorageSides.LEFT_RIGHT;
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
